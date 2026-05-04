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

    @Query("SELECT DISTINCT merchantVpa, merchant FROM transactions WHERE category = 'Unknown'")
    fun getUnknownTransactionMerchants(): Flow<List<UnknownMerchant>>

    @Query("UPDATE transactions SET merchant = :merchant, category = :category WHERE merchantVpa = :merchantVpa")
    suspend fun updateTransactionByVpa(merchantVpa: String, merchant: String, category: String)

    @Query("UPDATE transactions SET merchant = :name, category = :category WHERE id = :id")
    suspend fun updateCategoryById(id: Long, name: String, category: String)

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE merchantVpa = :vpa AND amount = :amount
        AND timestamp BETWEEN :ts - 60000 AND :ts + 60000
    """)
    suspend fun countDuplicates(vpa: String, amount: Double, ts: Long): Int

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' OR merchantVpa LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Transaction>>
}

data class CategorySpending(
    val category: String,
    val total: Double
)

data class UnknownMerchant(val merchantVpa: String, val merchant: String)

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
