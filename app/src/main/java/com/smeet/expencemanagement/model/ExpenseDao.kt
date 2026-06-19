package com.smeet.expencemanagement.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpence(expence: Expence)

    @Delete
    suspend fun deleteExpence(expence: Expence)

    @Update
    suspend fun updateExpence(expence: Expence)

    @Query("SELECT * FROM expence_table ORDER BY date DESC")
    fun getAllExpences(): Flow<List<Expence>>

    @Query("SELECT SUM(amount) FROM expence_table")
    fun getTotalBalance():Flow<Double?>

    @Query("DELETE FROM expence_table")
    suspend fun deleteAll()

}