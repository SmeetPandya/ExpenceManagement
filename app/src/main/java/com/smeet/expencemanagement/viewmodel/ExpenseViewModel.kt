package com.smeet.expencemanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smeet.expencemanagement.model.Expence
import com.smeet.expencemanagement.model.ScheduledBill
import com.smeet.expencemanagement.repository.ExpenseRepository
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository): ViewModel() {

    val allExpenses=repository.allExpenses
    val totalBalance=repository.totalBalance

    fun insert(expence: Expence){
        viewModelScope.launch {
            repository.insert(expence)
        }
    }

    fun update(expence: Expence){
        viewModelScope.launch {
            repository.edit(expence)
        }
    }

    fun delete(expence: Expence){
        viewModelScope.launch {
            repository.delete(expence)
        }
    }

    fun deleteAllExpence()=viewModelScope.launch {
        repository.deleteAllExpence()
    }

    val allScheduledBill=repository.allScheduledBill

    fun insertScheduleBills(bill: ScheduledBill){
        viewModelScope.launch {
            repository.insertScheduledBills(bill)
        }
    }

    fun deleteScheduleBills(bill: ScheduledBill){
        viewModelScope.launch {
            repository.deleteScheduledBills(bill)
        }
    }

    fun updateScheduledBill(bill: ScheduledBill) {
        viewModelScope.launch {
            repository.updateScheduledBill(bill)
        }
    }

    fun deleteAllScheduledBills() = viewModelScope.launch {
        repository.deleteAllScheduledBills()
    }
}