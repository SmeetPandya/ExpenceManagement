package com.smeet.expencemanagement.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expence_table")
data class Expence(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val amount:Double,
    val category:String,
    val note: String="",
    val date: Long= System.currentTimeMillis()
)
