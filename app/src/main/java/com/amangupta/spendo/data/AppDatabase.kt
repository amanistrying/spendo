package com.amangupta.spendo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, CategoryRule::class, Category::class], 
          version = 4, 
          exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to existing transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN merchantVpa TEXT NOT NULL DEFAULT 'unknown@upi'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN bankRef TEXT NOT NULL DEFAULT ''")
                // Drop the unique index on rawMessage if it exists
                db.execSQL("DROP INDEX IF EXISTS index_transactions_rawMessage")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spendo_database"
                )
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
