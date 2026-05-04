package com.amangupta.spendo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amangupta.spendo.MainViewModel
import com.amangupta.spendo.data.CategorySpending
import com.amangupta.spendo.ui.charts.DonutChart
import com.amangupta.spendo.ui.charts.categoryColor
import com.amangupta.spendo.ui.components.EmptyState
import com.amangupta.spendo.ui.components.SummaryCardLarge
import com.amangupta.spendo.ui.components.TransactionItem
import java.util.*

@Composable
fun AnalysisTab(viewModel: MainViewModel) {
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredTxs by viewModel.categoryFilteredTransactions.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Month total header
        val monthTotal = spendingByCategory.sumOf { it.total }
        SummaryCardLarge("This Month", monthTotal)

        if (spendingByCategory.isEmpty()) {
            EmptyState("No data yet. Make a UPI payment to get started.")
            return@Column
        }

        // Interactive donut
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                data = spendingByCategory,
                selectedCategory = selectedCategory,
                onCategoryClick = { cat ->
                    viewModel.selectCategory(if (selectedCategory == cat) null else cat)
                },
                modifier = Modifier.size(200.dp)
            )
        }

        // Category legend — tappable
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                spendingByCategory.sortedByDescending { it.total }.forEach { spending ->
                    CategoryLegendRow(spending, selectedCategory) {
                        viewModel.selectCategory(if (selectedCategory == spending.category) null else spending.category)
                    }
                }
            }
        }

        // Filtered transaction list when category is selected
        if (selectedCategory != null && filteredTxs.isNotEmpty()) {
            Text(
                "Transactions in $selectedCategory",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
            LazyColumn {
                items(filteredTxs) { tx ->
                    TransactionItem(tx)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryLegendRow(spending: CategorySpending, selectedCategory: String?, onClick: () -> Unit) {
    val isSelected = selectedCategory == spending.category
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(12.dp)
                .background(categoryColor(spending.category), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                spending.category,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
        Text(
            "₹${String.format(Locale.getDefault(), "%,.0f", spending.total)}",
            fontWeight = FontWeight.Bold,
            color = categoryColor(spending.category)
        )
    }
}
