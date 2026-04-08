package com.example.toycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.ui.components.ScreenHeader
import com.example.toycity.utils.Formatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInHandScreen(
    viewModel: FinancialViewModel = viewModel(),
    currentMonth: String,
    onMonthSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<com.example.toycity.data.CashTransaction?>(null) }

    // Use case-insensitive matching and check both 'Cash in Hand' and 'Initial Cash' if used
    val cashInHandTransactions = remember(uiState.cashTransactions) {
        uiState.cashTransactions.filter { 
            it.isCashIn && (it.category.equals("Cash in Hand", ignoreCase = true) || it.category.equals("Initial Cash", ignoreCase = true))
        }.sortedByDescending { it.date }
    }

    val totalCashInHand = cashInHandTransactions.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                entryToEdit = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Cash in Hand")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Moved ScreenHeader inside the main column and ensured no extra padding at top
            ScreenHeader(title = "Cash in Hand")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Cash in Hand Entries", style = MaterialTheme.typography.labelMedium)
                    Text(
                        Formatter.formatCurrency(totalCashInHand),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (cashInHandTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Wallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text("No cash in hand records", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cashInHandTransactions) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                entryToEdit = entry
                                showAddDialog = true
                            }
                        ) {
                            ListItem(
                                headlineContent = { Text(entry.note, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(Formatter.formatDate(Date(entry.date))) },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            Formatter.formatCurrency(entry.amount),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        IconButton(onClick = { viewModel.removeCashTransaction(entry.id) }) {
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
    }

    if (showAddDialog) {
        AddCashInHandDialog(
            existingEntry = entryToEdit,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, note, timestamp ->
                if (entryToEdit != null) {
                    viewModel.updateCashTransaction(entryToEdit!!, amount, note, timestamp)
                } else {
                    viewModel.addCashTransaction(amount, note, true, timestamp, "Cash in Hand")
                }
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashInHandDialog(
    existingEntry: com.example.toycity.data.CashTransaction? = null,
    onDismiss: () -> Unit, 
    onConfirm: (Double, String, Long) -> Unit
) {
    var amount by remember { mutableStateOf(existingEntry?.amount?.let { if(it == 0.0) "" else it.toString() } ?: "") }
    var note by remember { mutableStateOf(existingEntry?.note ?: "") }
    var selectedDate by remember { mutableLongStateOf(existingEntry?.date ?: System.currentTimeMillis()) }
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
        title = { Text(if (existingEntry == null) "Add Initial Cash in Hand" else "Edit Cash in Hand") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text("Detail / Note") },
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
                            Text("Date", style = MaterialTheme.typography.labelSmall)
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
                    if (amt > 0 && note.isNotEmpty()) onConfirm(amt, note, selectedDate)
                },
                enabled = amount.isNotEmpty() && note.isNotEmpty()
            ) { Text(if (existingEntry == null) "Save" else "Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
