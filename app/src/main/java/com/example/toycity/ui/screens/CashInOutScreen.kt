package com.example.toycity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.utils.Formatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInOutScreen(viewModel: FinancialViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val displayTransactions = remember(uiState.cashTransactions) {
        uiState.cashTransactions.filter { 
            val cat = it.category.lowercase().trim()
            it.isCashIn || (cat != "operational" && cat != "restock" && cat != "loan repayment")
        }.sortedByDescending { it.date }
    }

    val totalCashIn = displayTransactions.filter { it.isCashIn }.sumOf { it.amount }
    val totalCashOut = displayTransactions.filter { !it.isCashIn }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Ledger") },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset All", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Initial Cash Field
            OutlinedTextField(
                value = if (uiState.startingCash == 0.0) "" else uiState.startingCash.toString(),
                onValueChange = { 
                    val value = it.toDoubleOrNull() ?: 0.0
                    viewModel.updateStartingCash(value)
                },
                label = { Text("Opening Balance (Cash in Hand)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                supportingText = { Text("Setting this to 0 resets the starting point for this month.") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("General Cash In", style = MaterialTheme.typography.labelMedium)
                        Text(Formatter.formatCurrency(totalCashIn), style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("General Cash Out", style = MaterialTheme.typography.labelMedium)
                        Text(Formatter.formatCurrency(totalCashOut), style = MaterialTheme.typography.titleLarge, color = Color(0xFFC62828))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Cash Ledger (Excl. Op/Restock/Debt)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(displayTransactions) { transaction ->
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(transaction.note)
                                if (!transaction.isCashIn) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(transaction.category, fontSize = 9.sp)
                                    }
                                }
                            }
                        },
                        supportingContent = { Text(Formatter.formatDate(Date(transaction.date))) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (if (transaction.isCashIn) "+" else "-") + Formatter.formatCurrency(transaction.amount),
                                    color = if (transaction.isCashIn) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.removeCashTransaction(transaction.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        leadingContent = {
                            Icon(
                                if (transaction.isCashIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (transaction.isCashIn) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Monthly Records?") },
            text = { Text("This will set Starting Cash, Sales, and Expenses for THIS month back to 0. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetMonthlyLedger()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog) {
        CashTransactionDialog(
            viewModel = viewModel,
            items = uiState.inventoryData.items,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, note, isCashIn, date, category, productId ->
                viewModel.addCashTransaction(amount, note, isCashIn, date, category, productId)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashTransactionDialog(
    viewModel: com.example.toycity.ui.FinancialViewModel,
    items: List<com.example.toycity.data.InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Boolean, Long, String, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCashIn by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf("Other") }
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Cash Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = isCashIn,
                        onClick = { isCashIn = true },
                        label = { Text("Cash In") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isCashIn,
                        onClick = { isCashIn = false },
                        label = { Text("Cash Out") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isCashIn) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    val customCategories by viewModel.uiState.collectAsState()
                    val allCategories = listOf("Other", "Restock", "Operational") + customCategories.customCategories
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allCategories.distinct().forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { 
                                    selectedCategory = category
                                    if (category != "Restock") selectedProductId = null
                                },
                                label = { Text(category, fontSize = 10.sp) }
                            )
                        }
                    }

                    if (selectedCategory == "Restock") {
                        Text("Linked Product", style = MaterialTheme.typography.labelMedium)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = items.find { it.id == selectedProductId }?.name ?: "Select Product (Optional)",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                items.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name) },
                                        onClick = {
                                            selectedProductId = item.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date: ${Formatter.formatDate(Date(selectedDate))}")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                onConfirm(amt, note, isCashIn, selectedDate, if (isCashIn) "Income" else selectedCategory, selectedProductId)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
