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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.Loan
import com.example.toycity.ui.FinancialViewModel
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
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Total Outstanding Debt",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            Formatter.formatCurrency(uiState.totalDebt),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lenders / Suppliers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddLenderDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Lender")
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
    val progress = if (loan.principalAmount > 0) (loan.repaymentToDate / loan.principalAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(loan.lenderName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (remaining > 0) "Outstanding" else "Paid Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (remaining > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Repayment Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(Formatter.formatCurrency(loan.principalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(Formatter.formatCurrency(remaining), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (remaining > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("History", fontSize = 13.sp)
                }
                Button(
                    onClick = onTransaction,
                    modifier = Modifier.weight(1.2f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.AddCard, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Payment", fontSize = 13.sp)
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
        title = { Text("Add New Lender", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Lender or Supplier Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Badge, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You can track both personal loans and supplier credit here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Lender")
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
        title = { 
            Column {
                Text("New Transaction", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(loan.lenderName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = isRepayment,
                        onClick = { isRepayment = true },
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        label = { Text("Payment Out") }
                    )
                    SegmentedButton(
                        selected = !isRepayment,
                        onClick = { isRepayment = false },
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                        label = { Text("Loan In") }
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
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    prefix = { Text(Formatter.currencySymbol() + " ") }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
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
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Transaction")
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
