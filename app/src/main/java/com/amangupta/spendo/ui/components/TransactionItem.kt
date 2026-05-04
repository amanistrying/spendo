package com.amangupta.spendo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amangupta.spendo.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummaryCardLarge(title: String, amount: Double, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                "₹${String.format(Locale.getDefault(), "%,.0f", amount)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.merchant, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(transaction.category, fontSize = 12.sp, color = if (transaction.category == "Unknown") Color.Red else Color.Gray)
            Text(dateFormat.format(Date(transaction.timestamp)), fontSize = 10.sp, color = Color.LightGray)
        }
        Text(
            "₹${String.format(Locale.getDefault(), "%.2f", transaction.amount)}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
