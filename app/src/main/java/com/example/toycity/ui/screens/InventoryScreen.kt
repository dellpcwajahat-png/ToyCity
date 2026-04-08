package com.example.toycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.data.InventoryItem
import com.example.toycity.ui.FinancialViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: FinancialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Inventory Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.inventoryData.items) { item ->
                    InventoryCard(
                        item = item,
                        onUpdate = { viewModel.updateInventoryItem(it) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddInventoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, cost, sale ->
                viewModel.addInventoryItem(
                    InventoryItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        quantity = qty,
                        costPrice = cost,
                        sellingPrice = sale
                    )
                )
                showAddDialog = false
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete '${item.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInventoryItem(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InventoryCard(
    item: InventoryItem,
    onUpdate: (InventoryItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stock: ${item.quantity} | Cost: PKR ${item.costPrice} | Sale: PKR ${item.sellingPrice}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUpdate(item.copy(quantity = item.quantity - 1)) }) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = { onUpdate(item.copy(quantity = item.quantity + 1)) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddInventoryDialog(onDismiss: () -> Unit, onConfirm: (String, Int, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                TextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") })
                TextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost Price") })
                TextField(value = sellingPrice, onValueChange = { sellingPrice = it }, label = { Text("Selling Price") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(
                    name, 
                    qty.toIntOrNull() ?: 0, 
                    costPrice.toDoubleOrNull() ?: 0.0,
                    sellingPrice.toDoubleOrNull() ?: 0.0
                )
            }) { Text("Add") }
        }
    )
}
