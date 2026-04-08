package com.example.toycity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel

@Composable
fun SummaryScreen(
    viewModel: FinancialViewModel = viewModel(),
    onExportPdf: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnalyticsScreen(viewModel = viewModel)
        
        ExtendedFloatingActionButton(
            onClick = onExportPdf,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
            text = { Text("Export PDF") }
        )
    }
}
