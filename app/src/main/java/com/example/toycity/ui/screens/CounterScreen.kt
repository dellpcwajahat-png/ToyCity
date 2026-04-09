package com.example.toycity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.InventoryItem
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.ui.components.ScreenHeader
import com.example.toycity.utils.Formatter

@Composable
fun CounterScreen(
    viewModel: FinancialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("Walk-in Customer") }
    val cart = remember { mutableStateListOf<Triple<InventoryItem, Int, Double>>() } 
    
    val filteredItems = remember(uiState.inventoryData.items, searchQuery) {
        uiState.inventoryData.items.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalAmount = cart.sumOf { it.third * it.second }
    val originalTotal = cart.sumOf { it.first.sellingPrice * it.second }
    val totalDiscount = originalTotal - totalAmount

    var editingItem by remember { mutableStateOf<Triple<InventoryItem, Int, Double>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Point of Sale")
            
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Side: Inventory & Search
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search products...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            modifier = Modifier.weight(0.8f),
                            label = { Text("Customer") },
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                    }

                    if (filteredItems.isEmpty()) {
                        EmptySearchResults()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredItems) { item ->
                                POSItemCard(
                                    item = item,
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
                                    }
                                )
                            }
                        }
                    }
                }

                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Right Side: Cart Summary
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .fillMaxHeight()
                ) {
                    Text(
                        "Current Order",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    if (cart.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Your cart is empty",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart) { triple ->
                                CartItemCard(
                                    triple = triple,
                                    onEdit = { editingItem = triple },
                                    onRemove = {
                                        val index = cart.indexOfFirst { it.first.id == triple.first.id }
                                        if (index != -1) {
                                            if (cart[index].second > 1) {
                                                cart[index] = Triple(triple.first, cart[index].second - 1, cart[index].third)
                                            } else {
                                                cart.removeAt(index)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Order Summary
                        ElevatedCard(
                            modifier = Modifier.padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                if (totalDiscount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Discount", style = MaterialTheme.typography.bodySmall)
                                        Text("-${Formatter.formatCurrency(totalDiscount)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE67E22), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text("Total", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            Formatter.formatCurrency(totalAmount),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Checkout")
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
fun POSItemCard(item: InventoryItem, onClick: () -> Unit) {
    val isOutOfStock = item.quantity <= 0
    val isLowStock = item.quantity < item.lowStockThreshold

    ElevatedCard(
        onClick = onClick,
        enabled = !isOutOfStock,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isOutOfStock) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOutOfStock) Icons.Default.Block else Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = Formatter.formatCurrency(item.sellingPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isOutOfStock) "Out of Stock" else "Stock: ${item.quantity}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isOutOfStock -> MaterialTheme.colorScheme.error
                        isLowStock -> Color(0xFFE67E22)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
                if (!isOutOfStock) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    triple: Triple<InventoryItem, Int, Double>,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val (item, qty, price) = triple
    val discount = item.sellingPrice - price

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            "x$qty",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(Formatter.formatCurrency(price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit Price", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.RemoveCircleOutline, "Remove", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EmptySearchResults() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text("No products match your search", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Original Price: ${Formatter.formatCurrency(originalPrice)}", style = MaterialTheme.typography.bodySmall)
                
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) priceText = it },
                    label = { Text("New Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (discount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Discount: ${Formatter.formatCurrency(discount)}",
                            color = Color(0xFFE67E22),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else if (discount < 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Premium: ${Formatter.formatCurrency(-discount)}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(enteredPrice) }, shape = RoundedCornerShape(12.dp)) {
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
