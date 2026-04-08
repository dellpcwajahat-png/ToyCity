package com.example.toycity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.Loan
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.ui.components.ScreenHeader
import com.example.toycity.utils.Formatter
import java.util.*

@Composable
fun DebtManagementScreen(
    viewModel: FinancialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showAddLenderDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf<Loan?>(null) }
    var showHistoryDialog by remember { mutableStateOf<Loan?>(null) }
    var lenderToDelete by remember { mutableStateOf<Loan?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Debt Management")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Total Debt Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Outstanding Debt",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        Formatter.formatCurrency(uiState.totalDebt),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lenders / Suppliers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddLenderDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add New")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.loans.isEmpty()) {
                EmptyDebtState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(uiState.loans) { loan ->
                        LenderCard(
                            loan = loan,
                            onTransaction = { showTransactionDialog = loan },
                            onHistory = { showHistoryDialog = loan },
                            onDelete = { lenderToDelete = loan }
                        )
                    }
                }
            }
        }
    }

    if (showAddLenderDialog) {
        AddLenderDialog(
            onDismiss = { showAddLenderDialog = false },
            onConfirm = { name ->
                viewModel.addLender(name)
                showAddLenderDialog = false
            }
        )
    }

    showTransactionDialog?.let { loan ->
        LoanTransactionDialog(
            loan = loan,
            onDismiss = { showTransactionDialog = null },
            onConfirm = { amount, isRepayment, date, note ->
                viewModel.addLoanTransaction(loan.lenderName, amount, isRepayment, date, note)
                showTransactionDialog = null
            }
        )
    }

    showHistoryDialog?.let { loan ->
        LoanHistoryDialog(
            loan = loan,
            transactions = uiState.cashTransactions.filter { 
                it.category == "Loan Received" || it.category == "Loan Repayment" 
            }.filter { it.note.contains(loan.lenderName, ignoreCase = true) },
            onDismiss = { showHistoryDialog = null },
            onDeleteTransaction = { viewModel.removeCashTransaction(it) }
        )
    }

    lenderToDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { lenderToDelete = null },
            title = { Text("Delete Lender") },
            text = { Text("Are you sure you want to remove '${loan.lenderName}'? This will not delete the associated cash transactions, but the lender will disappear from this list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLender(loan.lenderName)
                        lenderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { lenderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LenderCard(
    loan: Loan,
    onTransaction: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit
) {
    val remaining = loan.principalAmount - loan.repaymentToDate
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(loan.lenderName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Remaining: ${Formatter.formatCurrency(remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remaining > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(Formatter.formatCurrency(loan.principalAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Repaid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(Formatter.formatCurrency(loan.repaymentToDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("History", fontSize = 12.sp)
                }
                Button(
                    onClick = onTransaction,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Transaction", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddLenderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Lender") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Lender/Supplier Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoanTransactionDialog(
    loan: Loan,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, Long, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var isRepayment by remember { mutableStateOf(true) } // Default to repayment
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Transaction: ${loan.lenderName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = isRepayment,
                        onClick = { isRepayment = true },
                        label = { Text("Repayment (Out)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = !isRepayment,
                        onClick = { isRepayment = false },
                        label = { Text("Received (In)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                selectedDate = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Date: ${Formatter.formatDate(Date(selectedDate))}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, isRepayment, selectedDate, note) 
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoanHistoryDialog(
    loan: Loan,
    transactions: List<com.example.toycity.data.CashTransaction>,
    onDismiss: () -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("History: ${loan.lenderName}") },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        },
        text = {
            if (transactions.isEmpty()) {
                Text("No transaction history found for this lender.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions.sortedByDescending { it.date }) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (tx.category == "Loan Received") "Received" else "Repayment",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tx.category == "Loan Received") MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                                )
                                Text(Formatter.formatCurrency(tx.amount), fontWeight = FontWeight.Bold)
                                Text(Formatter.formatDate(Date(tx.date)), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteTransaction(tx.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EmptyDebtState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No active debts",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            "Add a lender or supplier to track your loans and repayments.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
