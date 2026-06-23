package com.smeet.expencemanagement.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledBillDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bill: ScheduledBill)

    @Delete
    suspend fun delete(bill: ScheduledBill)

    @Update
    suspend fun update(bill: ScheduledBill)

    @Query("SELECT * FROM scheduled_bills_table ORDER BY dueDate ASC")
    fun getAllScheduledBills(): Flow<List<ScheduledBill>>

    @Query("SELECT * FROM scheduled_bills_table")
    suspend fun getAllBillSync() : List<ScheduledBill>
}