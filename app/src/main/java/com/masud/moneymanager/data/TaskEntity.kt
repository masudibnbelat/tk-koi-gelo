// FILE: app/src/main/java/com/masud/moneymanager/data/TaskEntity.kt
package com.masud.moneymanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val details: String = "",
        val amount: Double = 0.0,
        val remindAt: Long? = null,
        val remindDate: Long? = null,
        val isDone: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
)
