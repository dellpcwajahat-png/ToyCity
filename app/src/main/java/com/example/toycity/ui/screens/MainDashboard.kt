package com.example.toycity.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toycity.ui.FinancialViewModel
import com.example.toycity.data.FinancialRecord
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalContext
import com.example.toycity.utils.PdfGenerator
import com.example.toycity.utils.DataBackupManager
import com.example.toycity.ui.AuthViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.toycity.utils.Formatter
import com.example.toycity.utils.SecurityManager
import com.example.toycity.ui.components.ScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: FinancialViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Summary, 2: Top Selling, 3: Stock, 4: Expenses, 5: Categories, 6: Customers, 8: Counter, 9: Settings, 10: Ledger, 11: Suppliers, 12: Op Expenses, 13: Restock Expenses, 14: Cash in hand, 15: Cash In/Out
    var currentMonth by remember { mutableStateOf(Formatter.formatMonth(Date())) }
    val user by authViewModel.user.collectAsState()
    val displayName by authViewModel.displayName.collectAsState()
    val adminEmails = listOf(
        "toycity90@gmail.com",
        "dellpcwajahat@gmail.com",
        "wajahatabbasicentral@gmail.com"
    )
    val isAdmin = user?.email != null && user?.email in adminEmails
    val uiState by viewModel.uiState.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val isAllTimeView by viewModel.isAllTimeView.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    DataBackupManager.importData(context, it, "SHARED_STORE_DATA")
                    viewModel.loadData("SHARED_STORE_DATA", currentMonth)
                }
            }
        }
    )

    LaunchedEffect(user, currentMonth) {
        user?.let {
            viewModel.loadData("SHARED_STORE_DATA", currentMonth)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Management & Insights",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    NavigationDrawerItem(
                        label = { Text("Business Summary") },
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Top Selling") },
                        selected = selectedTab == 2,
                        onClick = { 
                            selectedTab = 2
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Operational Expenses") },
                        selected = selectedTab == 12,
                        onClick = { 
                            selectedTab = 12
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.SettingsApplications, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Restock Expenses") },
                        selected = selectedTab == 13,
                        onClick = { 
                            selectedTab = 13
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Product Categories") },
                        selected = selectedTab == 5,
                        onClick = { 
                            selectedTab = 5
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Category, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Customers") },
                        selected = selectedTab == 6,
                        onClick = { 
                            selectedTab = 6
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Debt Management") },
                        selected = selectedTab == 11,
                        onClick = { 
                            selectedTab = 11
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Cash in hand") },
                        selected = selectedTab == 14,
                        onClick = { 
                            selectedTab = 14
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Cash In/Out Ledger") },
                        selected = selectedTab == 15,
                        onClick = { 
                            selectedTab = 15
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        val title = when(selectedTab) {
                            0 -> "Toy City"
                            1 -> "Business Summary"
                            2 -> "Top Selling"
                            3 -> "Product Stock"
                            4, 12, 13 -> "Expenses"
                            5 -> "Categories"
                            6 -> "Customers"
                            8 -> "Point of Sale"
                            9 -> "Settings"
                            10 -> "Financial Ledger"
                            11 -> "Debt Management"
                            14 -> "Cash in Hand"
                            15 -> "Cash Ledger"
                            else -> "Toy City"
                        }
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Dashboard") },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        label = { Text("Products") },
                        icon = { Icon(Icons.Default.Storage, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 8,
                        onClick = { selectedTab = 8 },
                        label = { Text("POS") },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 10,
                        onClick = { selectedTab = 10 },
                        label = { Text("Ledger") },
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 9,
                        onClick = { selectedTab = 9 },
                        label = { Text("Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // Show month picker only for Summary, Top Selling, Expenses, and Cash screens
                if (selectedTab in listOf(1, 2, 4, 12, 13, 14, 15) && !isAllTimeView) {
                    MonthPicker(
                        selectedMonth = currentMonth,
                        onMonthSelected = { currentMonth = it }
                    )
                }
                
                when (selectedTab) {
                    0 -> DashboardScreen(viewModel = viewModel)
                    1 -> SummaryScreen(
                        viewModel = viewModel,
                        onExportPdf = { showExportDialog = true }
                    )
                    2 -> TopSellingScreen(
                        viewModel = viewModel,
                        isAdmin = isAdmin,
                        onNavigateBack = { selectedTab = 0 }
                    )
                    3 -> StockManagementScreen(viewModel = viewModel)
                    4 -> ExpensesScreen(
                        viewModel = viewModel,
                        currentMonth = currentMonth,
                        onMonthSelected = { currentMonth = it }
                    )
                    12 -> ExpensesScreen(
                        viewModel = viewModel,
                        type = "Operational",
                        currentMonth = currentMonth,
                        onMonthSelected = { currentMonth = it }
                    )
                    13 -> ExpensesScreen(
                        viewModel = viewModel,
                        type = "Restock",
                        currentMonth = currentMonth,
                        onMonthSelected = { currentMonth = it }
                    )
                    14 -> CashInHandScreen(
                        viewModel = viewModel,
                        currentMonth = currentMonth,
                        onMonthSelected = { currentMonth = it }
                    )
                    15 -> CashInOutScreen(viewModel = viewModel)
                    5 -> CategoriesScreen()
                    6 -> CustomersScreen()
                    8 -> CounterScreen(viewModel = viewModel)
                    9 -> SettingsScreen(
                        authViewModel = authViewModel,
                        onExportData = {
                            scope.launch {
                                DataBackupManager.exportData(context, "SHARED_STORE_DATA")
                            }
                        },
                        onImportData = { importLauncher.launch(arrayOf("application/json")) },
                        context = context
                    )
                    10 -> FinanceScreen(viewModel = viewModel)
                    11 -> DebtManagementScreen(viewModel = viewModel)
                }
            }

            if (showExportDialog) {
                ExportFormatDialog(
                    isAllTimeView = isAllTimeView,
                    onDismiss = { showExportDialog = false },
                    onExport = { use58mm ->
                        if (isAllTimeView) {
                            val aggregateRecord = FinancialRecord(
                                id = "All-Time",
                                totalSales = allRecords.sumOf { it.totalSales },
                                operatingExpenses = allRecords.sumOf { it.operatingExpenses },
                                expenseCategories = allRecords.flatMap { it.expenseCategories.asIterable() }
                                    .groupBy({ it.key }, { it.value })
                                    .mapValues { it.value.sum() },
                                customerReceivables = allRecords.filter { it.totalSales > 0 || it.customerReceivables > 0 }
                                    .maxByOrNull { it.id }?.customerReceivables ?: 0.0,
                                inventoryData = com.example.toycity.data.InventoryData(
                                    restockInvestment = allRecords.sumOf { it.inventoryData.restockInvestment },
                                    cogs = allRecords.sumOf { it.inventoryData.cogs }
                                ),
                                startingCash = allRecords.minByOrNull { it.id }?.startingCash ?: 0.0,
                                loans = allRecords.flatMap { it.loans }
                                    .groupBy { it.lenderName }
                                    .map { (name, loans) ->
                                        com.example.toycity.data.Loan(
                                            lenderName = name,
                                            principalAmount = loans.sumOf { it.principalAmount },
                                            repaymentToDate = loans.sumOf { it.repaymentToDate }
                                        )
                                    }
                            )
                            PdfGenerator.generateFinancialReport(context, aggregateRecord, use58mm)
                        } else {
                            PdfGenerator.generateFinancialReport(context, uiState, use58mm)
                        }
                        showExportDialog = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCategoriesScreen(
    viewModel: com.example.toycity.ui.FinancialViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                "Custom Categories",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    uiState.customCategories.forEach { category ->
                        ListItem(
                            headlineContent = { Text(category) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeCustomCategory(category) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (category != uiState.customCategories.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Text(
                "These categories will be available when recording new expenses.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(newCategoryName.trim())
                            newCategoryName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExportFormatDialog(
    isAllTimeView: Boolean,
    onDismiss: () -> Unit,
    onExport: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Report") },
        text = {
            Column {
                Text(
                    text = "Generate ${if (isAllTimeView) "Lifetime" else "Monthly"} PDF report. Choose your paper size:",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onExport(false) }) {
                    Text("A4 Size")
                }
                TextButton(onClick = { onExport(true) }) {
                    Text("58mm Thermal")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MonthPicker(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        MonthPickerDialog(
            initialMonth = selectedMonth,
            onDismissRequest = { showDialog = false },
            onMonthSelected = onMonthSelected
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Period: $selectedMonth",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Month")
        }
    }
}


@Composable
fun MonthPickerDialog(
    initialMonth: String,
    onDismissRequest: () -> Unit,
    onMonthSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val date = try { sdf.parse(initialMonth) } catch (e: Exception) { null } ?: Date()
    val calendar = Calendar.getInstance().apply { time = date }
    
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }

    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Period") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    val monthsPerRow = 3
                    for (i in months.indices step monthsPerRow) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (j in 0 until monthsPerRow) {
                                val monthIndex = i + j
                                if (monthIndex < months.size) {
                                    val monthName = months[monthIndex]
                                    val isSelected = monthIndex == selectedMonthIndex
                                    
                                    Button(
                                        onClick = { selectedMonthIndex = monthIndex },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(monthName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val resultCalendar = Calendar.getInstance()
                resultCalendar.set(Calendar.YEAR, selectedYear)
                resultCalendar.set(Calendar.MONTH, selectedMonthIndex)
                onMonthSelected(sdf.format(resultCalendar.time))
                onDismissRequest()
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

enum class SettingsView {
    MAIN, PROFILE, SECURITY, DATA_MANAGEMENT, BACKUP, RESTORE, RECEIPT, PRINTER, EXPENSE_CATEGORIES
}

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    context: android.content.Context
) {
    var currentView by remember { mutableStateOf(SettingsView.MAIN) }
    val user by authViewModel.user.collectAsState()

    BackHandler(enabled = currentView != SettingsView.MAIN) {
        when (currentView) {
            SettingsView.BACKUP, SettingsView.RESTORE -> currentView = SettingsView.DATA_MANAGEMENT
            else -> currentView = SettingsView.MAIN
        }
    }

    AnimatedContent(
        targetState = currentView,
        transitionSpec = {
            if (targetState != SettingsView.MAIN) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "SettingsNavigation"
    ) { view ->
        when (view) {
            SettingsView.MAIN -> SettingsMainView(
                user = user,
                onNavigate = { currentView = it },
                onLogout = { authViewModel.signOut(context) }
            )
            SettingsView.PROFILE -> ProfileScreen(
                user = user,
                onUpdateName = { authViewModel.updateDisplayName(it) },
                onBack = { currentView = SettingsView.MAIN }
            )
            SettingsView.SECURITY -> SecuritySettingsScreen(
                context = context,
                onBack = { currentView = SettingsView.MAIN }
            )
            SettingsView.DATA_MANAGEMENT -> DataManagementScreen(
                onNavigate = { currentView = it },
                onBack = { currentView = SettingsView.MAIN }
            )
            SettingsView.BACKUP -> BackupScreen(
                onExportData = onExportData,
                onBack = { currentView = SettingsView.DATA_MANAGEMENT }
            )
            SettingsView.RESTORE -> RestoreScreen(
                onImportData = onImportData,
                onBack = { currentView = SettingsView.DATA_MANAGEMENT }
            )
            SettingsView.RECEIPT -> ReceiptSettingsScreen(
                context = context,
                onBack = { currentView = SettingsView.MAIN }
            )
            SettingsView.PRINTER -> PrinterSettingsScreen(
                context = context,
                onBack = { currentView = SettingsView.MAIN }
            )
            SettingsView.EXPENSE_CATEGORIES -> ExpenseCategoriesScreen(
                onBack = { currentView = SettingsView.MAIN }
            )
        }
    }
}


@Composable
fun SettingsMainView(
    user: com.google.firebase.auth.FirebaseUser?,
    onNavigate: (SettingsView) -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout from Toy City?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Settings")

        // User Profile Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onNavigate(SettingsView.PROFILE) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user?.displayName?.take(1)?.uppercase() ?: user?.email?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.displayName ?: "Business Owner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = user?.email ?: "No email linked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section
            SettingsGroup(title = "Security & Access") {
                SettingsItem(
                    title = "App Security",
                    subtitle = "PIN, Biometrics & Privacy",
                    icon = Icons.Default.Security,
                    onClick = { onNavigate(SettingsView.SECURITY) }
                )
            }

            // Hardware & Customization
            SettingsGroup(title = "Business Setup") {
                SettingsItem(
                    title = "Expense Categories",
                    subtitle = "Manage custom business expenses",
                    icon = Icons.Default.Category,
                    onClick = { onNavigate(SettingsView.EXPENSE_CATEGORIES) }
                )
                SettingsItem(
                    title = "Printer Settings",
                    subtitle = "Bluetooth & Paper Size",
                    icon = Icons.Default.Print,
                    onClick = { onNavigate(SettingsView.PRINTER) }
                )
                SettingsItem(
                    title = "Receipt Designer",
                    subtitle = "Customize business receipts",
                    icon = Icons.Default.Receipt,
                    onClick = { onNavigate(SettingsView.RECEIPT) }
                )
            }

            // Data Management
            SettingsGroup(title = "Data & Storage") {
                SettingsItem(
                    title = "Backup & Restore",
                    subtitle = "Cloud & Local Data Management",
                    icon = Icons.Default.CloudSync,
                    onClick = { onNavigate(SettingsView.DATA_MANAGEMENT) }
                )
            }

            // Logout
            SettingsGroup(title = "Account Action") {
                SettingsItem(
                    title = "Sign Out",
                    subtitle = "Securely logout from your account",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = { showLogoutConfirm = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Composable
fun ProfileScreen(
    user: com.google.firebase.auth.FirebaseUser?,
    onUpdateName: (String) -> Unit,
    onBack: () -> Unit
) {
    var newName by remember { mutableStateOf(user?.displayName ?: "") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Profile Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Account Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Business / Owner Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                )

                OutlinedTextField(
                    value = user?.email ?: "",
                    onValueChange = { },
                    label = { Text("Registered Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onUpdateName(newName)
                        android.widget.Toast.makeText(context, "Profile updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}


@Composable
fun SecuritySettingsScreen(
    context: android.content.Context,
    onBack: () -> Unit
) {
    var isLockEnabled by remember { mutableStateOf(SecurityManager.isAppLockEnabled(context)) }
    var isBiometricEnabled by remember { mutableStateOf(SecurityManager.isBiometricEnabled(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    val canUseBiometric = remember { SecurityManager.canAuthenticateWithBiometrics(context) }

    if (showPinDialog) {
        SetPinDialog(
            onDismiss = { showPinDialog = false },
            onPinSet = { newPin ->
                SecurityManager.setPin(context, newPin)
                SecurityManager.setAppLockEnabled(context, true)
                isLockEnabled = true
                showPinDialog = false
                android.widget.Toast.makeText(context, "PIN updated successfully", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "App Security",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Manage your privacy & protection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Card for Security Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLockEnabled) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isLockEnabled) Icons.Default.Shield else Icons.Default.GppMaybe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        if (isLockEnabled) "Protection Active" else "Protection Disabled",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (isLockEnabled) "Your data is secured with a PIN" else "Enable App Lock to secure your data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SettingsGroup(title = "Authentication Methods") {
            // App Lock Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { 
                        if (!isLockEnabled) showPinDialog = true 
                        else {
                            SecurityManager.setAppLockEnabled(context, false)
                            isLockEnabled = false
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable App Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Require PIN to open ToyCity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = isLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinDialog = true
                        } else {
                            SecurityManager.setAppLockEnabled(context, false)
                            isLockEnabled = false
                        }
                    }
                )
            }

            if (isLockEnabled) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                // Update PIN Item
                SettingsItem(
                    title = "Change Security PIN",
                    subtitle = "Update your 4-digit numeric code",
                    icon = Icons.Default.VpnKey,
                    onClick = { showPinDialog = true }
                )
                
                if (canUseBiometric) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    // Biometric Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { 
                                val newStatus = !isBiometricEnabled
                                SecurityManager.setBiometricEnabled(context, newStatus)
                                isBiometricEnabled = newStatus
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Unlock with Fingerprint or Face ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                SecurityManager.setBiometricEnabled(context, enabled)
                                isBiometricEnabled = enabled
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Security Tips
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Security Tips",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Use a PIN that is difficult to guess. Avoid using birth dates or simple patterns like 1234.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val isError = pin.isNotEmpty() && pin.length < 4

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(32.dp))
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Icon and Title Header
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        "Security PIN",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Enter a 4-digit code to protect your business data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // PIN Input Field
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    placeholder = { 
                        Text(
                            "0000", 
                            modifier = Modifier.fillMaxWidth(), 
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 8.sp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ) 
                    },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.width(180.dp),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                    ),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Must be 4 digits", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { if (pin.length == 4) onPinSet(pin) },
                        enabled = pin.length == 4,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Save PIN")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptSettingsScreen(
    context: android.content.Context,
    onBack: () -> Unit
) {
    val settings = remember { SecurityManager.getReceiptSettings(context) }
    var businessName by remember { mutableStateOf(settings["name"] ?: "") }
    var businessAddress by remember { mutableStateOf(settings["address"] ?: "") }
    var businessPhone by remember { mutableStateOf(settings["phone"] ?: "") }
    var thankYouNote by remember { mutableStateOf(settings["note"] ?: "") }
    var salesPerson by remember { mutableStateOf(settings["salesPerson"] ?: "") }
    var ntnNo by remember { mutableStateOf(settings["ntnNo"] ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Receipt Designer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            SettingsGroup(title = "Business Information") {
                ReceiptTextField(label = "Business Name", value = businessName, onValueChange = { businessName = it }, icon = Icons.Default.Business)
                ReceiptTextField(label = "Address", value = businessAddress, onValueChange = { businessAddress = it }, icon = Icons.Default.LocationOn)
                ReceiptTextField(label = "Phone Number", value = businessPhone, onValueChange = { businessPhone = it }, icon = Icons.Default.Phone)
                ReceiptTextField(label = "NTN No (Optional)", value = ntnNo, onValueChange = { ntnNo = it }, icon = Icons.Default.Numbers)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "Receipt Footer") {
                ReceiptTextField(label = "Sales Person Name", value = salesPerson, onValueChange = { salesPerson = it }, icon = Icons.Default.Person)
                ReceiptTextField(label = "Thank You Note", value = thankYouNote, onValueChange = { thankYouNote = it }, icon = Icons.Default.RateReview, isSingleLine = false)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    SecurityManager.saveReceiptSettings(
                        context, businessName, businessAddress, businessPhone, thankYouNote, salesPerson, ntnNo
                    )
                    android.widget.Toast.makeText(context, "Settings saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Composable
fun PrinterSettingsScreen(context: android.content.Context, onBack: () -> Unit) {
    val settings: Map<String, String> = remember { SecurityManager.getPrinterSettings(context) }
    var selectedPrinterAddress by remember { mutableStateOf(settings["address"] ?: "") }
    var pageSize by remember { mutableStateOf(settings["pageSize"] ?: "80mm") }
    
    var showPrinterDialog by remember { mutableStateOf(false) }
    val pairedDevices = remember { mutableStateListOf<android.bluetooth.BluetoothDevice>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            if (com.example.toycity.utils.PrinterManager.isBluetoothEnabled(context)) {
                pairedDevices.clear()
                pairedDevices.addAll(com.example.toycity.utils.PrinterManager.getPairedDevices(context))
                showPrinterDialog = true
            } else {
                android.widget.Toast.makeText(context, "Please enable Bluetooth", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Bluetooth permissions are required to connect a printer", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (showPrinterDialog) {
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Available Printers") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (pairedDevices.isEmpty()) {
                        Text("No paired devices found. Please pair your printer in Bluetooth settings first.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        pairedDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedPrinterAddress = device.address
                                        showPrinterDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    @SuppressLint("MissingPermission")
                                    val deviceName = device.name ?: "Unknown Device"
                                    Text(deviceName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                if (selectedPrinterAddress == device.address) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Printer Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        SettingsGroup(title = "Hardware Connection") {
            SettingsItem(
                title = "Select Bluetooth Printer",
                subtitle = if (selectedPrinterAddress.isEmpty()) "Choose your thermal printer" else "Linked to: $selectedPrinterAddress",
                icon = Icons.Default.Bluetooth,
                onClick = {
                    if (com.example.toycity.utils.PrinterManager.hasBluetoothPermissions(context)) {
                        if (com.example.toycity.utils.PrinterManager.isBluetoothEnabled(context)) {
                            pairedDevices.clear()
                            pairedDevices.addAll(com.example.toycity.utils.PrinterManager.getPairedDevices(context))
                            showPrinterDialog = true
                        } else {
                            android.widget.Toast.makeText(context, "Please enable Bluetooth", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            arrayOf(
                                android.Manifest.permission.BLUETOOTH_SCAN,
                                android.Manifest.permission.BLUETOOTH_CONNECT
                            )
                        } else {
                            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        permissionLauncher.launch(permissions)
                    }
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paper Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Standard thermal width", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                
                Row {
                    FilterChip(
                        selected = pageSize == "80mm",
                        onClick = { pageSize = "80mm" },
                        label = { Text("80mm") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = pageSize == "58mm",
                        onClick = { pageSize = "58mm" },
                        label = { Text("58mm") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                SecurityManager.savePrinterSettings(context, selectedPrinterAddress, pageSize)
                android.widget.Toast.makeText(context, "Printer settings saved", android.widget.Toast.LENGTH_SHORT).show()
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply Settings", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}



@Composable
fun ReceiptTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    isSingleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = isSingleLine,
        maxLines = if (isSingleLine) 1 else 3,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    )
}

@Composable
fun DataManagementScreen(onNavigate: (SettingsView) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Data & Storage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        SettingsGroup(title = "Backup Operations") {
            SettingsItem(
                title = "Export Database",
                subtitle = "Create a secure backup file",
                icon = Icons.Default.CloudDownload,
                onClick = { onNavigate(SettingsView.BACKUP) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsItem(
                title = "Import Database",
                subtitle = "Restore records from backup",
                icon = Icons.Default.CloudUpload,
                onClick = { onNavigate(SettingsView.RESTORE) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Always backup your data before performing a factory reset or switching devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}


@Composable
fun BackupScreen(onExportData: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Cloud Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Secure Data Export",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Create a complete snapshot of your business records. This file contains all transactions, inventory data, and settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onExportData,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Backup File", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}


@Composable
fun RestoreScreen(onImportData: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Data Restoration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Restore Database",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Warning: Importing a backup will replace all current data. Ensure you have the correct .json backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onImportData,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Backup File", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}


@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}


@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (textColor == MaterialTheme.colorScheme.error) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                color = textColor, 
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        if (showChevron && textColor != MaterialTheme.colorScheme.error) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


