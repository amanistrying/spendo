package com.amangupta.spendo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchantVpa: String,   // NEW: e.g. "zomato@icici"
    val merchant: String,      // display name
    val category: String,
    val timestamp: Long,
    val rawMessage: String,
    val bankRef: String = ""   // NEW: UPI ref number
)
