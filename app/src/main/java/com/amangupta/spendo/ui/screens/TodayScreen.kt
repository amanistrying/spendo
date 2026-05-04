package com.amangupta.spendo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amangupta.spendo.MainViewModel
import com.amangupta.spendo.ui.components.EmptyState
import com.amangupta.spendo.ui.components.SummaryCardLarge
import com.amangupta.spendo.ui.components.TransactionItem

@Composable
fun TodayTab(viewModel: MainViewModel) {
    val transactions by viewModel.todayTransactions.collectAsState()
    val total by viewModel.todayExpense.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SummaryCardLarge("Today's Spend", total)
        if (transactions.isEmpty()) {
            EmptyState("No transactions today.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions) { transaction ->
                    TransactionItem(transaction)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        }
    }
}
