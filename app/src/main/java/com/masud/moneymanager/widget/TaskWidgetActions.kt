// FILE: app/src/main/java/com/masud/moneymanager/widget/TaskWidgetActions.kt
package com.masud.moneymanager.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.ui.TaskAlarmScheduler

class ToggleTaskDoneCallback : ActionCallback {

    companion object {
        val TaskIdKey = ActionParameters.Key<Long>("task_id")
    }

    override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters
    ) {
        Log.d("WidgetToggle", "onAction called")
        val taskId =
                parameters[TaskIdKey]
                        ?: run {
                            Log.e("WidgetToggle", "missing task_id param")
                            return
                        }
        val dao = AppDatabase.getInstance(context).taskDao()
        val task =
                dao.getById(taskId)
                        ?: run {
                            Log.e("WidgetToggle", "task not found: $taskId")
                            return
                        }
        if (task.isDone) {
            dao.setDone(taskId, false)
            runCatching { TaskAlarmScheduler.schedule(context, task.copy(isDone = false)) }
                    .onFailure { Log.e("WidgetToggle", "alarm schedule failed", it) }
        } else {
            dao.setDone(taskId, true)
            runCatching { TaskAlarmScheduler.cancel(context, taskId) }.onFailure {
                Log.e("WidgetToggle", "alarm cancel failed", it)
            }
        }
        Log.d("WidgetToggle", "setDone done, updating widget")
        runCatching { TaskWidget().updateAll(context) }.onFailure {
            Log.e("WidgetToggle", "widget update failed", it)
        }
    }
}
