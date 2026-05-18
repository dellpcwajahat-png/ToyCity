package com.example.toycity.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.utils.Formatter
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
    val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH) + 1

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
        val (monthS, monthP, monthE) = getMetricsForPeriod(firstOfCurrentMonth)
        val (yearS, yearP, yearE) = getMetricsForPeriod(firstOfCurrentYear)
        
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
            val todayProfit = todayP // This is Gross Profit
            val todayExpenses = todayE
            val todayNetProfit = todayP - todayE
            val yesterdaySales = yesterdayS
            val yesterdayProfit = yesterdayP
            val monthSales = monthS
            val monthProfit = monthP
            val monthExpenses = monthE
            val yearSales = yearS
            val yearProfit = yearP
            val yearExpenses = yearE
            val graphData = graphData
        }
    }

    // Inventory and Financial Summary Stats
    val customerBalance = uiState.customerReceivables
    val totalDebt = uiState.totalDebt
    
    val totalProducts = uiState.inventoryData.items.size
    val outOfStockProducts = uiState.inventoryData.items.count { it.quantity <= 0 }
    val lowStockProducts = uiState.inventoryData.items.count { it.quantity > 0 && it.quantity <= it.lowStockThreshold }
    val stockValueNoProfit = uiState.inventoryData.items.sumOf { it.costPrice * it.quantity }
    val stockValueWithProfit = uiState.inventoryData.items.sumOf { it.sellingPrice * it.quantity }

    val cashInDrawer = uiState.cashInDrawer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Main Card - Hero Section
        MainPerformanceCard(
            sales = metrics.todaySales,
            profit = metrics.todayProfit,
            expenses = metrics.todayExpenses,
            cashInDrawer = cashInDrawer
        )


        // Secondary Metrics Row - Compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricItem(
                modifier = Modifier.weight(1f),
                title = "Yesterday",
                sales = metrics.yesterdaySales,
                profit = metrics.yesterdayProfit,
                icon = Icons.Default.History
            )
            DashboardMetricItem(
                modifier = Modifier.weight(1f),
                title = "This Month",
                sales = metrics.monthSales,
                profit = metrics.monthProfit,
                icon = Icons.Default.CalendarMonth
            )
        }

        // Sales Trend Graph
        SalesGraphCard(data = metrics.graphData)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Business Vitals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Cash in Drawer, Receivables, Debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Cust. Balance",
                    value = Formatter.formatCurrency(customerBalance),
                    icon = Icons.Default.Groups,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Total Debt",
                    value = Formatter.formatCurrency(totalDebt),
                    icon = Icons.Default.CreditCard,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // Row 2: Stock Value (Cost), Stock Value (Sell)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Stock (Cost)",
                    value = Formatter.formatCurrency(stockValueNoProfit),
                    icon = Icons.Default.Inventory2,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Stock (Sell)",
                    value = Formatter.formatCurrency(stockValueWithProfit),
                    icon = Icons.Default.Sell,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Row 3: Product Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Out of Stock",
                    value = outOfStockProducts.toString(),
                    icon = Icons.Default.Warning,
                    containerColor = if (outOfStockProducts > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (outOfStockProducts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Tot. Products",
                    value = totalProducts.toString(),
                    icon = Icons.AutoMirrored.Filled.List,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Restock Inv.",
                    value = Formatter.formatCurrency(uiState.inventoryData.restockInvestment),
                    icon = Icons.Default.AddShoppingCart,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Row 4: Yearly Average Sale & Expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Yearly Avg Sale",
                    value = Formatter.formatCurrency(metrics.yearSales / currentMonthIndex),
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "Yearly Avg Exp.",
                    value = Formatter.formatCurrency(metrics.yearExpenses / currentMonthIndex),
                    icon = Icons.Default.BarChart,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}


@Composable
fun SummaryBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
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
    val netProfit = profit - expenses
    val isPositive = netProfit >= 0

    val heroGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(heroGradient)
    ) {
        // Subtle decorative circle in background
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 160.dp, y = (-60).dp)
                .background(
                    Color.White.copy(alpha = 0.06f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = (-40).dp, y = 80.dp)
                .background(
                    Color.White.copy(alpha = 0.04f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "TODAY'S REVENUE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatter.formatCurrency(sales),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(28.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metric chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Net Profit",
                    value = Formatter.formatCurrency(netProfit),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    chipColor = if (isPositive) Color(0xFF34D399).copy(alpha = 0.22f) else Color(0xFFF87171).copy(alpha = 0.22f),
                    valueColor = if (isPositive) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)
                )
                MetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Expenses",
                    value = Formatter.formatCurrency(expenses),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    chipColor = Color(0xFFF87171).copy(alpha = 0.18f),
                    valueColor = Color(0xFFFCA5A5)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cash Drawer strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Cash in Drawer",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        Formatter.formatCurrency(cashInDrawer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chipColor: Color,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = chipColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PerformanceMetricItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SalesGraphCard(data: List<Float>) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Day Sales Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Revenue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            val maxValue = (data.maxOrNull() ?: 0f).let { if (it < 1000) 1000f else it * 1.2f }
            
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val labelWidth = 45.dp.toPx()
                    val chartWidth = width - labelWidth
                    val chartHeight = height - 25.dp.toPx()
                    
                    // Draw Horizontal Grid Lines and Y-Axis Labels
                    val gridLines = 4
                    val textStyle = TextStyle(fontSize = 10.sp, color = onSurfaceVariant)
                    
                    for (i in 0..gridLines) {
                        val y = chartHeight - (i * chartHeight / gridLines)
                        val value = (i * maxValue / gridLines).toInt()
                        
                        // Grid line
                        drawLine(
                            color = outlineVariant.copy(alpha = 0.5f),
                            start = Offset(labelWidth, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Y-Axis Label
                        drawText(
                            textMeasurer = textMeasurer,
                            text = if (value >= 1000) "${value / 1000}k" else value.toString(),
                            style = textStyle,
                            topLeft = Offset(0f, y - 7.dp.toPx())
                        )
                    }

                    if (data.isNotEmpty()) {
                        val spacing = chartWidth / (data.size - 1)
                        val points = data.mapIndexed { index, value ->
                            Offset(
                                x = labelWidth + (index * spacing),
                                y = chartHeight - (value / maxValue * chartHeight)
                            )
                        }

                        val path = Path().apply {
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

                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, chartHeight)
                            lineTo(points.first().x, chartHeight)
                            close()
                        }

                        // Draw Gradient Fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primary.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )

                        // Draw Main Line
                        drawPath(
                            path = path,
                            color = primary,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Draw Points
                        points.forEach { point ->
                            drawCircle(color = primary, radius = 4.dp.toPx(), center = point)
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 45.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("6d", "5d", "4d", "3d", "2d", "1d", "Today")
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
    profit: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.padding(6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = Formatter.formatCurrency(sales),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (profit >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (profit >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                    Text(
                        text = Formatter.formatCurrency(profit),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (profit >= 0) Color(0xFF059669) else Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
