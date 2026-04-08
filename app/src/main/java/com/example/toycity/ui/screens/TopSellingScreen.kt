package com.example.toycity.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel

@Composable
fun TopSellingScreen(
    viewModel: FinancialViewModel = viewModel(),
    isAdmin: Boolean = false,
    onNavigateBack: () -> Unit = {}
) {
    if (isAdmin) {
        FinanceScreen(viewModel = viewModel)
    } else {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
    }
}
