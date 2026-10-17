// FILE: app/src/main/java/com/masud/moneymanager/widget/TaskDeepLink.kt
package com.masud.moneymanager.widget

data class TaskDeepLink(
        val nonce: Long = 0L,
        val openTasksTab: Boolean = false,
        val openAddSheet: Boolean = false,
        val editTaskId: Long? = null
) {
        val isEmpty: Boolean get() = !openTasksTab && !openAddSheet && editTaskId == null
}
