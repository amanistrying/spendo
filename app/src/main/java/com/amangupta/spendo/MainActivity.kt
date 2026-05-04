package com.amangupta.spendo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amangupta.spendo.data.Transaction
import com.amangupta.spendo.ui.theme.SpendoTheme
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.InputChip
import com.amangupta.spendo.data.CategoryRule
import com.amangupta.spendo.data.Category

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendoTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] == true
    }

    LaunchedEffect(Unit) {
        if (!hasSmsPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "Month", "Analysis", "Merchants")
    val icons = listOf(Icons.Default.Today, Icons.Default.DateRange, Icons.Default.PieChart, Icons.Default.Storefront)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.syncHistory(30) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync History")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (hasSmsPermission) {
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> TodayTab(viewModel)
                    1 -> MonthTab(viewModel)
                    2 -> AnalysisTab(viewModel)
                    3 -> MerchantsTab(viewModel)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.permission_rationale), modifier = Modifier.padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun TodayTab(viewModel: MainViewModel) {
    val transactions by viewModel.todayTransactions.collectAsState()
    val total by viewModel.todayExpense.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SummaryCardLarge("Today's Spend", total)
        TransactionListSimple(transactions)
    }
}

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
            Text("Monthly History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                viewModel.setFilterRange(start, end)
                showFilter = false
            }
        )
    }
}

@Composable
fun AnalysisTab(viewModel: MainViewModel) {
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Spending Analysis", modifier = Modifier.padding(16.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        if (spendingByCategory.isEmpty()) {
            EmptyState("Not enough data for analysis.")
        } else {
            Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    SimplePieChart(spendingByCategory)
                }
            }

            Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    spendingByCategory.sortedByDescending { it.total }.forEach { spending ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(spending.category)
                            Text("₹${String.format(Locale.getDefault(), "%.2f", spending.total)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimplePieChart(data: List<com.amangupta.spendo.data.CategorySpending>) {
    val total = data.sumOf { it.total }
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan)
    
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = -90f
            data.forEachIndexed { index, item ->
                val sweepAngle = (item.total / total * 360).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}

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
                "₹${String.format(Locale.getDefault(), "%.2f", amount)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionListSimple(transactions: List<Transaction>) {
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

@Composable
fun MerchantsTab(viewModel: MainViewModel) {
    val rules by viewModel.allRules.collectAsState()
    val unknownMerchants by viewModel.unknownMerchants.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf<String?>(null) } 
    var showCatDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = { showCatDialog = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                    Icon(Icons.Default.Category, contentDescription = "Add Category")
                }
                FloatingActionButton(onClick = { showAddDialog = "" }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Merchant")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(scrollState)) {
            if (unknownMerchants.isNotEmpty()) {
                Text(
                    "Action Required: Unknown Merchants",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Card(modifier = Modifier.padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))) {
                    Column {
                        unknownMerchants.forEach { merchant ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showAddDialog = merchant }.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(merchant, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            if (categories.isNotEmpty()) {
                Text(
                    "Custom Categories",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.deleteCategory(cat) },
                            label = { Text(cat.name) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Text(
                "Configured Merchants",
                modifier = Modifier.padding(16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (rules.isEmpty()) {
                EmptyState("No merchants configured.\nAdd rules to identify your payments.")
            } else {
                Column {
                    rules.forEach { rule ->
                        MerchantRuleItem(rule, onDelete = { viewModel.deleteRule(rule) })
                    }
                }
            }
        }
    }

    if (showAddDialog != null) {
        AddMerchantDialog(
            initialVpa = showAddDialog!!,
            customCategories = categories.map { it.name },
            onDismiss = { showAddDialog = null },
            onSave = { vpa, name, cat ->
                viewModel.saveRule(vpa, name, cat)
                showAddDialog = null
            }
        )
    }

    if (showCatDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCatDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Category Name") })
            },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        viewModel.addCategory(newCatName)
                        showCatDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showCatDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MerchantRuleItem(rule: CategoryRule, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.friendlyName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(rule.merchant, fontSize = 12.sp, color = Color.Gray)
                Text("Category: ${rule.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddMerchantDialog(initialVpa: String = "", customCategories: List<String> = emptyList(), onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var vpa by remember { mutableStateOf(initialVpa) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    val defaultCategories = listOf("Food", "Shopping", "Travel", "Fuel", "Bills", "Health", "Others")
    val allCategories = (defaultCategories + customCategories).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialVpa.isEmpty()) "Add Merchant Rule" else "Classify Unknown Merchant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vpa, onValueChange = { vpa = it }, label = { Text("UPI ID / Merchant Name") }, enabled = initialVpa.isEmpty())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Friendly Name (e.g. Juice Shop)") })
                Text("Select Category", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(allCategories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { category = cat }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat)
                            if (category == cat) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (vpa.isNotBlank() && name.isNotBlank()) onSave(vpa, name, category) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
            color = MaterialTheme.colorScheme.error
        )
    }
}
