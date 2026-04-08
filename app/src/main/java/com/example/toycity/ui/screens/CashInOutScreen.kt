package com.example.toycity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.utils.Formatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInOutScreen(viewModel: FinancialViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val totalCashIn = uiState.cashTransactions.filter { it.isCashIn }.sumOf { it.amount }
    val totalCashOut = uiState.cashTransactions.filter { !it.isCashIn }.sumOf { it.amount }

    Scaffold(
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
                    viewModel.triggerManualSave()
                },
                label = { Text("Initial Cash / Starting Balance") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Wallet, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Cash In", style = MaterialTheme.typography.labelMedium)
                        Text(Formatter.formatCurrency(totalCashIn), style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Cash Out", style = MaterialTheme.typography.labelMedium)
                        Text(Formatter.formatCurrency(totalCashOut), style = MaterialTheme.typography.titleLarge, color = Color(0xFFC62828))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.cashTransactions.sortedByDescending { it.date }) { transaction ->
                    ListItem(
                        headlineContent = { Text(transaction.note) },
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

    if (showAddDialog) {
        CashTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, note, isCashIn, date ->
                viewModel.addCashTransaction(amount, note, isCashIn, date)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Boolean, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCashIn by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                onConfirm(amt, note, isCashIn, selectedDate)
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
