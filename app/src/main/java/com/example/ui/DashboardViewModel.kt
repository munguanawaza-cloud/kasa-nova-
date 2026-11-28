package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CurrencyType
import com.example.data.local.PaymentChannel
import com.example.data.local.SyncStatus
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao: TransactionDao =
        AppDatabase.getDatabase(application).transactionDao()

    val cashBalanceUsd: StateFlow<Double> = transactionDao.getCashBalanceUSD()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val cashBalanceCdf: StateFlow<Double> = transactionDao.getCashBalanceCDF()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalSalesUsd: StateFlow<Double> = transactionDao.getTotalSalesUSD()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalExpensesUsd: StateFlow<Double> = transactionDao.getTotalExpensesUSD()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalSalesCdf: StateFlow<Double> = transactionDao.getTotalSalesCDF()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalExpensesCdf: StateFlow<Double> = transactionDao.getTotalExpensesCDF()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val transactions: StateFlow<List<TransactionEntity>> = transactionDao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        currency: CurrencyType,
        paymentChannel: PaymentChannel,
        customerName: String? = null,
        description: String? = null
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            transactionDao.insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    currency = currency,
                    paymentChannel = paymentChannel,
                    customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                    timestamp = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
    }
}
