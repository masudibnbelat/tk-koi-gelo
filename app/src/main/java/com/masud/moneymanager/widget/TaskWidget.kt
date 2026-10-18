// FILE: app/src/main/java/com/masud/moneymanager/widget/TaskWidget.kt
package com.masud.moneymanager.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalState
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.background
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.masud.moneymanager.MainActivity
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val EXTRA_OPEN_TASKS = "tkkoigelo.extra.OPEN_TASKS"
const val EXTRA_ADD_TASK = "tkkoigelo.extra.ADD_TASK"
const val EXTRA_EDIT_TASK_ID = "tkkoigelo.extra.EDIT_TASK_ID"

private const val MAX_WIDGET_TASKS = 100
private val BorderDay = Color(0xCC1A1C1E)
private val BorderNight = Color(0xCCE6E0E9)

class TaskWidget : GlanceAppWidget() {

        override suspend fun provideGlance(context: Context, id: GlanceId) {
                Log.d("TaskWidget", "provideGlance called for $id")
                val dao = AppDatabase.getInstance(context).taskDao()
                val initial =
                        try {
                                dao.getPending().take(MAX_WIDGET_TASKS)
                        } catch (e: Exception) {
                                Log.e("TaskWidget", "provideGlance failed for $id", e)
                                throw e
                        }
                provideContent {
                        GlanceTheme {
                                val refreshVersion by WidgetRefreshSignal.version.collectAsState()
                                val pending by
                                        produceState(
                                                initialValue = initial,
                                                key1 = LocalState.current,
                                                key2 = refreshVersion
                                        ) {
                                                val fresh =
                                                        runCatching {
                                                                        dao.getPending()
                                                                                .take(
                                                                                        MAX_WIDGET_TASKS
                                                                                )
                                                                }
                                                                .getOrDefault(initial)
                                                WidgetCheckAnimation.syncRemoved(
                                                        fresh.map { it.id }.toSet()
                                                )
                                                Log.d(
                                                        "TaskWidget",
                                                        "tasks re-queried for $id: ${fresh.size}"
                                                )
                                                value = fresh
                                        }
                                val overlay by WidgetCheckAnimation.overlay.collectAsState()
                                val visible = pending.filter { it.id !in overlay.removedIds }
                                TaskWidgetContent(tasks = visible, stages = overlay.stages)
                        }
                }
        }

