package com.example.toycity.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.FinancialRecord
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.utils.Formatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val sevenDaysAgo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val firstOfCurrentMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Extract metrics from actual Sales data across allRecords
    fun getMetricsForPeriod(startTime: Long, endTime: Long? = null): Triple<Double, Double, Double> {
        var salesTotal = 0.0
        var profitTotal = 0.0
        var expensesTotal = 0.0
        
        allRecords.forEach { record ->
            // Use actual sales for revenue and profit
            record.sales.forEach { sale ->
                if (sale.timestamp >= startTime && (endTime == null || sale.timestamp < endTime)) {
                    salesTotal += sale.totalAmount
                    
                    // Calculate profit for this sale by looking up cost prices in inventory
                    var saleCogs = 0.0
                    sale.items.forEach { saleItem ->
                        val inventoryItem = record.inventoryData.items.find { it.id == saleItem.productId }
                        if (inventoryItem != null) {
                            saleCogs += inventoryItem.costPrice * saleItem.quantity
                        } else {
                            // Fallback to record's average COGS % if item not found
                            val cogsRatio = if (record.totalSales > 0) record.inventoryData.cogs / record.totalSales else 0.3
                            saleCogs += saleItem.price * saleItem.quantity * cogsRatio
                        }
                    }
                    profitTotal += (sale.totalAmount - saleCogs)
                }
            }
            
            // Use cash transactions for actual expenses
            record.cashTransactions.forEach { trans ->
                if (trans.date >= startTime && (endTime == null || trans.date < endTime)) {
                    if (!trans.isCashIn) {
                        expensesTotal += trans.amount
                    }
                }
            }
        }
        return Triple(salesTotal, profitTotal, expensesTotal)
    }

    val metrics = remember(allRecords, today, yesterday, sevenDaysAgo, firstOfCurrentMonth) {
        val (todayS, todayP, todayE) = getMetricsForPeriod(today)
        val (yesterdayS, yesterdayP, _) = getMetricsForPeriod(yesterday, today)
        val (last7DaysS, last7DaysP, _) = getMetricsForPeriod(sevenDaysAgo)
        val (monthS, monthP, monthE) = getMetricsForPeriod(firstOfCurrentMonth)
        
        // Prepare data for the 7-day graph
        val graphData = (0..6).map { dayOffset ->
            val start = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -dayOffset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + 24 * 60 * 60 * 1000L
            getMetricsForPeriod(start, end).first.toFloat()
        }.reversed()
        
        object {
            val todaySales = todayS
            val todayProfit = todayP
            val todayExpenses = todayE
            val yesterdaySales = yesterdayS
            val yesterdayProfit = yesterdayP
            val last7DaysSales = last7DaysS
            val last7DaysProfit = last7DaysP
            val monthSales = monthS
            val monthProfit = monthP
            val monthExpenses = monthE
            val graphData = graphData
        }
    }

    // Inventory and Financial Summary Stats
    val customerBalance = uiState.customerReceivables
    val totalDebt = uiState.loans.sumOf { it.principalAmount - it.repaymentToDate }
    
    val totalProducts = uiState.inventoryData.items.size
    val outOfStockProducts = uiState.inventoryData.items.count { it.quantity <= 0 }
    val stockValueNoProfit = uiState.inventoryData.items.sumOf { it.costPrice * it.quantity }
    val stockValueWithProfit = uiState.inventoryData.items.sumOf { it.sellingPrice * it.quantity }

    val cashInDrawer = uiState.cashInDrawer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Secondary Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardMetricItem(
                modifier = Modifier.weight(1f),
                title = "Yesterday",
                sales = metrics.yesterdaySales,
                profit = metrics.yesterdayProfit
            )
            DashboardMetricItem(
                modifier = Modifier.weight(1f),
                title = "Last 7 Days",
                sales = metrics.last7DaysSales,
                profit = metrics.last7DaysProfit
            )
            DashboardMetricItem(
                modifier = Modifier.weight(1f),
                title = "This Month",
                sales = metrics.monthSales,
                profit = metrics.monthProfit
            )
        }

        // Today's Main Card
        MainPerformanceCard(
            sales = metrics.todaySales,
            profit = metrics.todayProfit,
            expenses = metrics.todayExpenses,
            cashInDrawer = cashInDrawer
        )

        // Sales Trend Graph
        SalesGraphCard(data = metrics.graphData)

    // Financial & Inventory Summary Boxes
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Receivables, Debt, Today's Expenses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryBox(
                modifier = Modifier.weight(1f),
                label = "Cust. Balance",
                value = Formatter.formatCurrency(customerBalance),
                color = MaterialTheme.colorScheme.primary
            )
            SummaryBox(
                modifier = Modifier.weight(1f),
                label = "Total Debt",
                value = Formatter.formatCurrency(totalDebt),
                color = Color(0xFFC62828)
            )
            SummaryBox(
                modifier = Modifier.weight(1f),
                label = "Today Exp.",
                value = Formatter.formatCurrency(metrics.todayExpenses),
                color = Color(0xFFE65100)
            )
        }

            // Row 2: Stock Value (Cost), Stock Value (Sell), Total Products
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Stock (Cost)",
                    value = Formatter.formatCurrency(stockValueNoProfit),
                    color = MaterialTheme.colorScheme.secondary
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Stock (Sell)",
                    value = Formatter.formatCurrency(stockValueWithProfit),
                    color = Color(0xFF2E7D32)
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Tot. Products",
                    value = totalProducts.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Row 3: Out of Stock, Daily Avg Sale, Total Restock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Out of Stock",
                    value = outOfStockProducts.toString(),
                    color = if (outOfStockProducts > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Daily Avg Sale",
                    value = Formatter.formatCurrency(uiState.dailyAverageSale),
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Tot. Restock",
                    value = Formatter.formatCurrency(uiState.inventoryData.restockInvestment),
                    color = Color(0xFF6750A4)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // Extra space for bottom bar
    }
}

