package com.amangupta.spendo

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amangupta.spendo.data.*
import com.amangupta.spendo.sms.SmsHistoryLoader
import com.amangupta.spendo.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
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

    val unknownMerchants: StateFlow<List<UnknownMerchant>> = transactionDao
        .getUnknownTransactionMerchants()
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

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    fun selectCategory(category: String?) { _selectedCategory.value = category }

    val categoryFilteredTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, _selectedCategory) { txs, cat ->
            if (cat == null) txs else txs.filter { it.category == cat }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    fun searchResults(query: String): Flow<List<Transaction>> {
        if (query.isBlank()) return flowOf(emptyList())
        return transactionDao.search(query).debounce(300)
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

    fun saveRule(vpa: String, friendlyName: String, category: String) {
        viewModelScope.launch {
            categoryRuleDao.insert(CategoryRule(vpa, friendlyName, category))
            transactionDao.getByCategory("Unknown").first()
                .filter { it.merchantVpa == vpa }
                .forEach { transactionDao.updateCategoryById(it.id, friendlyName, category) }
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

    fun exportCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val txs = transactionDao.getAllTransactions().first()
            val csv = buildString {
                appendLine("Date,Merchant,VPA,Category,Amount,Reference")
                txs.forEach { tx ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(tx.timestamp))
                    appendLine("$date,${tx.merchant},${tx.merchantVpa},${tx.category},${tx.amount},${tx.bankRef}")
                }
            }
            val cacheDir = File(context.cacheDir, "exports")
            cacheDir.mkdirs()
            val file = File(cacheDir, "spendo_export.csv")
            file.writeText(csv)
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Spendo Expense Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Expenses").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
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
