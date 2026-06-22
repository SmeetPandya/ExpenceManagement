package com.smeet.expencemanagement.repository

import com.smeet.expencemanagement.model.Expence
import com.smeet.expencemanagement.model.ExpenseDao
import com.smeet.expencemanagement.model.ScheduledBill
import com.smeet.expencemanagement.model.ScheduledBillDao
import kotlin.math.exp

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val scheduledBillDao: ScheduledBillDao
) {

    val allExpenses=expenseDao.getAllExpences()

    val totalBalance=expenseDao.getTotalBalance()

    suspend fun insert(expence: Expence){
        expenseDao.insertExpence(expence)
    }

    suspend fun delete(expence: Expence){
        expenseDao.deleteExpence(expence)
    }

    suspend fun edit(expence: Expence){
        expenseDao.updateExpence(expence)
    }

    suspend fun deleteAllExpence(){
        expenseDao.deleteAll()
    }

    val allScheduledBill=scheduledBillDao.getAllScheduledBills()

    suspend fun insertScheduledBills(bill: ScheduledBill){
        scheduledBillDao.insert(bill)
    }

    suspend fun deleteScheduledBills(bill: ScheduledBill){
        scheduledBillDao.delete(bill)
    }

    suspend fun updateScheduledBill(bill: ScheduledBill) {
        scheduledBillDao.update(bill)
    }
}