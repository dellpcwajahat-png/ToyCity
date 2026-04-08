package com.example.toycity.ui

import java.util.UUID
import androidx.lifecycle.ViewModel
import com.example.toycity.utils.Formatter
import androidx.lifecycle.viewModelScope
import com.example.toycity.data.FinancialRecord
import com.example.toycity.data.FinancialRepository
import com.example.toycity.data.InventoryItem
import com.example.toycity.data.Loan
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialViewModel : ViewModel() {
    private val repository = FinancialRepository()
    private val _uiState = MutableStateFlow(FinancialRecord())
    val uiState: StateFlow<FinancialRecord> = _uiState.asStateFlow()

    private val _allRecordsRaw = MutableStateFlow<List<FinancialRecord>>(emptyList())
    val allRecords: StateFlow<List<FinancialRecord>> = combine(_uiState, _allRecordsRaw) { current, all ->
        if (current.id.isEmpty()) {
            all.sortedBy { it.id }
        } else {
            val otherMonths = all.filter { it.id != current.id }
            (otherMonths + current).sortedBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAllTimeView = MutableStateFlow(true)
    val isAllTimeView: StateFlow<Boolean> = _isAllTimeView.asStateFlow()

    private var currentUserId: String = ""
    private var currentMonthId: String = ""

    fun setAllTimeView(enabled: Boolean) {
        _isAllTimeView.value = enabled
    }

    private var loadDataJob: kotlinx.coroutines.Job? = null

    fun loadData(userId: String, monthId: String) {
        currentUserId = userId
        currentMonthId = monthId
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            repository.getRecordFlow(userId, monthId).collect { record ->
                if (record == null) {
                    // Try to fetch previous month's data for starting cash
                    val prevMonthId = getPreviousMonthId(monthId)
                    val prevRecord = repository.getRecord(userId, prevMonthId)
                    val startingCash = prevRecord?.cashInDrawer ?: 0.0
                    
                    // Only update if we still don't have a record (avoid race conditions)
                    if (_uiState.value.id != monthId || _uiState.value.userId != userId || record == null) {
                        _uiState.value = FinancialRecord(id = monthId, userId = userId, startingCash = startingCash)
                    }
                } else {
                    _uiState.value = record
                }
            }
        }
        loadAllTrends(userId)
    }

    private fun getPreviousMonthId(monthId: String): String {
        return try {
            val parts = monthId.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.YEAR, year)
            calendar.set(java.util.Calendar.MONTH, month - 1)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            
            calendar.add(java.util.Calendar.MONTH, -1)
            Formatter.formatMonth(calendar.time)
        } catch (e: Exception) {
            ""
        }
    }

    private fun loadAllTrends(userId: String) {
        viewModelScope.launch {
            repository.getAllRecords(userId).collect { records ->
                _allRecordsRaw.value = records
            }
        }
    }

    private val _saveStatus = MutableSharedFlow<Boolean>()
    val saveStatus: SharedFlow<Boolean> = _saveStatus.asSharedFlow()

    fun triggerManualSave() {
        viewModelScope.launch {
            try {
                saveData()
                _saveStatus.emit(true)
            } catch (e: Exception) {
                _saveStatus.emit(false)
            }
        }
    }

    private fun saveData() {
        if (currentUserId.isEmpty() || currentMonthId.isEmpty()) return
        viewModelScope.launch {
            repository.saveRecord(_uiState.value.copy(userId = currentUserId, id = currentMonthId))
        }
    }

    fun updateExpenseCategory(category: String, amount: Double) {
        _uiState.update { state ->
            val updatedCategories = state.expenseCategories.toMutableMap()
            if (amount == 0.0) updatedCategories.remove(category)
            else updatedCategories[category] = amount
            state.copy(expenseCategories = updatedCategories)
        }
    }

    fun addInventoryItem(item: InventoryItem) {
        _uiState.update { state ->
            val updatedItems = state.inventoryData.items + item
            state.copy(inventoryData = state.inventoryData.copy(items = updatedItems))
        }
    }

    fun updateInventoryItem(updatedItem: InventoryItem) {
        _uiState.update { state ->
            val updatedItems = state.inventoryData.items.map { 
                if (it.id == updatedItem.id) updatedItem else it 
            }
            state.copy(inventoryData = state.inventoryData.copy(items = updatedItems))
        }
    }

    fun deleteInventoryItem(itemId: String) {
        _uiState.update { state ->
            val updatedItems = state.inventoryData.items.filter { it.id != itemId }
            state.copy(inventoryData = state.inventoryData.copy(items = updatedItems))
        }
    }

    fun recordSale(items: List<Pair<InventoryItem, Int>>, customerName: String = "Walk-in Customer") {
        _uiState.update { state ->
            var totalSaleAmount = 0.0
            var totalCogsIncrement = 0.0
            val saleItems = mutableListOf<com.example.toycity.data.SaleItem>()
            
            // Map over items in the cart (which may have overridden selling prices)
            val updatedItems = state.inventoryData.items.map { currentItem ->
                val cartEntry = items.find { it.first.id == currentItem.id }
                if (cartEntry != null) {
                    val quantitySold = cartEntry.second
                    val overriddenSellingPrice = cartEntry.first.sellingPrice // Use the price from the passed item
                    
                    totalSaleAmount += overriddenSellingPrice * quantitySold
                    totalCogsIncrement += currentItem.costPrice * quantitySold
                    
                    saleItems.add(
                        com.example.toycity.data.SaleItem(
                            productId = currentItem.id,
                            productName = currentItem.name,
                            quantity = quantitySold,
                            price = overriddenSellingPrice
                        )
                    )
                    
                    currentItem.copy(quantity = (currentItem.quantity - quantitySold).coerceAtLeast(0))
                } else {
                    currentItem
                }
            }.toMutableList()

            val newSale = com.example.toycity.data.Sale(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                items = saleItems,
                totalAmount = totalSaleAmount,
                customerName = customerName
            )

            state.copy(
                totalSales = state.totalSales + totalSaleAmount,
                inventoryData = state.inventoryData.copy(
                    items = updatedItems,
                    cogs = state.inventoryData.cogs + totalCogsIncrement
                ),
                sales = state.sales + newSale
            )
        }
        triggerManualSave()
    }

    fun updateLoanDueDate(index: Int, timestamp: Long?) {
        _uiState.update { state ->
            val updatedLoans = state.loans.toMutableList()
            updatedLoans[index] = updatedLoans[index].copy(dueDate = timestamp)
            state.copy(loans = updatedLoans)
        }
    }

    fun updateStartingCash(value: Double) {
        _uiState.update { it.copy(startingCash = value) }
    }

    fun updateTotalSales(value: Double) {
        _uiState.update { it.copy(totalSales = value) }
    }

    fun updateOperatingExpenses(value: Double) {
        _uiState.update { it.copy(operatingExpenses = value) }
    }

    fun updateCustomerReceivables(value: Double) {
        _uiState.update { it.copy(customerReceivables = value) }
    }

    fun updateRestockInvestment(value: Double) {
        _uiState.update { it.copy(inventoryData = it.inventoryData.copy(restockInvestment = value)) }
    }

    fun updateCOGS(value: Double) {
        _uiState.update { it.copy(inventoryData = it.inventoryData.copy(cogs = value)) }
    }

    fun updateLoanPrincipal(index: Int, value: Double) {
        _uiState.update { state ->
            val updatedLoans = state.loans.toMutableList()
            updatedLoans[index] = updatedLoans[index].copy(principalAmount = value)
            state.copy(loans = updatedLoans)
        }
    }

    fun updateLoanRepayment(index: Int, value: Double) {
        _uiState.update { state ->
            val updatedLoans = state.loans.toMutableList()
            updatedLoans[index] = updatedLoans[index].copy(repaymentToDate = value)
            state.copy(loans = updatedLoans)
        }
    }

    fun addCashTransaction(amount: Double, note: String, isCashIn: Boolean, date: Long, category: String = "Operational") {
        val transactionMonthId = Formatter.formatMonth(java.util.Date(date))
        
        // Use currentUserId since we are adding a transaction for the current user
        val userId = currentUserId.ifEmpty { "SHARED_STORE_DATA" } // Fallback to default if empty

        if (transactionMonthId == currentMonthId) {
            _uiState.update { state ->
                val newTransaction = com.example.toycity.data.CashTransaction(
                    id = java.util.UUID.randomUUID().toString(),
                    amount = amount,
                    note = note,
                    isCashIn = isCashIn,
                    date = date,
                    category = category
                )
                state.copy(cashTransactions = state.cashTransactions + newTransaction)
            }
            triggerManualSave()
        } else {
            viewModelScope.launch {
                try {
                    val targetRecord = repository.getRecord(userId, transactionMonthId) 
                        ?: com.example.toycity.data.FinancialRecord(id = transactionMonthId, userId = userId)
                    
                    val newTransaction = com.example.toycity.data.CashTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        amount = amount,
                        note = note,
                        isCashIn = isCashIn,
                        date = date,
                        category = category
                    )
                    
                    val updatedRecord = targetRecord.copy(
                        cashTransactions = targetRecord.cashTransactions + newTransaction
                    )
                    repository.saveRecord(updatedRecord)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun removeCashTransaction(transactionId: String) {
        val currentTransaction = _uiState.value.cashTransactions.find { it.id == transactionId }
        if (currentTransaction != null) {
            _uiState.update { state ->
                state.copy(cashTransactions = state.cashTransactions.filter { it.id != transactionId })
            }
            triggerManualSave()
        } else {
            viewModelScope.launch {
                val records = _allRecordsRaw.value
                val recordWithTransaction = records.find { it.cashTransactions.any { t -> t.id == transactionId } }
                if (recordWithTransaction != null) {
                    val updatedRecord = recordWithTransaction.copy(
                        cashTransactions = recordWithTransaction.cashTransactions.filter { it.id != transactionId }
                    )
                    repository.saveRecord(updatedRecord)
                }
            }
        }
    }

    fun deleteSale(saleId: String) {
        _uiState.update { state ->
            val saleToDelete = state.sales.find { it.id == saleId } ?: return@update state
            
            // Deduct from total sales
            val newTotalSales = (state.totalSales - saleToDelete.totalAmount).coerceAtLeast(0.0)
            
            // Calculate COGS to deduct
            var cogsToDeduct = 0.0
            val updatedInventoryItems = state.inventoryData.items.map { item ->
                val saleItem = saleToDelete.items.find { it.productId == item.id }
                if (saleItem != null) {
                    cogsToDeduct += item.costPrice * saleItem.quantity
                    // Add back to inventory
                    item.copy(quantity = item.quantity + saleItem.quantity)
                } else {
                    item
                }
            }

            state.copy(
                totalSales = newTotalSales,
                sales = state.sales.filter { it.id != saleId },
                inventoryData = state.inventoryData.copy(
                    items = updatedInventoryItems,
                    cogs = (state.inventoryData.cogs - cogsToDeduct).coerceAtLeast(0.0)
                )
            )
        }
        triggerManualSave()
    }

    fun updateSale(updatedSale: com.example.toycity.data.Sale) {
        _uiState.update { state ->
            val oldSale = state.sales.find { it.id == updatedSale.id } ?: return@update state
            
            // First, "revert" the old sale
            var cogsToRevert = 0.0
            val revertedItems = state.inventoryData.items.map { item ->
                val oldSaleItem = oldSale.items.find { it.productId == item.id }
                if (oldSaleItem != null) {
                    cogsToRevert += item.costPrice * oldSaleItem.quantity
                    item.copy(quantity = item.quantity + oldSaleItem.quantity)
                } else {
                    item
                }
            }

            // Now, "apply" the updated sale
            var newTotalAmount = 0.0
            var newCogs = 0.0
            val updatedInventoryItems = revertedItems.map { item ->
                val newSaleItem = updatedSale.items.find { it.productId == item.id }
                if (newSaleItem != null) {
                    // Use the price from the receipt (maintains discounts) instead of the global item sellingPrice
                    newTotalAmount += newSaleItem.price * newSaleItem.quantity
                    newCogs += item.costPrice * newSaleItem.quantity
                    item.copy(quantity = (item.quantity - newSaleItem.quantity).coerceAtLeast(0))
                } else {
                    item
                }
            }

            val finalSale = updatedSale.copy(totalAmount = newTotalAmount)

            state.copy(
                totalSales = state.totalSales - oldSale.totalAmount + newTotalAmount,
                sales = state.sales.map { if (it.id == updatedSale.id) finalSale else it },
                inventoryData = state.inventoryData.copy(
                    items = updatedInventoryItems,
                    cogs = state.inventoryData.cogs - cogsToRevert + newCogs
                )
            )
        }
        triggerManualSave()
    }

    fun returnProductFromSale(saleId: String, productId: String, quantityToReturn: Int) {
        _uiState.update { state ->
            val sale = state.sales.find { it.id == saleId } ?: return@update state
            val saleItem = sale.items.find { it.productId == productId } ?: return@update state
            
            val actualReturnQty = quantityToReturn.coerceAtMost(saleItem.quantity)
            if (actualReturnQty <= 0) return@update state

            // Update Sale Items
            val updatedSaleItems = sale.items.map {
                if (it.productId == productId) {
                    it.copy(quantity = it.quantity - actualReturnQty)
                } else it
            }.filter { it.quantity > 0 }

            val amountToDeduct = saleItem.price * actualReturnQty
            val updatedSale = sale.copy(
                items = updatedSaleItems,
                totalAmount = (sale.totalAmount - amountToDeduct).coerceAtLeast(0.0)
            )

            // Update Inventory and COGS
            var cogsToDeduct = 0.0
            val updatedInventoryItems = state.inventoryData.items.map { item ->
                if (item.id == productId) {
                    cogsToDeduct = item.costPrice * actualReturnQty
                    item.copy(quantity = item.quantity + actualReturnQty)
                } else item
            }

            // If sale has no items left, remove it
            val updatedSales = if (updatedSaleItems.isEmpty()) {
                state.sales.filter { it.id != saleId }
            } else {
                state.sales.map { if (it.id == saleId) updatedSale else it }
            }

            state.copy(
                totalSales = (state.totalSales - amountToDeduct).coerceAtLeast(0.0),
                sales = updatedSales,
                inventoryData = state.inventoryData.copy(
                    items = updatedInventoryItems,
                    cogs = (state.inventoryData.cogs - cogsToDeduct).coerceAtLeast(0.0)
                )
            )
        }
        triggerManualSave()
    }
}
