// FILE: app/src/main/java/com/masud/moneymanager/widget/WidgetAddTaskActivity.kt
package com.masud.moneymanager.widget

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TaskEntity
import com.masud.moneymanager.ui.AddTaskSheet
import com.masud.moneymanager.ui.MoneyManagerTheme
import com.masud.moneymanager.ui.TaskAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetAddTaskActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Heartbeat keeps the Glance session alive while the sheet is open.
        activityScope.launch {
            while (true) {
                // Immediate manual push (bypasses Glance worker).
                ManualWidgetHelper.pushUpdate(appCtx)
                // Glance refresh for consistency (when worker runs).
                refreshTaskWidgets(appCtx)

                refreshTaskWidgets(applicationContext)
                delay(25_000)
            }
        }
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
                                    // Refetch: bumps WidgetRefreshSignal so the live
                                    // widget re-queries the DB on the next frame.
                                    refreshTaskWidgets(appCtx)
                                    Log.d("WidgetAdd", "widget refreshed")
                                    delay(3000)
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
