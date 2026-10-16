
package com.masud.moneymanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String = "General",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
