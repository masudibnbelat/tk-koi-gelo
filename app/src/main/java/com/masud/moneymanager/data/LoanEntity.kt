
package com.masud.moneymanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val personName: String,
    val type: String, // "LENT" or "BORROWED"
    val principalAmount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
