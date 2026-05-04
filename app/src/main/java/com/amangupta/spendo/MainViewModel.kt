package com.amangupta.spendo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewModelScope
import com.amangupta.spendo.data.AppDatabase
import com.amangupta.spendo.data.*
import com.amangupta.spendo.sms.SmsHistoryLoader
import com.amangupta.spendo.sms.SmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val categoryRuleDao = db.categoryRuleDao()
    private val categoryDao = db.categoryDao()

    val allTransactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRules: StateFlow<List<CategoryRule>> = categoryRuleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendingByCategory: StateFlow<List<CategorySpending>> = transactionDao.getSpendingByCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unknownMerchants: StateFlow<List<String>> = transactionDao.getUnknownRawMessages()
        .map { messages ->
            messages.mapNotNull { SmsParser.parse(it)?.merchant }.distinct()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)

    val filteredTransactions: StateFlow<List<Transaction>> = combine(allTransactions, _startDate, _endDate) { list, start, end ->
        if (start == null || end == null) list
        else list.filter { it.timestamp in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    val todayTransactions: StateFlow<List<Transaction>> = getStartOfToday().let { start ->
        transactionDao.getTransactionsInRange(start, Long.MAX_VALUE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val todayExpense: StateFlow<Double> = getStartOfToday().let { start ->
        transactionDao.getTotalInRange(start, Long.MAX_VALUE)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    val monthlyExpense: StateFlow<Double> = getStartOfMonth().let { start ->
        transactionDao.getTotalInRange(start, Long.MAX_VALUE)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    fun syncHistory(days: Int) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (days.toLong() * 24 * 60 * 60 * 1000)
            SmsHistoryLoader.loadHistory(getApplication(), startTime, endTime)
        }
    }

    fun saveRule(merchant: String, friendlyName: String, category: String) {
        viewModelScope.launch {
            categoryRuleDao.insert(CategoryRule(merchant, friendlyName, category))
            // Also update existing "Unknown" transactions with this merchant
            val unknownMessages = transactionDao.getUnknownRawMessages().first()
            unknownMessages.forEach { raw ->
                val parsed = SmsParser.parse(raw)
                if (parsed?.merchant == merchant) {
                    transactionDao.updateTransactionByRawMessage(raw, friendlyName, category)
                }
            }
        }
    }

    fun addCategory(name: String, color: String = "#FF0000") {
        viewModelScope.launch {
            categoryDao.insert(Category(name, color))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.delete(category)
        }
    }

    fun deleteRule(rule: CategoryRule) {
        viewModelScope.launch {
            categoryRuleDao.delete(rule)
        }
    }

    private fun getStartOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
