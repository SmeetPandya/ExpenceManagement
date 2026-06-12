package com.smeet.expencemanagement.repository

import com.smeet.expencemanagement.model.Expence
import com.smeet.expencemanagement.model.ExpenseDao

class ExpenseRepository(private val expenseDao: ExpenseDao) {

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
}