package com.smeet.expencemanagement.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_bills_table")
data class ScheduledBill(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val billName: String,
    val amount: Double,
    val category: String,
    val dueDate: Long,
    val isPaid: Boolean=false
)
