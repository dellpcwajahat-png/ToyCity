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
    val customCategories: List<String> = listOf("Operational", "Rent", "Utilities", "Salaries", "Marketing"),
    val inventoryData: InventoryData = InventoryData(),
    val loans: List<Loan> = emptyList(),
    val cashTransactions: List<CashTransaction> = emptyList(), // Daily Cash In/Out
    val sales: List<Sale> = emptyList(), // List of all sales/receipts
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Days in Month helper
    val daysInMonth: Int
        get() {
            if (id.isEmpty()) return 30
            return try {
                val parts = id.split("-")
                val month = parts[0].toInt() - 1 // MM is first part
                val year = parts[1].toInt()      // YYYY is second part
                val calendar = java.util.Calendar.getInstance()
                calendar.set(year, month, 1)
                calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            } catch (e: Exception) {
                30
            }
        }

    // Daily Average Sale = Computed Sales / Days in Month
    val dailyAverageSale: Double
        get() = if (daysInMonth > 0) salesTotal / daysInMonth else 0.0

    // 1. Total Sales (Computed from list, fallback to field)
    val salesTotal: Double
        get() = if (sales.isNotEmpty()) sales.sumOf { it.totalAmount } else totalSales

    // 2. Total Expenses (Categories + Manual Cash Out + Operating Expenses)
    // We trust the cashTransactions list if it contains operational records to avoid double-counting.
    val totalExpenses: Double
        get() {
            val categorizedSum = expenseCategories.values.sum()
            // All cash out that isn't Restock or Loan Repayment
            val manualCashOut = cashTransactions.filter { 
                !it.isCashIn && 
                !it.category.equals("Restock", ignoreCase = true) && 
                !it.category.equals("Loan Repayment", ignoreCase = true)
            }.sumOf { it.amount }
            
            return if (cashTransactions.any { !it.isCashIn && it.category.equals("Operational", ignoreCase = true) }) {
                manualCashOut + categorizedSum
            } else {
                operatingExpenses + categorizedSum + manualCashOut
            }
        }

    // 3. Net Profit = Sales - Expenses - COGS
    val netProfit: Double
        get() = salesTotal - totalExpenses - inventoryData.cogs

    // Total Debt = Sum of (Principal - Repayment)
    val totalDebt: Double
        get() = loans.sumOf { it.principalAmount - it.repaymentToDate }

    // Profit Margin (%)
    val profitMargin: Double
        get() = if (salesTotal > 0) (netProfit / salesTotal) * 100 else 0.0

    // 4. Cash in Drawer (Cash in Hand)
    // Formula: Initial Cash + Sales + Cash In - Expenses - Restock - Loan Repayments
    val cashInDrawer: Double
        get() {
            val totalCashInTransactions = cashTransactions.filter { it.isCashIn }.sumOf { it.amount }
            val loanRepayments = cashTransactions.filter { 
                !it.isCashIn && it.category.equals("Loan Repayment", ignoreCase = true) 
            }.sumOf { it.amount }
            
            val result = startingCash + salesTotal + totalCashInTransactions - totalExpenses - 
                         inventoryData.restockInvestment - loanRepayments
            return if (result < 0.0001 && result > -0.0001) 0.0 else result
        }
}

data class CashTransaction(
    val id: String = "",
    val date: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val note: String = "",
    val isCashIn: Boolean = true,
    val category: String = "Operational", // "Operational", "Restock", or "Loan Repayment"
    val productId: String? = null // For linking restock to a specific item
)

data class Sale(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<SaleItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val customerName: String = "Walk-in Customer"
)

data class SaleItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)

data class InventoryData(
    val restockInvestment: Double = 0.0,
    val cogs: Double = 0.0,
    val items: List<InventoryItem> = emptyList()
)

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val quantity: Int = 0,
    val totalQuantity: Int = 0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val imageUrl: String = "",
    val lowStockThreshold: Int = 5
)

data class Loan(
    val lenderName: String = "",
    val principalAmount: Double = 0.0,
    val repaymentToDate: Double = 0.0,
    val dueDate: Long? = null // For repayment reminders
)

data class UserLog(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
