package com.example.toycity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.example.toycity.utils.Formatter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val allTimeExpenses = allRecords.sumOf { it.totalExpenses }
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
            val cashInDrawer = allRecords.maxByOrNull { it.id }?.cashInDrawer ?: uiState.cashInDrawer
            
            // Customer Balance logic:
            // Monthly view: Show current month's receivables
            // All-Time view: Show receivables from the latest month that has any data (sales or receivables)
            // This ensures April shows once you enter data for it, otherwise it stays on March.
            val customerBalance = if (isAllTimeView) {
                allRecords.filter { it.totalSales > 0 || it.customerReceivables > 0 }
                    .maxByOrNull { it.id }?.customerReceivables ?: 0.0
            } else {
                uiState.customerReceivables
            }
            
            val totalCOGS = if (isAllTimeView) allTimeCOGS else uiState.inventoryData.cogs
            val totalRestock = if (isAllTimeView) allTimeRestock else uiState.inventoryData.restockInvestment

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCardItem(Modifier.weight(1f), "Net Profit", Formatter.formatCurrency(netProfit), MaterialTheme.colorScheme.primaryContainer)
                    MetricCardItem(Modifier.weight(1f), "Daily Avg Sale", Formatter.formatCurrency(dailyAvg), MaterialTheme.colorScheme.tertiaryContainer)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCardItem(Modifier.weight(1f), "Cash In Drawer", Formatter.formatCurrency(cashInDrawer), MaterialTheme.colorScheme.surfaceVariant)
                    MetricCardItem(Modifier.weight(1f), "Cost of Goods", Formatter.formatCurrency(totalCOGS), MaterialTheme.colorScheme.outlineVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCardItem(Modifier.weight(1f), "Total Restock", Formatter.formatCurrency(totalRestock), MaterialTheme.colorScheme.inverseOnSurface)
                    MetricCardItem(Modifier.weight(1f), "Customer Balance", Formatter.formatCurrency(customerBalance), MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
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
                    fontWeight = FontWeight.Bold
                )
            }
            items(activeLoans) { loan ->
                LoanSummaryItem(loan)
            }
        }

        // Trends Section
        if (allRecords.size > 1) {
            item {
                Text("Sales Trends (Last 6 Months)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TrendChart(allRecords)
            }
        }

        // Low Stock Alerts
        val lowStockItems = uiState.inventoryData.items.filter { it.quantity <= it.lowStockThreshold }
        if (lowStockItems.isNotEmpty()) {
            item {
                Text("Low Stock Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            items(lowStockItems) { item ->
                InventoryAlertItem(item)
            }
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
    val maxSales = records.maxOfOrNull { it.totalSales }?.takeIf { it > 0 } ?: 1.0
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            records.takeLast(6).forEach { record ->
                val barHeight = (record.totalSales / maxSales).toFloat().coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun InventoryAlertItem(item: com.example.toycity.data.InventoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text("Only ${item.quantity} left", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun MetricCardItem(modifier: Modifier = Modifier, label: String, value: String, containerColor: Color) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
