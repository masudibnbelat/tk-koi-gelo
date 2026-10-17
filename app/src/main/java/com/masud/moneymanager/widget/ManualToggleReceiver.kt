// FILE: app/src/main/java/com/masud/moneymanager/widget/ManualToggleReceiver.kt
package com.masud.moneymanager.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.ui.TaskAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ManualToggleReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        if (taskId == -1L) return
        Log.d("ManualWidget", "toggle received for task $taskId")
        scope.launch {
            try {
                val appCtx = context.applicationContext
                val dao = AppDatabase.getInstance(appCtx).taskDao()
                val task = dao.getById(taskId) ?: return@launch
                if (!task.isDone) {
                    dao.setDone(taskId, true)
                    runCatching { TaskAlarmScheduler.cancel(appCtx, taskId) }
                } else {
                    dao.setDone(taskId, false)
                    runCatching { TaskAlarmScheduler.schedule(appCtx, task.copy(isDone = false)) }
                }
                // Push manual update immediately.
                ManualWidgetHelper.pushUpdate(appCtx)
                // Also trigger Glance refresh for consistency.
                refreshTaskWidgets(appCtx)
            } catch (e: Exception) {
                Log.e("ManualWidget", "toggle failed", e)
            } finally {
                scope.cancel()
            }
        }
    }
}
