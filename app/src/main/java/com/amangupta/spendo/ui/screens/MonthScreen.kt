package com.amangupta.spendo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amangupta.spendo.MainViewModel
import com.amangupta.spendo.data.Transaction
import com.amangupta.spendo.ui.components.EmptyState
import com.amangupta.spendo.ui.components.SummaryCardLarge
import com.amangupta.spendo.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthTab(viewModel: MainViewModel) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val total = transactions.sumOf { it.amount }
    var showFilter by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showFilter = true }) {
                Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
            }
        }
        SummaryCardLarge("Total for Period", total, MaterialTheme.colorScheme.secondaryContainer)
        CollapsibleTransactionList(transactions)
    }

    if (showFilter) {
        DateRangePickerDialog(
            onDismiss = { showFilter = false },
            onRangeSelected = { start, end ->
                viewModel.setFilterRange(if (start == 0L && end == 0L) null else start, if (start == 0L && end == 0L) null else end)
                showFilter = false
            }
        )
    }
}

@Composable
fun CollapsibleTransactionList(transactions: List<Transaction>) {
    if (transactions.isEmpty()) {
        EmptyState("No transactions recorded.")
    } else {
        val grouped = remember(transactions) {
            transactions.groupBy { 
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.timestamp)) 
            }
        }
        val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (date, dailyTransactions) ->
                val isExpanded = expandedStates[date] ?: true
                item {
                    DateHeaderCollapsible(date, isExpanded) {
                        expandedStates[date] = !isExpanded
                    }
                }
                if (isExpanded) {
                    items(dailyTransactions) { transaction ->
                        TransactionItem(transaction)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeaderCollapsible(date: String, isExpanded: Boolean, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = date, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(onDismiss: () -> Unit, onRangeSelected: (Long, Long) -> Unit) {
    val state = rememberDateRangePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis
                if (start != null && end != null) {
                    onRangeSelected(start, end)
                }
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = {
                onRangeSelected(0L, 0L) // Reset
                onDismiss()
            }) { Text("Reset") }
        }
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}
