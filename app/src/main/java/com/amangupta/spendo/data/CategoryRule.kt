package com.amangupta.spendo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules")
data class CategoryRule(
    @PrimaryKey val merchant: String, // The raw merchant name or VPA
    val friendlyName: String,         // User-friendly name (e.g., "Juice Shop")
    val category: String              // Category (e.g., "Food")
)
