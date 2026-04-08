package com.example.toycity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.InventoryItem
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.ui.components.ScreenHeader
import com.example.toycity.utils.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    viewModel: FinancialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("Walk-in Customer") }
    val cart = remember { mutableStateListOf<Triple<InventoryItem, Int, Double>>() } // Triple(Item, Quantity, OverriddenSellingPrice)
    
    val filteredItems = uiState.inventoryData.items.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery, ignoreCase = true)
    }

    val totalAmount = cart.sumOf { it.third * it.second }
    val originalTotal = cart.sumOf { it.first.sellingPrice * it.second }
    val totalDiscount = originalTotal - totalAmount

    var editingItem by remember { mutableStateOf<Triple<InventoryItem, Int, Double>?>(null) }

    Scaffold(
        topBar = {
            Column {
                ScreenHeader(title = "Point of Sale (POS)")
                
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search items...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            modifier = Modifier.weight(0.8f),
                            label = { Text("Customer Name") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (cart.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            if (totalDiscount > 0) {
                                Text(
                                    "Discount: ${Formatter.formatCurrency(totalDiscount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE67E22),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("Total Payable", style = MaterialTheme.typography.labelMedium)
                            Text(
                                Formatter.formatCurrency(totalAmount),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = {
                                val saleItems = cart.map { (item, qty, price) ->
                                    item.copy(sellingPrice = price) to qty
                                }
                                viewModel.recordSale(saleItems, customerName)
                                cart.clear()
                                customerName = "Walk-in Customer"
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checkout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Inventory List
            LazyColumn(
                modifier = Modifier.weight(1.2f).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems) { item ->
                    Card(
                        onClick = {
                            val existingIndex = cart.indexOfFirst { it.first.id == item.id }
                            if (existingIndex != -1) {
                                val currentQty = cart[existingIndex].second
                                if (currentQty < item.quantity) {
                                    cart[existingIndex] = Triple(item, currentQty + 1, cart[existingIndex].third)
                                }
                            } else if (item.quantity > 0) {
                                cart.add(Triple(item, 1, item.sellingPrice))
                            }
                        },
                        enabled = item.quantity > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(Formatter.formatCurrency(item.sellingPrice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Stock: ${item.quantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.quantity < item.lowStockThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (item.quantity <= 0) {
                                Text("OUT", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            VerticalDivider()

            // Cart Sidebar
            Column(modifier = Modifier.weight(0.8f).padding(8.dp)) {
                Text(
                    "Cart (${cart.sumOf { it.second }})", 
                    fontWeight = FontWeight.ExtraBold, 
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (cart.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                            Text("Empty", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(cart) { triple ->
                            val (item, qty, price) = triple
                            val discount = item.sellingPrice - price
                            Card(
                                onClick = { editingItem = triple },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("x$qty", style = MaterialTheme.typography.labelSmall)
                                            Spacer(Modifier.width(8.dp))
                                            Text(Formatter.formatCurrency(price), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        if (discount > 0) {
                                            Text(
                                                "Disc: ${Formatter.formatCurrency(discount)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE67E22),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconButton(onClick = { 
                                        val index = cart.indexOfFirst { it.first.id == item.id }
                                        if (index != -1) {
                                            if (cart[index].second > 1) {
                                                cart[index] = Triple(item, cart[index].second - 1, cart[index].third)
                                            } else {
                                                cart.removeAt(index)
                                            }
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { triple ->
        PriceEditDialog(
            item = triple.first,
            currentPrice = triple.third,
            onDismiss = { editingItem = null },
            onConfirm = { newPrice ->
                val index = cart.indexOfFirst { it.first.id == triple.first.id }
                if (index != -1) {
                    cart[index] = Triple(triple.first, triple.second, newPrice)
                }
                editingItem = null
            }
        )
    }
}

@Composable
fun PriceEditDialog(
    item: InventoryItem,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(currentPrice.toString()) }
    val originalPrice = item.sellingPrice
    val enteredPrice = priceText.toDoubleOrNull() ?: 0.0
    val discount = originalPrice - enteredPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Selling Price") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text("Original Price: ${Formatter.formatCurrency(originalPrice)}", style = MaterialTheme.typography.bodySmall)
                
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) priceText = it },
                    label = { Text("New Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true
                )

                if (discount > 0) {
                    Text(
                        "Discount: ${Formatter.formatCurrency(discount)}",
                        color = Color(0xFFE67E22),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else if (discount < 0) {
                    Text(
                        "Premium: ${Formatter.formatCurrency(-discount)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(enteredPrice) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
