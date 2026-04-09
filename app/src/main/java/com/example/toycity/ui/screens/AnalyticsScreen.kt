package com.example.toycity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.example.toycity.utils.Formatter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.FinancialRecord
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.ui.components.ScreenHeader
import java.util.Locale

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: FinancialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val isAllTimeView by viewModel.isAllTimeView.collectAsState()

    // Calculate All-Time Totals (Only include months where data has been entered/sales recorded)
    val activeRecords = allRecords.filter { it.totalSales > 0 }
    
    val allTimeSales = allRecords.sumOf { it.totalSales }
    val allTimeExpenses = allRecords.sumOf { it.totalExpenses + it.inventoryData.cogs }
    val allTimeCOGS = allRecords.sumOf { it.inventoryData.cogs }
    val allTimeRestock = allRecords.sumOf { it.inventoryData.restockInvestment }
    val allTimeNetProfit = allRecords.sumOf { it.netProfit }
    
    val totalDays = activeRecords.sumOf { it.daysInMonth }
    val allTimeDailyAvg = if (totalDays > 0) allTimeSales / totalDays else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(title = "Business Dashboard")
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isAllTimeView,
                    onClick = { viewModel.setAllTimeView(!isAllTimeView) },
                    label = { Text(if (isAllTimeView) "All-Time" else "Monthly") },
                    leadingIcon = if (isAllTimeView) {
                        { Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }

        // Summary Chart Section
        item {
            val displaySales = if (isAllTimeView) allTimeSales else uiState.totalSales
            val displayExpenses = if (isAllTimeView) allTimeExpenses else (uiState.totalExpenses + uiState.inventoryData.cogs)
            
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        if (isAllTimeView) "Lifetime Performance" else "Monthly Performance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val maxVal = maxOf(displaySales, displayExpenses, 1.0)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProgressBarRow("Total Sales", displaySales, maxVal, Color(0xFF4CAF50))
                        ProgressBarRow("Expenses", displayExpenses, maxVal, Color(0xFFF44336))
                    }
                }
            }
        }

        // KPI Grid
        item {
            val netProfit = if (isAllTimeView) allTimeNetProfit else uiState.netProfit
            val dailyAvg = if (isAllTimeView) allTimeDailyAvg else uiState.dailyAverageSale
            
            // Find the latest record based on chronological order
            val latestRecord = allRecords.maxByOrNull { 
                Formatter.parseMonth(it.id) ?: java.util.Date(0) 
            }
            
            val cashInDrawer = if (isAllTimeView) {
                latestRecord?.cashInDrawer ?: uiState.cashInDrawer
            } else {
                uiState.cashInDrawer
            }
            
            val customerBalance = if (isAllTimeView) {
                // Find latest month that actually has some activity
                allRecords.filter { it.totalSales > 0 || it.customerReceivables > 0 }
                    .maxByOrNull { Formatter.parseMonth(it.id) ?: java.util.Date(0) }
                    ?.customerReceivables ?: 0.0
            } else {
                uiState.customerReceivables
            }
            
            val totalCOGS = if (isAllTimeView) allTimeCOGS else uiState.inventoryData.cogs
            val totalRestock = if (isAllTimeView) allTimeRestock else uiState.inventoryData.restockInvestment

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCardItem(
                        Modifier.weight(1f),
                        "Net Profit",
                        Formatter.formatCurrency(netProfit),
                        MaterialTheme.colorScheme.primaryContainer,
                        Icons.Default.Payments
                    )
                    MetricCardItem(
                        Modifier.weight(1f),
                        "Daily Avg",
                        Formatter.formatCurrency(dailyAvg),
                        MaterialTheme.colorScheme.tertiaryContainer,
                        Icons.AutoMirrored.Filled.TrendingUp
                    )
                }
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCardItem(
                        Modifier.weight(1f),
                        "Cash In Drawer",
                        Formatter.formatCurrency(cashInDrawer),
                        MaterialTheme.colorScheme.surfaceVariant,
                        Icons.Default.AccountBalanceWallet
                    )
                    MetricCardItem(
                        Modifier.weight(1f),
                        "Cost of Goods",
                        Formatter.formatCurrency(totalCOGS),
                        MaterialTheme.colorScheme.secondaryContainer,
                        Icons.Default.Inventory2
                    )
                }
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCardItem(
                        Modifier.weight(1f),
                        "Total Restock",
                        Formatter.formatCurrency(totalRestock),
                        MaterialTheme.colorScheme.inverseOnSurface,
                        Icons.Default.AddShoppingCart
                    )
                    MetricCardItem(
                        Modifier.weight(1f),
                        "Receivables",
                        Formatter.formatCurrency(customerBalance),
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        Icons.Default.People
                    )
                }
            }
        }

        // Loans Section
        val activeLoans = if (isAllTimeView) {
            // Aggregate loans by lender across all months
            allRecords.flatMap { it.loans }
                .groupBy { it.lenderName }
                .map { (name, loans) ->
                    com.example.toycity.data.Loan(
                        lenderName = name,
                        principalAmount = loans.sumOf { it.principalAmount },
                        repaymentToDate = loans.sumOf { it.repaymentToDate }
                    )
                }.filter { it.principalAmount > 0 }
        } else {
            uiState.loans.filter { it.principalAmount > 0 }
        }

        if (activeLoans.isNotEmpty()) {
            item {
                Text(
                    text = if (isAllTimeView) "Lifetime Debt Summary" else "Monthly Debt Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(activeLoans) { loan ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LoanSummaryItem(loan)
                }
            }
        }

        // Trends Section
        if (allRecords.size > 1) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Sales Trends (Last 6 Months)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    TrendChart(allRecords)
                }
            }
        }

        // Low Stock Alerts
        val lowStockItems = uiState.inventoryData.items.filter { it.quantity <= it.lowStockThreshold }
        if (lowStockItems.isNotEmpty()) {
            item {
                Text(
                    "Low Stock Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(lowStockItems) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InventoryAlertItem(item)
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun LoanSummaryItem(loan: com.example.toycity.data.Loan) {
    val progress = if (loan.principalAmount > 0) (loan.repaymentToDate / loan.principalAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = loan.principalAmount - loan.repaymentToDate
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(loan.lenderName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${Formatter.formatCurrency(remaining)} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Repaid: ${Formatter.formatCurrency(loan.repaymentToDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TrendChart(records: List<FinancialRecord>) {
    val displayRecords = records.takeLast(6)
    val maxSales = displayRecords.maxOfOrNull { it.totalSales }?.takeIf { it > 0 } ?: 1.0
    
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                displayRecords.forEach { record ->
                    val barHeight = (record.totalSales / maxSales).toFloat().coerceIn(0.05f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = record.id.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryAlertItem(item: com.example.toycity.data.InventoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Current Stock: ${item.quantity} units", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "LOW STOCK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun MetricCardItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    containerColor: Color,
    icon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ProgressBarRow(label: String, value: Double, max: Double, color: Color) {
    val progress = (value / max).toFloat().coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            Text(Formatter.formatCurrency(value), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
