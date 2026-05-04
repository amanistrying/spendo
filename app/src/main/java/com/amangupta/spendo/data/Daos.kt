package com.amangupta.spendo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :start AND timestamp <= :end")
    fun getTotalInRange(start: Long, end: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM transactions GROUP BY category")
    fun getSpendingByCategory(): Flow<List<CategorySpending>>

    @Query("SELECT rawMessage FROM transactions WHERE category = 'Unknown' GROUP BY rawMessage")
    fun getUnknownRawMessages(): Flow<List<String>>

    @Query("UPDATE transactions SET merchant = :merchant, category = :category WHERE rawMessage = :rawMessage")
    suspend fun updateTransactionByRawMessage(rawMessage: String, merchant: String, category: String)
}

data class CategorySpending(
    val category: String,
    val total: Double
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules")
    fun getAllRules(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules WHERE merchant = :merchant LIMIT 1")
    suspend fun getRuleForMerchant(merchant: String): CategoryRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRule)

    @Delete
    suspend fun delete(rule: CategoryRule)
}
