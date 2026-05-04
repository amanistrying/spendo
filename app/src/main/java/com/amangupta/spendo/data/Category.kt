package com.amangupta.spendo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val color: String // Hex color string for charts
)
