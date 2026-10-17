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

/**
 * Direct widget update bypassing Glance's worker.
 * Used for immediate feedback after add — Glance's SessionWorker is unreliable
 * on Realme/Oppo devices (blocked/delayed). This pushes via AppWidgetManager
 * synchronously from the foreground activity.
 */
object ManualWidgetHelper {

    suspend fun pushUpdate(context: Context) {
        try {
            val appCtx = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appCtx)
            val glanceIds = GlanceAppWidgetManager(appCtx).getGlanceIds(TaskWidget::class.java)
            if (glanceIds.isEmpty()) {
                Log.d("ManualWidget", "no widgets placed")
                return
            }
            val dao = AppDatabase.getInstance(appCtx).taskDao()
            val tasks = dao.getPending().take(100)
            Log.d("ManualWidget", "pushing ${tasks.size} tasks manually")

            for (glanceId in glanceIds) {
                val appWidgetId = GlanceAppWidgetManager(appCtx).getAppWidgetId(glanceId)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) continue

                val views = RemoteViews(appCtx.packageName, R.layout.widget_manual)

                // +Add button.
                val addIntent = Intent(appCtx, WidgetAddTaskActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val addPending = PendingIntent.getActivity(
                    appCtx, 0, addIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.btn_add, addPending)

                // Task rows.
                for (task in tasks) {
                    val row = RemoteViews(appCtx.packageName, R.layout.widget_manual_row)
                    row.setTextViewText(R.id.task_title, task.title)

                    // Checkbox toggle.
                    val toggleIntent = Intent(appCtx, ManualToggleReceiver::class.java).apply {
                        putExtra("task_id", task.id)
                    }
                    val togglePending = PendingIntent.getBroadcast(
                        appCtx, task.id.toInt(), toggleIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    row.setOnClickPendingIntent(R.id.btn_check, togglePending)

                    // Row tap to edit.
                    val editIntent = Intent(appCtx, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(EXTRA_OPEN_TASKS, true)
                        putExtra(EXTRA_EDIT_TASK_ID, task.id)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val editPending = PendingIntent.getActivity(
                        appCtx, task.id.toInt() + 1000000, editIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    row.setOnClickPendingIntent(R.id.row_container, editPending)

                    views.addView(R.id.task_container, row)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("ManualWidget", "pushed to appWidgetId=$appWidgetId")
            }
        } catch (e: Exception) {
            Log.e("ManualWidget", "manual push failed", e)
        }
    }
}
