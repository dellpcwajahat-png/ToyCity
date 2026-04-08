package com.example.toycity.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    val firstOfCurrentYear = Calendar.getInstance().apply {
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

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

    val metrics = remember(allRecords, today, yesterday, sevenDaysAgo, firstOfCurrentMonth, firstOfCurrentYear) {
        val (todayS, todayP, todayE) = getMetricsForPeriod(today)
        val (yesterdayS, yesterdayP, _) = getMetricsForPeriod(yesterday, today)
        val (last7DaysS, last7DaysP, _) = getMetricsForPeriod(sevenDaysAgo)
        val (monthS, monthP, monthE) = getMetricsForPeriod(firstOfCurrentMonth)
        val (yearS, _, _) = getMetricsForPeriod(firstOfCurrentYear)
        
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
            val yearSales = yearS
            val graphData = graphData
        }
    }

    // Inventory and Financial Summary Stats
    val customerBalance = uiState.customerReceivables
    val totalDebt = uiState.totalDebt
    
    val totalProducts = uiState.inventoryData.items.size
    val inStockProducts = uiState.inventoryData.items.count { it.quantity > 0 }
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

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Cash in Drawer, Receivables, Debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "$currentYear Avg Sale",
                    value = Formatter.formatCurrency(metrics.yearSales / dayOfYear),
                    color = MaterialTheme.colorScheme.primary
                )
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
                    label = "In Stock",
                    value = inStockProducts.toString(),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Sale",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Formatter.formatCurrency(sales),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                
                // Moved profit here to be more visible and save vertical space
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.End) {
                        Text("Today's Profit", style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp), color = Color(0xFF2E7D32))
                        Text(
                            Formatter.formatCurrency(profit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Simplified Expenses Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFC62828)))
                        Column {
                            Text("Expenses", style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatter.formatCurrency(expenses), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                
                // Cash Drawer (replaces Net Cash)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00796B)))
                        Column {
                            Text("Cash Drawer", style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatter.formatCurrency(cashInDrawer), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Column {
                Text(
                    text = "Sale",
                    style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = Formatter.formatCurrency(sales).replace("PKR", "Rs"),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Column {
                Text(
                    text = "Profit",
                    style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = Formatter.formatCurrency(profit).replace("PKR", "Rs"),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (profit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
