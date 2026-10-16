// FILE: app/src/main/java/com/masud/moneymanager/ui/TaskReminders.kt
package com.masud.moneymanager.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.masud.moneymanager.R
import com.masud.moneymanager.data.AppDatabase
import com.masud.moneymanager.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TASK_CHANNEL_ID = "task_reminders"

object TaskAlarmScheduler {

    fun schedule(context: Context, task: TaskEntity) {
        cancel(context, task.id)
        if (task.isDone) return
        val now = System.currentTimeMillis()
        val triggerTimes = mutableListOf<Long>()

        // Time set thakle exact oi time e ekbar
        task.remindAt?.let { if (it > now) triggerTimes.add(it) }

        // Sudhu date thakle oi dine 3 bar: sokal 9ta, dupur 1ta, sondha 6ta
        task.remindDate?.let { dayStart ->
            listOf(9, 13, 18).forEach { hour ->
                val at = Calendar.getInstance().apply {
                    timeInMillis = dayStart
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (at > now) triggerTimes.add(at)
            }
        }

        if (triggerTimes.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()

        triggerTimes.forEachIndexed { index, at ->
            val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                putExtra("task_id", task.id)
                putExtra("task_title", task.title)
                putExtra("task_details", task.details)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmRequestCode(task.id, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            }
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..2) {
            val intent = Intent(context, TaskAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmRequestCode(taskId, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun alarmRequestCode(taskId: Long, index: Int): Int =
        (taskId * 10 + index).toInt()
}

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        val title = intent.getStringExtra("task_title") ?: return
        val details = intent.getStringExtra("task_details").orEmpty()

        // Alarm fire howar age task complete/delete hoye gele notify korbe na
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getById(taskId)
                if (task != null && !task.isDone) {
                    showNotification(context, taskId, title, details)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, title: String, details: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    TASK_CHANNEL_ID,
                    "Task reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_bell)
            .setContentTitle(title)
            .setContentText(details.ifBlank { "Task reminder" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        manager.notify(taskId.toInt(), notification)
    }
}

class TaskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Phone restart hole pending reminder gula abar schedule hobe
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                db.taskDao().observeAll().first()
                    .filter { !it.isDone }
                    .forEach { TaskAlarmScheduler.schedule(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
