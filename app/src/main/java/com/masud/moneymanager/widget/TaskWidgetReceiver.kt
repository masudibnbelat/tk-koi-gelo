// FILE: app/src/main/java/com/masud/moneymanager/widget/TaskWidgetReceiver.kt
package com.masud.moneymanager.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}
