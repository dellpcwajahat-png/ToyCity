package com.example.toycity.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.Sale
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.ui.components.ScreenHeader
import com.example.toycity.utils.Formatter
import com.example.toycity.utils.PdfGenerator
import java.util.*

@Composable
fun FinanceScreen(
    viewModel: FinancialViewModel = viewModel(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var saleToEdit by remember { mutableStateOf<Sale?>(null) }
    var saleToReturnFrom by remember { mutableStateOf<Sale?>(null) }

    // Combine all sales from all records for trends
    val allTimeSales = remember(allRecords) {
        allRecords.flatMap { it.sales }.sortedBy { it.timestamp }
    }

    val filteredSales = uiState.sales.filter {
        it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.id.takeLast(8).contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.timestamp }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScreenHeader(title = "Ledger")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Trends Graph Section
            if (allTimeSales.isNotEmpty()) {
                SalesTrendSection(allTimeSales)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search by customer or ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyLedgerState()
                }
            } else {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSales, key = { it.id }) { sale ->
                        SaleCard(
                            sale = sale,
                            onClick = { selectedSale = sale },
                            onPrint = { PdfGenerator.generateReceipt(context, sale, true) }
                        )
                    }
                }
            }
        }

        // Receipt Detail Dialog
        selectedSale?.let { sale ->
            ReceiptDetailDialog(
                sale = sale,
                onDismiss = { selectedSale = null },
                onExportPDF = { PdfGenerator.generateReceipt(context, sale, false) },
                onPrintThermal = { PdfGenerator.generateReceipt(context, sale, true) },
                onEdit = { 
                    saleToEdit = sale
                    selectedSale = null
                },
                onDelete = {
                    saleToDelete = sale
                    selectedSale = null
                },
                onReturn = {
                    saleToReturnFrom = sale
                    selectedSale = null
                }
            )
        }

        // Delete Confirmation Dialog
        saleToDelete?.let { sale ->
            AlertDialog(
                onDismissRequest = { saleToDelete = null },
                title = { Text("Delete Receipt") },
                text = { Text("Are you sure you want to delete this receipt? This will add products back to inventory and deduct the sale amount.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSale(sale.id)
                            saleToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { saleToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Sale Dialog
        saleToEdit?.let { sale ->
            EditSaleDialog(
                sale = sale,
                onDismiss = { saleToEdit = null },
                onConfirm = { updatedSale ->
                    viewModel.updateSale(updatedSale)
                    saleToEdit = null
                }
            )
        }

        // Return Product Dialog
        saleToReturnFrom?.let { sale ->
            ReturnProductDialog(
                sale = sale,
                onDismiss = { saleToReturnFrom = null },
                onConfirm = { productId, qty ->
                    viewModel.returnProductFromSale(sale.id, productId, qty)
                    saleToReturnFrom = null
                }
            )
        }

        // Monthly Report Dialog
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("Generate Monthly Report") },
                text = { Text("Choose the format for the full monthly financial overview of ${uiState.id}.") },
                confirmButton = {
                    TextButton(onClick = {
                        PdfGenerator.generateFinancialReport(context, uiState, false)
                        showReportDialog = false
                    }) {
                        Text("A4 Document")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        PdfGenerator.generateFinancialReport(context, uiState, true)
                        showReportDialog = false
                    }) {
                        Text("58mm Thermal")
                    }
                }
            )
        }
    }
}

