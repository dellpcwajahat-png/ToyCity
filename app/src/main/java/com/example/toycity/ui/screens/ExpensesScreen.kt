package com.example.toycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.utils.Formatter
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: com.example.toycity.ui.FinancialViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    type: String = "All",
    currentMonth: String,
    onMonthSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val isAllTimeView by viewModel.isAllTimeView.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isAllTimeView,
                    onClick = { viewModel.setAllTimeView(!isAllTimeView) },
                    label = { Text(if (isAllTimeView) "All-Time" else "Monthly") },
                    leadingIcon = if (isAllTimeView) {
                        {
                            Icon(
                                Icons.Default.AllInclusive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )

                if (!isAllTimeView) {
                    IconButton(onClick = { onMonthSelected(currentMonth) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currentMonth,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Pick Month",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            val expenseTransactions = remember(uiState, allRecords, isAllTimeView, type) {
                val transactions = if (isAllTimeView) {
                    allRecords.flatMap { it.cashTransactions }
                } else {
                    uiState.cashTransactions
                }
                
                transactions.filter { 
                    !it.isCashIn && (type == "All" || it.category.equals(type, ignoreCase = true))
                }.sortedByDescending { it.date }
            }

            if (expenseTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Payments,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text("No $type expenses recorded", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenseTransactions) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(expense.note, fontWeight = FontWeight.Bold) },
                                supportingContent = { 
                                    Column {
                                        Text(Formatter.formatDate(Date(expense.date)))
                                        if (type == "All") {
                                            Text("Category: ${expense.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            Formatter.formatCurrency(expense.amount),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        IconButton(onClick = { viewModel.removeCashTransaction(expense.id) }) {
                                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
        }
    }

    if (showAddDialog) {
        val initialType = when (type) {
            "Operational" -> "Operational"
            "Restock" -> "Restock"
            else -> "Operational"
        }
        AddExpenseDialog(
            type = initialType,
            isAllView = type == "All",
            customCategories = uiState.customCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, note, timestamp, selectedType ->
                viewModel.addCashTransaction(amount, note, false, timestamp, selectedType)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    type: String, 
    isAllView: Boolean = false,
    customCategories: List<String> = emptyList(),
    onDismiss: () -> Unit, 
    onConfirm: (Double, String, Long, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(type) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAllView) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allCategories = listOf("Operational", "Restock") + customCategories
                        allCategories.distinct().forEach { category ->
                            FilterChip(
                                selected = selectedType == category,
                                onClick = { selectedType = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.replace(".", "").all { c -> c.isDigit() }) amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Expense Detail / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Expense Date", style = MaterialTheme.typography.labelSmall)
                            Text(Formatter.formatDate(Date(selectedDate)), fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && note.isNotEmpty()) onConfirm(amt, note, selectedDate, selectedType)
                },
                enabled = amount.isNotEmpty() && note.isNotEmpty()
            ) { Text("Add Expense") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
