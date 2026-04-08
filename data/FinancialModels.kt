package com.example.toycity.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FinancialRecord(
    val id: String = "", // Used for Firestore document ID (Month, e.g., "2024-04")
    val userId: String = "",
    val startingCash: Double = 0.0,
    val totalSales: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val expenseCategories: Map<String, Double> = emptyMap(), // Categorized expenses
    val customerReceivables: Double = 0.0,
    val inventoryData: InventoryData = InventoryData(),
    val loans: List<Loan> = listOf(
        Loan(lenderName = "Lender M"),
        Loan(lenderName = "Lender W")
    ),
    val cashTransactions: List<CashTransaction> = emptyList(), // Daily Cash In/Out
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Total Expenses = Sum of operatingExpenses (legacy) + expenseCategories
    val totalExpenses: Double
        get() = operatingExpenses + expenseCategories.values.sum()

    // Net Profit = Total Sales - Total Expenses - COGS
    val netProfit: Double
        get() = totalSales - totalExpenses - inventoryData.cogs

    // Profit Margin (%) = (Net Profit / Total Sales) * 100
    val profitMargin: Double
        get() = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0

    // Cash in Drawer
    val cashInDrawer: Double
        get() = startingCash + totalSales - totalExpenses - 
                inventoryData.restockInvestment - loans.sumOf { it.repaymentToDate }
}

data class InventoryData(
    val restockInvestment: Double = 0.0,
    val cogs: Double = 0.0,
    val items: List<InventoryItem> = emptyList()
)

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val lowStockThreshold: Int = 5
)

data class Loan(
    val lenderName: String = "",
    val principalAmount: Double = 0.0,
    val repaymentToDate: Double = 0.0,
    val dueDate: Long? = null // For repayment reminders
)
