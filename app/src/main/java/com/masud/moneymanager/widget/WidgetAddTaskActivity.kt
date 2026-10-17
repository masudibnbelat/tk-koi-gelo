// FILE: app/src/main/java/com/masud/moneymanager/widget/WidgetAddTaskActivity.kt
package com.masud.moneymanager.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TaskEntity
import com.masud.moneymanager.ui.AddTaskSheet
import com.masud.moneymanager.ui.MoneyManagerTheme
import com.masud.moneymanager.ui.TaskAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WidgetAddTaskActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyManagerTheme(darkTheme = isSystemInDarkTheme()) {
                AddTaskSheet(
                        editing = null,
                        onDismiss = { finish() },
                        onSave = { title, details, amount, dateMillis, exactMillis ->
                            Log.d("WidgetAdd", "onSave called")
                            activityScope.launch {
                                try {
                                    val appCtx = applicationContext
                                    val id =
                                            AppDatabase.getInstance(appCtx)
                                                    .taskDao()
                                                    .insert(
                                                            TaskEntity(
                                                                    title = title,
                                                                    details = details,
                                                                    amount = amount,
                                                                    remindDate = dateMillis,
                                                                    remindAt = exactMillis
                                                            )
                                                    )
                                    Log.d("WidgetAdd", "insert done, id=$id")
                                    runCatching {
                                        TaskAlarmScheduler.schedule(
                                                appCtx,
                                                TaskEntity(
                                                        id = id,
                                                        title = title,
                                                        details = details,
                                                        amount = amount,
                                                        remindDate = dateMillis,
                                                        remindAt = exactMillis
                                                )
                                        )
                                    }
                                            .onFailure {
                                                Log.e("WidgetAdd", "alarm schedule failed", it)
                                            }
                                    // Path 1: Glance updateAll with Activity context
                                    runCatching {
                                        val manager = GlanceAppWidgetManager(appCtx)
                                        val ids = manager.getGlanceIds(TaskWidget::class.java)
                                        Log.d("WidgetAdd", "glanceIds=$ids")
                                        TaskWidget().updateAll(this@WidgetAddTaskActivity)
                                        Log.d("WidgetAdd", "updateAll returned")
                                    }
                                            .onFailure {
                                                Log.e("WidgetAdd", "widget update failed", it)
                                            }
                                    // Path 2: System broadcast (official update path)
                                    runCatching {
                                        val mgr = AppWidgetManager.getInstance(appCtx)
                                        val comp =
                                                ComponentName(
                                                        appCtx,
                                                        TaskWidgetReceiver::class.java
                                                )
                                        val widgetIds = mgr.getAppWidgetIds(comp)
                                        Log.d("WidgetAdd", "broadcasting for ${widgetIds.toList()}")
                                        val updateIntent =
                                                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                                                        .apply {
                                                            putExtra(
                                                                    AppWidgetManager
                                                                            .EXTRA_APPWIDGET_IDS,
                                                                    widgetIds
                                                            )
                                                            `package` = appCtx.packageName
                                                        }
                                        appCtx.sendBroadcast(updateIntent)
                                        Log.d("WidgetAdd", "broadcast sent")
                                    }
                                            .onFailure {
                                                Log.e("WidgetAdd", "broadcast failed", it)
                                            }
                                } catch (e: Exception) {
                                    Log.e("WidgetAdd", "save flow failed", e)
                                } finally {
                                    finish()
                                }
                            }
                        }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