@Composable
fun SummaryBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MainPerformanceCard(
    sales: Double,
    profit: Double,
    expenses: Double,
    cashInDrawer: Double
) {
    val actualCash = remember(cashInDrawer) { cashInDrawer }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Daily Performance Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total sale",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatter.formatCurrency(sales),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profit Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Net Profit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = Formatter.formatCurrency(profit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            maxLines = 1
                        )
                    }
                }
                
                // Expenses Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = Formatter.formatCurrency(expenses),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            maxLines = 1
                        )
                    }
                }

                // Cash in Drawal Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cash in Drawer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = Formatter.formatCurrency(actualCash),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SalesGraphCard(data: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Last 7 Days Sales Trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            val maxValue = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
            
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val spacing = width / (data.size - 1)
                
                val points = data.mapIndexed { index, value ->
                    Offset(
                        x = index * spacing,
                        y = height - (value / maxValue * height)
                    )
                }

                val path = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val p0 = points[i - 1]
                            val p1 = points[i]
                            cubicTo(
                                (p0.x + p1.x) / 2, p0.y,
                                (p0.x + p1.x) / 2, p1.y,
                                p1.x, p1.y
                            )
                        }
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6750A4).copy(alpha = 0.3f),
                            Color(0xFF6750A4).copy(alpha = 0.0f)
                        )
                    )
                )

                drawPath(
                    path = path,
                    color = Color(0xFF6750A4),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw dots
                points.forEach { point ->
                    drawCircle(
                        color = Color(0xFF6750A4),
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("6d", "5d", "4d", "3d", "2d", "1d", "Now")
                days.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardMetricItem(
    modifier: Modifier = Modifier,
    title: String,
    sales: Double,
    profit: Double
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Column {
                Text(
                    text = "Sale",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatter.formatCurrency(sales),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = "Profit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatter.formatCurrency(profit),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (profit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}
