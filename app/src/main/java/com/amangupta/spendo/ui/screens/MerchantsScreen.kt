package com.amangupta.spendo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.amangupta.spendo.data.CategoryRule
import com.amangupta.spendo.data.UnknownMerchant
import com.amangupta.spendo.ui.components.EmptyState

@Composable
fun MerchantsTab(viewModel: MainViewModel) {
    val rules by viewModel.allRules.collectAsState()
    val unknownMerchants by viewModel.unknownMerchants.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf<UnknownMerchant?>(null) } 
    var showCatDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = { showCatDialog = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                    Icon(Icons.Default.Category, contentDescription = "Add Category")
                }
                FloatingActionButton(onClick = { showAddDialog = UnknownMerchant("", "") }) {
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
                            UnknownMerchantRow(merchant) { showAddDialog = merchant }
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
                FlowRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        @OptIn(ExperimentalMaterial3Api::class)
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
                "Configured Rules",
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
            initialMerchant = showAddDialog!!,
            customCategories = categories.map { it.name },
            onDismiss = { showAddDialog = null },
            onSave = { vpa, name, cat ->
                viewModel.saveRule(vpa, name, cat)
                showAddDialog = null
            }
        )
    }

    if (showCatDialog) {
        AddCategoryDialog(
            onDismiss = { showCatDialog = false },
            onSave = { name ->
                viewModel.addCategory(name)
                showCatDialog = false
            }
        )
    }
}

@Composable
fun UnknownMerchantRow(merchant: UnknownMerchant, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(merchant.merchant, fontWeight = FontWeight.Bold)
            Text(merchant.merchantVpa, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
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
fun AddMerchantDialog(
    initialMerchant: UnknownMerchant, 
    customCategories: List<String> = emptyList(), 
    onDismiss: () -> Unit, 
    onSave: (String, String, String) -> Unit
) {
    var vpa by remember { mutableStateOf(initialMerchant.merchantVpa) }
    var name by remember { mutableStateOf(initialMerchant.merchant) }
    var category by remember { mutableStateOf("Food") }
    val defaultCategories = listOf("Food", "Shopping", "Travel", "Fuel", "Bills", "Health", "Others")
    val allCategories = (defaultCategories + customCategories).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMerchant.merchantVpa.isEmpty()) "Add Merchant Rule" else "Classify Merchant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vpa, 
                    onValueChange = { vpa = it }, 
                    label = { Text("UPI ID / Merchant VPA") },
                    placeholder = { Text("e.g. merchant@upi") }
                )
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Juice Corner") }
                )
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
fun AddCategoryDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newCatName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Category") },
        text = {
            OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Category Name") })
        },
        confirmButton = {
            Button(onClick = {
                if (newCatName.isNotBlank()) {
                    onSave(newCatName)
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
