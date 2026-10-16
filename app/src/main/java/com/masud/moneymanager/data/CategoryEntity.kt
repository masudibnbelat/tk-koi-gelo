// FILE: app/src/main/java/com/masud/moneymanager/data/CategoryEntity.kt
package com.masud.moneymanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val createdAt: Long = System.currentTimeMillis(),
)