        @Composable
        private fun TaskWidgetContent(tasks: List<TaskEntity>, stages: Map<Long, Int>) {
                val context = LocalContext.current
                val isDark =
                        (context.resources.configuration.uiMode and
                                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val borderColor = if (isDark) BorderNight else BorderDay
                // Outer: border color + 1dp padding = thin border, inner transparent
                Column(
                        modifier =
                                GlanceModifier.fillMaxSize()
                                        .background(day = borderColor, night = borderColor)
                                        .padding(1.dp)
                ) {
                        Column(modifier = GlanceModifier.fillMaxSize().padding(11.dp)) {
                                Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "Tasks",
                                                modifier =
                                                        GlanceModifier.defaultWeight()
                                                                .clickable(
                                                                        actionStartActivity(
                                                                                taskIntent(
                                                                                        context,
                                                                                        addTask =
                                                                                                false,
                                                                                        editTaskId =
                                                                                                null
                                                                                )
                                                                        )
                                                                )
                                                                .padding(vertical = 6.dp),
                                                style =
                                                        TextStyle(
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = GlanceTheme.colors.onSurface
                                                        ),
                                                maxLines = 1
                                        )
                                        Text(
                                                text = "+ Add",
                                                modifier =
                                                        GlanceModifier.clickable(
                                                                        actionStartActivity(
                                                                                Intent(
                                                                                                context,
                                                                                                WidgetAddTaskActivity::class
                                                                                                        .java
                                                                                        )
                                                                                        .addFlags(
                                                                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                                                                        )
                                                                        )
                                                                )
                                                                .padding(8.dp),
                                                style =
                                                        TextStyle(
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = GlanceTheme.colors.primary
                                                        )
                                        )
                                }
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                if (tasks.isEmpty()) {
                                        Box(
                                                modifier =
                                                        GlanceModifier.fillMaxWidth()
                                                                .padding(vertical = 32.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text = "No pending tasks",
                                                        style =
                                                                TextStyle(
                                                                        fontSize = 13.sp,
                                                                        color =
                                                                                GlanceTheme.colors
                                                                                        .onSurfaceVariant
                                                                )
                                                )
                                        }
                                } else {
                                        // Scrollable list: header stays fixed, rows scroll inside
                                        // the widget bounds.
                                        LazyColumn(
                                                modifier =
                                                        GlanceModifier.fillMaxWidth()
                                                                .defaultWeight()
                                        ) {
                                                items(tasks.size) { index ->
                                                        val task = tasks[index]
                                                        TaskWidgetRow(
                                                                task = task,
                                                                context = context,
                                                                stage = stages[task.id] ?: 0
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }

        @Composable
        private fun TaskWidgetRow(task: TaskEntity, context: Context, stage: Int) {
                // stage: 0 = normal, 1 = checked bright, 2 = checked dim.
                // NOTE: GlanceTheme.colors.* are ColorProviders, not Colors — no alpha
                // fades possible, hence the two discrete stages.
                val checked = stage > 0
                Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // TaskWidgetRow এর ভেতরে checkbox Text এ fontSize বাড়াও:
                        Box(
                                modifier =
                                        GlanceModifier.clickable(
                                                        actionRunCallback<ToggleTaskDoneCallback>(
                                                                actionParametersOf(
                                                                        ToggleTaskDoneCallback
                                                                                .TaskIdKey to
                                                                                task.id
                                                                )
                                                        )
                                                )
                                                .padding(end = 10.dp), // ← cornerRadius(6.dp) নেই
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = if (checked) "☑" else "☐",
                                        style =
                                                TextStyle(
                                                        fontSize = 20.sp,
                                                        color =
                                                                if (stage == 1)
                                                                        GlanceTheme.colors.primary
                                                                else
                                                                        GlanceTheme.colors
                                                                                .onSurfaceVariant
                                                )
                                )
                        }

                        Column(
                                modifier =
                                        GlanceModifier.defaultWeight()
                                                .clickable(
                                                        actionStartActivity(
                                                                taskIntent(
                                                                        context,
                                                                        addTask = false,
                                                                        editTaskId = task.id
                                                                )
                                                        )
                                                )
                        ) {
                                Text(
                                        text = task.title,
                                        style =
                                                TextStyle(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color =
                                                                if (checked)
                                                                        GlanceTheme.colors
                                                                                .onSurfaceVariant
                                                                else GlanceTheme.colors.onSurface
                                                ),
                                        maxLines = 1
                                )
                                val secondLine = buildString {
                                        if (task.amount > 0) append(formatWidgetAmount(task.amount))
                                        widgetReminderLabel(task)?.let {
                                                if (isNotEmpty()) append("  ·  ")
                                                append(it)
                                        }
                                }
                                if (secondLine.isNotEmpty()) {
                                        Text(
                                                text = secondLine,
                                                style =
                                                        TextStyle(
                                                                fontSize = 11.sp,
                                                                color =
                                                                        GlanceTheme.colors
                                                                                .onSurfaceVariant
                                                        ),
                                                maxLines = 1
                                        )
                                }
                        }
                }
        }

        private fun taskIntent(context: Context, addTask: Boolean, editTaskId: Long?): Intent =
                Intent(Intent.ACTION_VIEW).apply {
                        setClass(context, MainActivity::class.java)
                        putExtra(EXTRA_OPEN_TASKS, true)
                        putExtra(EXTRA_ADD_TASK, addTask)
                        editTaskId?.let { putExtra(EXTRA_EDIT_TASK_ID, it) }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data =
                                Uri.parse(
                                        buildString {
                                                append("tkkoigelo://tasks")
                                                if (addTask) append("/add")
                                                editTaskId?.let { append("/$it") }
                                        }
                                )
                }
}

private fun widgetReminderLabel(task: TaskEntity): String? {
        val at = task.remindAt ?: return null
        if (at < System.currentTimeMillis() - 60_000L) return null
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = at }
        val sameDay =
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        return if (sameDay)
                "Today ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(at))}"
        else SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(at))
}

private fun formatWidgetAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) String.format(Locale.US, "%,.0f", amount)
        else String.format(Locale.US, "%,.2f", amount)
