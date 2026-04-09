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
        val combined = if (current.id.isEmpty()) {
            all
        } else {
            val otherMonths = all.filter { it.id != current.id }
            otherMonths + current
        }
        
        combined.sortedWith { r1, r2 ->
            val d1 = Formatter.parseMonth(r1.id) ?: java.util.Date(0)
            val d2 = Formatter.parseMonth(r2.id) ?: java.util.Date(0)
            d1.compareTo(d2)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAllTimeView = MutableStateFlow(false)
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
                    // Try to fetch previous month's data for carry-over
                    val prevMonthId = getPreviousMonthId(monthId)
                    val prevRecord = repository.getRecord(userId, prevMonthId)
                    
                    val startingCash = prevRecord?.cashInDrawer ?: 0.0
                    val carriedLoans = prevRecord?.loans ?: emptyList()
                    val carriedInventoryItems = prevRecord?.inventoryData?.items ?: emptyList()
                    val carriedReceivables = prevRecord?.customerReceivables ?: 0.0
                    
                    _uiState.update { current ->
                        if (current.id != monthId) {
                            FinancialRecord(
                                id = monthId, 
                                userId = userId, 
                                startingCash = startingCash,
                                loans = carriedLoans,
                                customerReceivables = carriedReceivables,
                                inventoryData = com.example.toycity.data.InventoryData(items = carriedInventoryItems)
                            )
                        } else {
                            // If we're already on this month but record was null (newly created), 
                            // update carry-over values if they are default
                            current.copy(
                                startingCash = if (current.startingCash == 0.0) startingCash else current.startingCash,
                                loans = if (current.loans.isEmpty()) carriedLoans else current.loans,
                                customerReceivables = if (current.customerReceivables == 0.0) carriedReceivables else current.customerReceivables,
                                inventoryData = if (current.inventoryData.items.isEmpty()) 
                                    current.inventoryData.copy(items = carriedInventoryItems) 
                                    else current.inventoryData
                            )
                        }
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
            val month = parts[0].toInt()
            val year = parts[1].toInt()
            
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
                sales = state.sales + newSale,
                cashTransactions = state.cashTransactions + com.example.toycity.data.CashTransaction(
                    id = java.util.UUID.randomUUID().toString(),
                    amount = totalSaleAmount,
                    note = "Sale to $customerName (${newSale.id.takeLast(4)})",
                    isCashIn = true,
                    date = newSale.timestamp,
                    category = "Sale"
                )
            )
        }
        triggerManualSave()
    }

    fun addLender(name: String) {
        _uiState.update { state ->
            val newLender = Loan(lenderName = name)
            state.copy(loans = state.loans + newLender)
        }
        triggerManualSave()
    }

    fun deleteLender(name: String) {
        _uiState.update { state ->
            state.copy(loans = state.loans.filter { it.lenderName != name })
        }
        triggerManualSave()
    }

    fun addLoanTransaction(lenderName: String, amount: Double, isRepayment: Boolean, date: Long, note: String = "") {
        val transactionMonthId = Formatter.formatMonth(java.util.Date(date))
        
        _uiState.update { state ->
            val updatedLoans = state.loans.map { loan ->
                if (loan.lenderName == lenderName) {
                    if (isRepayment) {
                        loan.copy(repaymentToDate = loan.repaymentToDate + amount)
                    } else {
                        loan.copy(principalAmount = loan.principalAmount + amount)
                    }
                } else loan
            }

            val category = if (isRepayment) "Loan Repayment" else "Loan Received"
            val fullNote = if (note.isNotEmpty()) "$lenderName: $note" else lenderName
            
            val newTransaction = com.example.toycity.data.CashTransaction(
                id = java.util.UUID.randomUUID().toString(),
                amount = amount,
                note = fullNote,
                isCashIn = !isRepayment, // Loan Received is Cash In, Repayment is Cash Out
                date = date,
                category = category
            )

            state.copy(
                loans = updatedLoans,
                cashTransactions = if (state.id == transactionMonthId) state.cashTransactions + newTransaction else state.cashTransactions
            )
        }
        triggerManualSave()
    }

    fun updateLoanDueDate(lenderName: String, timestamp: Long?) {
        _uiState.update { state ->
            val updatedLoans = state.loans.map { 
                if (it.lenderName == lenderName) it.copy(dueDate = timestamp) else it 
            }
            state.copy(loans = updatedLoans)
        }
    }

    fun resetMonthlyLedger() {
        _uiState.update { state ->
            state.copy(
                startingCash = 0.0,
                totalSales = 0.0,
                operatingExpenses = 0.0,
                sales = emptyList(),
                cashTransactions = emptyList(),
                inventoryData = state.inventoryData.copy(restockInvestment = 0.0, cogs = 0.0)
            )
        }
        triggerManualSave()
    }

    fun updateStartingCash(value: Double) {
        _uiState.update { it.copy(startingCash = value) }
        triggerManualSave()
    }

    fun updateTotalSales(value: Double) {
        _uiState.update { it.copy(totalSales = value) }
    }

    fun updateOperatingExpenses(value: Double) {
        _uiState.update { it.copy(operatingExpenses = value) }
    }

    fun updateRestockInvestment(value: Double) {
        _uiState.update { it.copy(inventoryData = it.inventoryData.copy(restockInvestment = value)) }
    }

    fun updateCOGS(value: Double) {
        _uiState.update { it.copy(inventoryData = it.inventoryData.copy(cogs = value)) }
    }

    fun updateLoanPrincipal(lenderName: String, value: Double) {
        _uiState.update { state ->
            val updatedLoans = state.loans.map {
                if (it.lenderName == lenderName) it.copy(principalAmount = value) else it
            }
            state.copy(loans = updatedLoans)
        }
    }

    fun updateLoanRepayment(lenderName: String, value: Double) {
        _uiState.update { state ->
            val updatedLoans = state.loans.map {
                if (it.lenderName == lenderName) it.copy(repaymentToDate = value) else it
            }
            state.copy(loans = updatedLoans)
        }
    }

    fun updateCustomerReceivables(value: Double) {
        _uiState.update { it.copy(customerReceivables = value) }
        triggerManualSave()
    }

    fun addCashTransaction(amount: Double, note: String, isCashIn: Boolean, date: Long, category: String = "Operational", productId: String? = null) {
        val transactionMonthId = Formatter.formatMonth(java.util.Date(date))
        val userId = currentUserId.ifEmpty { "SHARED_STORE_DATA" }

        _uiState.update { state ->
            val newTransaction = com.example.toycity.data.CashTransaction(
                id = java.util.UUID.randomUUID().toString(),
                amount = amount,
                note = note,
                isCashIn = isCashIn,
                date = date,
                category = category,
                productId = productId
            )
            
            // If it's an Operational Expense, automatically update the operatingExpenses field
            val updatedOperatingExpenses = if (state.id == transactionMonthId && !isCashIn && category == "Operational") {
                state.operatingExpenses + amount
            } else state.operatingExpenses

            val updatedInventory = if (state.id == transactionMonthId && !isCashIn && category == "Restock") {
                val updatedItems = state.inventoryData.items.map { item ->
                    if (productId != null && item.id == productId) {
                        // If it's a specific product restock, update its quantity
                        // We use amount / costPrice to estimate quantity if it wasn't provided, 
                        // but usually it's better to just track the investment.
                        // However, per requirement "automated quantity increments":
                        val addedQty = if (item.costPrice > 0) (amount / item.costPrice).toInt() else 0
                        item.copy(
                            quantity = item.quantity + addedQty,
                            totalQuantity = item.totalQuantity + addedQty
                        )
                    } else if (productId == null && note.contains(item.name, ignoreCase = true)) {
                        item
                    } else item
                }
                state.inventoryData.copy(
                    restockInvestment = state.inventoryData.restockInvestment + amount,
                    items = updatedItems
                )
            } else state.inventoryData

            // If it's a Loan Repayment, try to match by note name
            val updatedLoans = if (state.id == transactionMonthId && !isCashIn && category == "Loan Repayment") {
                state.loans.map { loan ->
                    if (note.contains(loan.lenderName, ignoreCase = true)) {
                        loan.copy(repaymentToDate = loan.repaymentToDate + amount)
                    } else loan
                }
            } else state.loans

            state.copy(
                cashTransactions = if (state.id == transactionMonthId) state.cashTransactions + newTransaction else state.cashTransactions,
                operatingExpenses = updatedOperatingExpenses,
                inventoryData = updatedInventory,
                loans = updatedLoans
            )
        }
        triggerManualSave()

        // Handle off-month transactions separately if needed (rare for mobile entry)
        if (transactionMonthId != currentMonthId) {
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
                        category = category,
                        productId = productId
                    )

                    val updatedOperatingExpenses = if (!isCashIn && category == "Operational") {
                        targetRecord.operatingExpenses + amount
                    } else targetRecord.operatingExpenses

                    val updatedInventory = if (!isCashIn && category == "Restock") {
                        val updatedItems = targetRecord.inventoryData.items.map { item ->
                            if (productId != null && item.id == productId) {
                                val addedQty = if (item.costPrice > 0) (amount / item.costPrice).toInt() else 0
                                item.copy(
                                    quantity = item.quantity + addedQty,
                                    totalQuantity = item.totalQuantity + addedQty
                                )
                            } else item
                        }
                        targetRecord.inventoryData.copy(
                            restockInvestment = targetRecord.inventoryData.restockInvestment + amount,
                            items = updatedItems
                        )
                    } else targetRecord.inventoryData

                    val updatedLoans = if (!isCashIn && category == "Loan Repayment") {
                        targetRecord.loans.map { loan ->
                            if (note.contains(loan.lenderName, ignoreCase = true)) {
                                loan.copy(repaymentToDate = loan.repaymentToDate + amount)
                            } else loan
                        }
                    } else targetRecord.loans
                    
                    val updatedRecord = targetRecord.copy(
                        cashTransactions = targetRecord.cashTransactions + newTransaction,
                        operatingExpenses = updatedOperatingExpenses,
                        inventoryData = updatedInventory,
                        loans = updatedLoans
                    )
                    repository.saveRecord(updatedRecord)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateCashTransaction(oldTransaction: com.example.toycity.data.CashTransaction, newAmount: Double, newNote: String, newDate: Long) {
        val oldMonthId = Formatter.formatMonth(java.util.Date(oldTransaction.date))
        val newMonthId = Formatter.formatMonth(java.util.Date(newDate))
        
        if (oldMonthId == newMonthId) {
            _uiState.update { state ->
                val updatedTransactions = state.cashTransactions.map {
                    if (it.id == oldTransaction.id) {
                        it.copy(amount = newAmount, note = newNote, date = newDate)
                    } else it
                }
                
                var updatedOperatingExpenses = state.operatingExpenses
                if (!oldTransaction.isCashIn && oldTransaction.category == "Operational") {
                    updatedOperatingExpenses = updatedOperatingExpenses - oldTransaction.amount + newAmount
                }
                
                var updatedInventory = state.inventoryData
                if (!oldTransaction.isCashIn && oldTransaction.category == "Restock") {
                    updatedInventory = updatedInventory.copy(restockInvestment = updatedInventory.restockInvestment - oldTransaction.amount + newAmount)
                }
                
                var updatedLoans = state.loans
                if (!oldTransaction.isCashIn && oldTransaction.category == "Loan Repayment") {
                    updatedLoans = updatedLoans.map { loan ->
                        if (oldTransaction.note.contains(loan.lenderName, ignoreCase = true)) {
                            loan.copy(repaymentToDate = loan.repaymentToDate - oldTransaction.amount + newAmount)
                        } else loan
                    }
                }
                
                state.copy(
                    cashTransactions = updatedTransactions,
                    operatingExpenses = updatedOperatingExpenses,
                    inventoryData = updatedInventory,
                    loans = updatedLoans
                )
            }
            triggerManualSave()
        } else {
            // If month changed, remove from old and add to new
            removeCashTransaction(oldTransaction.id)
            addCashTransaction(newAmount, newNote, oldTransaction.isCashIn, newDate, oldTransaction.category)
        }
    }

    fun removeCashTransaction(transactionId: String) {
        val currentTransaction = _uiState.value.cashTransactions.find { it.id == transactionId }
        if (currentTransaction != null) {
            _uiState.update { state ->
                // Revert any associated operational expense, restock or loan repayment
                val updatedOperatingExpenses = if (!currentTransaction.isCashIn && currentTransaction.category == "Operational") {
                    (state.operatingExpenses - currentTransaction.amount).coerceAtLeast(0.0)
                } else state.operatingExpenses

                val updatedInventory = if (!currentTransaction.isCashIn && currentTransaction.category == "Restock") {
                    state.inventoryData.copy(restockInvestment = (state.inventoryData.restockInvestment - currentTransaction.amount).coerceAtLeast(0.0))
                } else state.inventoryData

                val updatedLoans = if (!currentTransaction.isCashIn && currentTransaction.category == "Loan Repayment") {
                    state.loans.map { loan ->
                        if (currentTransaction.note.contains(loan.lenderName, ignoreCase = true)) {
                            loan.copy(repaymentToDate = (loan.repaymentToDate - currentTransaction.amount).coerceAtLeast(0.0))
                        } else loan
                    }
                } else state.loans

                state.copy(
                    cashTransactions = state.cashTransactions.filter { it.id != transactionId },
                    operatingExpenses = updatedOperatingExpenses,
                    inventoryData = updatedInventory,
                    loans = updatedLoans
                )
            }
            triggerManualSave()
        } else {
            viewModelScope.launch {
                val records = _allRecordsRaw.value
                val recordWithTransaction = records.find { it.cashTransactions.any { t -> t.id == transactionId } }
                if (recordWithTransaction != null) {
                    val transactionToRemove = recordWithTransaction.cashTransactions.find { it.id == transactionId }!!
                    
                    val updatedOperatingExpenses = if (!transactionToRemove.isCashIn && transactionToRemove.category == "Operational") {
                        (recordWithTransaction.operatingExpenses - transactionToRemove.amount).coerceAtLeast(0.0)
                    } else recordWithTransaction.operatingExpenses

                    val updatedInventory = if (!transactionToRemove.isCashIn && transactionToRemove.category == "Restock") {
                        recordWithTransaction.inventoryData.copy(restockInvestment = (recordWithTransaction.inventoryData.restockInvestment - transactionToRemove.amount).coerceAtLeast(0.0))
                    } else recordWithTransaction.inventoryData

                    val updatedLoans = if (!transactionToRemove.isCashIn && transactionToRemove.category == "Loan Repayment") {
                        recordWithTransaction.loans.map { loan ->
                            if (transactionToRemove.note.contains(loan.lenderName, ignoreCase = true)) {
                                loan.copy(repaymentToDate = (loan.repaymentToDate - transactionToRemove.amount).coerceAtLeast(0.0))
                            } else loan
                        }
                    } else recordWithTransaction.loans

                    val updatedRecord = recordWithTransaction.copy(
                        cashTransactions = recordWithTransaction.cashTransactions.filter { it.id != transactionId },
                        operatingExpenses = updatedOperatingExpenses,
                        inventoryData = updatedInventory,
                        loans = updatedLoans
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

            val saleSuffix = saleId.takeLast(4)
            val updatedCashTransactions = state.cashTransactions.filterNot { 
                it.isCashIn && it.category == "Sale" && it.note.contains("($saleSuffix)") 
            }

            state.copy(
                totalSales = newTotalSales,
                sales = state.sales.filter { it.id != saleId },
                inventoryData = state.inventoryData.copy(
                    items = updatedInventoryItems,
                    cogs = (state.inventoryData.cogs - cogsToDeduct).coerceAtLeast(0.0)
                ),
                cashTransactions = updatedCashTransactions
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
            val saleSuffix = updatedSale.id.takeLast(4)
            val updatedCashTransactions = state.cashTransactions.map { tx ->
                if (tx.isCashIn && tx.category == "Sale" && tx.note.contains("($saleSuffix)")) {
                    tx.copy(amount = newTotalAmount)
                } else tx
            }

            state.copy(
                totalSales = state.totalSales - oldSale.totalAmount + newTotalAmount,
                sales = state.sales.map { if (it.id == updatedSale.id) finalSale else it },
                inventoryData = state.inventoryData.copy(
                    items = updatedInventoryItems,
                    cogs = state.inventoryData.cogs - cogsToRevert + newCogs
                ),
                cashTransactions = updatedCashTransactions
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

            val saleSuffix = saleId.takeLast(4)
            val updatedCashTransactions = if (updatedSaleItems.isEmpty()) {
                state.cashTransactions.filterNot { 
                    it.isCashIn && it.category == "Sale" && it.note.contains("($saleSuffix)") 
                }
            } else {
                state.cashTransactions.map { tx ->
                    if (tx.isCashIn && tx.category == "Sale" && tx.note.contains("($saleSuffix)")) {
                        tx.copy(amount = updatedSale.totalAmount)
                    } else tx
                }
            }

            state.copy(
                totalSales = (state.totalSales - amountToDeduct).coerceAtLeast(0.0),
                sales = updatedSales,
                inventoryData = state.inventoryData.copy(
                    items = updatedInventoryItems,
                    cogs = (state.inventoryData.cogs - cogsToDeduct).coerceAtLeast(0.0)
                ),
                cashTransactions = updatedCashTransactions
            )
        }
        triggerManualSave()
    }
}