@Composable
fun SalesTrendSection(allSales: List<Sale>) {
    var selectedPeriod by remember { mutableStateOf("7D") }
    val periods = listOf("7D", "This Month", "Prev Month", "This Year", "Prev Year")

    val filteredSales = remember(allSales, selectedPeriod) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        when (selectedPeriod) {
            "7D" -> allSales.filter { it.timestamp > now - 7 * 24 * 60 * 60 * 1000L }
            "This Month" -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                allSales.filter { it.timestamp >= calendar.timeInMillis }
            }
            "Prev Month" -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis
                allSales.filter { it.timestamp in start until end }
            }
            "This Year" -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                allSales.filter { it.timestamp >= calendar.timeInMillis }
            }
            "Prev Year" -> {
                calendar.timeInMillis = now
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.set(Calendar.YEAR, currentYear - 1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfPrev = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                val endOfPrev = calendar.timeInMillis
                allSales.filter { it.timestamp in startOfPrev until endOfPrev }
            }
            else -> allSales
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ScrollableTabRow(
                selectedTabIndex = periods.indexOf(selectedPeriod),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period, fontSize = 10.sp) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Line Chart
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                if (filteredSales.isEmpty()) {
                    Text("No data for this period", style = MaterialTheme.typography.bodySmall)
                } else {
                    val aggregated = remember(filteredSales, selectedPeriod) {
                        filteredSales.groupBy { 
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.timestamp
                            if (selectedPeriod.contains("Year")) {
                                String.format(Locale.US, "%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                            } else {
                                String.format(Locale.US, "%d-%03d", cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
                            }
                        }.mapValues { it.value.sumOf { s -> s.totalAmount } }
                        .toSortedMap()
                        .values.toList()
                    }

                    val maxVal = (aggregated.maxOfOrNull { it }?.toFloat() ?: 1f).coerceAtLeast(1f)
                    
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                        val width = size.width
                        val height = size.height
                        
                        if (aggregated.size > 1) {
                            val spacing = width / (aggregated.size - 1)
                            val path = Path()
                            
                            aggregated.forEachIndexed { index, amount ->
                                val x = index * spacing
                                val y = height - (amount.toFloat() / maxVal * height)
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            
                            // Fill area under the line
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                color = primaryColor.copy(alpha = 0.2f)
                            )

                            // Points
                            aggregated.forEachIndexed { index, amount ->
                                val x = index * spacing
                                val y = height - (amount.toFloat() / maxVal * height)
                                drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                        } else if (aggregated.size == 1) {
                            val x = width / 2
                            val y = height - (aggregated[0].toFloat() / maxVal * height)
                            drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(x, y))
                            drawLine(color = primaryColor.copy(alpha = 0.3f), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 2.dp.toPx())
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Period Sales:", style = MaterialTheme.typography.labelMedium)
                Text(
                    Formatter.formatCurrency(filteredSales.sumOf { it.totalAmount }),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SaleCard(
    sale: Sale,
    onClick: () -> Unit,
    onPrint: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sale.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Formatter.formatDate(Date(sale.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatter.formatCurrency(sale.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onPrint, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = "Quick Print",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onExportPDF: () -> Unit,
    onPrintThermal: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReturn: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Receipt Details", fontWeight = FontWeight.ExtraBold)
                Text(
                    "ID: ${sale.id.takeLast(8).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onReturn) {
                        Icon(Icons.AutoMirrored.Filled.AssignmentReturn, "Return Item", tint = Color(0xFFE67E22))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Customer:", fontWeight = FontWeight.Bold)
                    Text(sale.customerName)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Time:", fontWeight = FontWeight.Bold)
                    Text(Formatter.formatDate(Date(sale.timestamp)))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                sale.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity}x ${item.productName}", modifier = Modifier.weight(1f))
                        Text(Formatter.formatCurrency(item.price * item.quantity))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        Formatter.formatCurrency(sale.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OutlinedButton(
                        onClick = onPrintThermal,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Print Receipt (58mm)", fontSize = 14.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun EditSaleDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onConfirm: (Sale) -> Unit
) {
    var customerName by remember { mutableStateOf(sale.customerName) }
    var timestamp by remember { mutableLongStateOf(sale.timestamp) }
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Receipt") },
        text = {
            Column {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val datePicker = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val timePicker = android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                                        timestamp = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                )
                                timePicker.show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePicker.show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Date: ${Formatter.formatDate(Date(timestamp))}")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Note: Item quantities and prices can be returned individually via the 'Return' option for better inventory tracking.", 
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(sale.copy(customerName = customerName, timestamp = timestamp)) 
            }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReturnProductDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var selectedProductId by remember { mutableStateOf("") }
    var quantityToReturn by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return Product") },
        text = {
            Column {
                Text("Select product to return from this receipt:")
                Spacer(modifier = Modifier.height(8.dp))
                
                sale.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedProductId = item.productId 
                                quantityToReturn = 1
                            }
                            .background(
                                if (selectedProductId == item.productId) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else Color.Transparent
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity}x ${item.productName}")
                        Text(Formatter.formatCurrency(item.price))
                    }
                }

                if (selectedProductId.isNotEmpty()) {
                    val selectedItem = sale.items.find { it.productId == selectedProductId }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Qty to Return: ")
                        IconButton(onClick = { if (quantityToReturn > 1) quantityToReturn-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Text("$quantityToReturn", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { 
                            if (quantityToReturn < (selectedItem?.quantity ?: 0)) quantityToReturn++ 
                        }) {
                            Icon(Icons.Default.Add, null)
                        }
                        Text("/ ${selectedItem?.quantity}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedProductId.isNotEmpty(),
                onClick = { onConfirm(selectedProductId, quantityToReturn) }
            ) {
                Text("Confirm Return")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyLedgerState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No receipts found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            "Complete sales in the Counter tab to see them here.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
