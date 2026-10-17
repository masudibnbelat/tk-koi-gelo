// FILE: app/src/main/java/com/masud/moneymanager/widget/TaskWidgetActions.kt
package com.masud.moneymanager.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.ui.TaskAlarmScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Widget refetch signal. Every refreshTaskWidgets() bumps the version; the widget composition
 * collects it and re-queries the DB via produceState. This is the reliable refetch trigger —
 * independent of Glance internals.
 */
object WidgetRefreshSignal {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    fun bump() {
        _version.value += 1
    }
}

/**
 * Refetch widget data and push to all placed instances. Call after EVERY task mutation
 * (add/edit/delete/toggle).
 */
suspend fun refreshTaskWidgets(context: Context) {
    WidgetRefreshSignal.bump()
    try {
        val appCtx = context.applicationContext
        val ids = GlanceAppWidgetManager(appCtx).getGlanceIds(TaskWidget::class.java)
        Log.d("TaskWidgetRefresh", "refreshing ${ids.size} widget(s): $ids")
        withContext(Dispatchers.IO) { TaskWidget().updateAll(appCtx) }
        Log.d("TaskWidgetRefresh", "refresh done")
    } catch (e: Exception) {
        Log.e("TaskWidgetRefresh", "refresh failed", e)
    }
}

data class WidgetOverlay(
        val stages: Map<Long, Int> = emptyMap(),
        val removedIds: Set<Long> = emptySet()
)

object WidgetCheckAnimation {
    private const val TAG = "WidgetToggle"
    private val jobs = HashMap<Long, Job?>()
    private val _overlay = MutableStateFlow(WidgetOverlay())
    val overlay: StateFlow<WidgetOverlay> = _overlay

    @Synchronized
    fun beginOrCancel(taskId: Long): Boolean {
        if (!jobs.containsKey(taskId)) {
            jobs[taskId] = null
            val cur = _overlay.value
            _overlay.value = cur.copy(stages = cur.stages + (taskId to 1))
            Log.d(TAG, "check stage 1 for task $taskId")
            return false
        }
        jobs.remove(taskId)?.cancel()
        val cur = _overlay.value
        _overlay.value =
                cur.copy(stages = cur.stages - taskId, removedIds = cur.removedIds - taskId)
        Log.d(TAG, "check cancelled for task $taskId")
        return true
    }

    @Synchronized
    fun setStage(taskId: Long, stage: Int) {
        if (jobs.containsKey(taskId)) {
            val cur = _overlay.value
            _overlay.value = cur.copy(stages = cur.stages + (taskId to stage))
            Log.d(TAG, "check stage $stage for task $taskId")
        }
    }

    @Synchronized
    fun markRemoved(taskId: Long) {
        val cur = _overlay.value
        _overlay.value =
                cur.copy(stages = cur.stages - taskId, removedIds = cur.removedIds + taskId)
        Log.d(TAG, "marked removed for task $taskId")
    }

    @Synchronized
    fun syncRemoved(pendingIds: Set<Long>) {
        val cur = _overlay.value
        if (cur.removedIds.isNotEmpty()) {
            _overlay.value = cur.copy(removedIds = emptySet())
            Log.d(TAG, "overlay removals synced with DB")
        }
    }

    @Synchronized
    fun attach(taskId: Long, job: Job) {
        if (jobs.containsKey(taskId)) jobs[taskId] = job else job.cancel()
    }

    @Synchronized
    fun finish(taskId: Long) {
        jobs.remove(taskId)
        val cur = _overlay.value
        _overlay.value = cur.copy(stages = cur.stages - taskId)
    }
}

class ToggleTaskDoneCallback : ActionCallback {

    companion object {
        val TaskIdKey = ActionParameters.Key<Long>("task_id")
        private const val TAG = "WidgetToggle"
        private const val ANIM_BEAT_MS = 400L
    }

    override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters
    ) {
        Log.d(TAG, "onAction called")
        val taskId =
                parameters[TaskIdKey]
                        ?: run {
                            Log.e(TAG, "missing task_id param")
                            return
                        }
        if (WidgetCheckAnimation.beginOrCancel(taskId)) {
            refreshTaskWidgets(context)
            return
        }
        val dao = AppDatabase.getInstance(context).taskDao()
        val task =
                dao.getById(taskId)
                        ?: run {
                            Log.e(TAG, "task not found: $taskId")
                            WidgetCheckAnimation.finish(taskId)
                            refreshTaskWidgets(context)
                            return
                        }
        if (task.isDone) {
            WidgetCheckAnimation.finish(taskId)
            dao.setDone(taskId, false)
            runCatching { TaskAlarmScheduler.schedule(context, task.copy(isDone = false)) }
                    .onFailure { Log.e(TAG, "alarm schedule failed", it) }
            refreshTaskWidgets(context)
            return
        }
        try {
            coroutineScope {
                val commit = launch {
                    try {
                        delay(ANIM_BEAT_MS)
                        WidgetCheckAnimation.setStage(taskId, 2)
                        delay(ANIM_BEAT_MS)
                        withContext(NonCancellable) {
                            dao.setDone(taskId, true)
                            runCatching { TaskAlarmScheduler.cancel(context, taskId) }.onFailure {
                                Log.e(TAG, "alarm cancel failed", it)
                            }
                            WidgetCheckAnimation.markRemoved(taskId)
                            Log.d(TAG, "check committed for task $taskId")
                        }
                    } finally {
                        WidgetCheckAnimation.finish(taskId)
                    }
                }
                WidgetCheckAnimation.attach(taskId, commit)
                refreshTaskWidgets(context)
            }
            refreshTaskWidgets(context)
        } catch (ce: CancellationException) {
            Log.d(TAG, "check coroutine cancelled for task $taskId")
            refreshTaskWidgets(context)
            throw ce
        }
    }
}
