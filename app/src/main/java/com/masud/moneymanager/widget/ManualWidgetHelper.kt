// FILE: app/src/main/java/com/masud/moneymanager/widget/ManualWidgetHelper.kt
package com.masud.moneymanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.masud.moneymanager.MainActivity
import com.masud.moneymanager.R
import com.masud.moneymanager.data.AppDatabase

object ManualWidgetHelper {

    suspend fun pushUpdate(context: Context) {
        try {
            val appCtx = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appCtx)
            val glanceIds = GlanceAppWidgetManager(appCtx).getGlanceIds(TaskWidget::class.java)
            if (glanceIds.isEmpty()) return

            val dao = AppDatabase.getInstance(appCtx).taskDao()
            val tasks = dao.getPending().take(100)
            Log.d("ManualWidget", "pushing ${tasks.size} tasks")

            // Build text list (simple, works on all APIs).
            val taskText =
                    if (tasks.isEmpty()) {
                        "No pending tasks"
                    } else {
                        tasks.joinToString("\n") { "☐ ${it.title}" }
                    }

            for (glanceId in glanceIds) {
                val appWidgetId = GlanceAppWidgetManager(appCtx).getAppWidgetId(glanceId)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) continue

                val views = RemoteViews(appCtx.packageName, R.layout.widget_manual)
                views.setTextViewText(R.id.task_list, taskText)

                // +Add button.
                val addIntent =
                        Intent(appCtx, WidgetAddTaskActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                val addPending =
                        PendingIntent.getActivity(
                                appCtx,
                                0,
                                addIntent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                views.setOnClickPendingIntent(R.id.btn_add, addPending)

                // Tap list to open app.
                val openIntent =
                        Intent(appCtx, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(EXTRA_OPEN_TASKS, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                val openPending =
                        PendingIntent.getActivity(
                                appCtx,
                                1,
                                openIntent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                views.setOnClickPendingIntent(R.id.task_list, openPending)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("ManualWidget", "pushed to $appWidgetId")
            }
        } catch (e: Exception) {
            Log.e("ManualWidget", "push failed", e)
        }
    }
}
