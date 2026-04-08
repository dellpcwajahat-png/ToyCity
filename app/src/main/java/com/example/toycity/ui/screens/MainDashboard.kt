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
import com.example.toycity.utils.Formatter
import com.example.toycity.utils.SecurityManager
import com.example.toycity.ui.components.ScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: FinancialViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Summary, 2: Top Selling, 3: Stock, 4: Expenses, 5: Categories, 6: Customers, 7: Cash, 8: Counter, 9: Settings, 10: Ledger
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
                    label = { Text("Suppliers") },
                    selected = selectedTab == 11,
                    onClick = { 
                        selectedTab = 11
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Cash In/Out") },
                    selected = selectedTab == 7,
                    onClick = { 
                        selectedTab = 7
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Toy City",
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
                // Show month picker only for Summary, Top Selling, and Expenses
                if (selectedTab in listOf(1, 2, 4, 12, 13) && !isAllTimeView) {
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
                    5 -> CategoriesScreen()
                    6 -> CustomersScreen()
                    7 -> CashInOutScreen(viewModel = viewModel)
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
    MAIN, PROFILE, SECURITY, DATA_MANAGEMENT, BACKUP, RESTORE, RECEIPT, PRINTER
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

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section
            SettingsGroup(title = "Account") {
            SettingsItem(
                title = "Profile",
                subtitle = user?.displayName ?: user?.email ?: "Manage your profile",
                icon = Icons.Default.Person,
                onClick = { onNavigate(SettingsView.PROFILE) }
            )
            SettingsItem(
                title = "Security",
                subtitle = "App lock, PIN, and Biometrics",
                icon = Icons.Default.Security,
                onClick = { onNavigate(SettingsView.SECURITY) }
            )
        }

        // Hardware & Customization
        SettingsGroup(title = "Hardware & Customization") {
            SettingsItem(
                title = "Printer Setting",
                subtitle = "Manage Bluetooth printer and paper size",
                icon = Icons.Default.Print,
                onClick = { onNavigate(SettingsView.PRINTER) }
            )
            SettingsItem(
                title = "Receipt Settings",
                subtitle = "Customize your business receipts",
                icon = Icons.Default.Receipt,
                onClick = { onNavigate(SettingsView.RECEIPT) }
            )
        }

        // Data Management
        SettingsGroup(title = "Data Management") {
            SettingsItem(
                title = "Backup & Restore",
                subtitle = "Manage your data storage",
                icon = Icons.Default.CloudSync,
                onClick = { onNavigate(SettingsView.DATA_MANAGEMENT) }
            )
        }

        // Logout
        SettingsGroup(title = "Exit") {
            SettingsItem(
                title = "Sign Out",
                subtitle = "Securely logout from your account",
                icon = Icons.AutoMirrored.Filled.Logout,
                textColor = MaterialTheme.colorScheme.error,
                onClick = { showLogoutConfirm = true }
            )
        }
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
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        onUpdateName(newName)
                        android.widget.Toast.makeText(context, "Profile updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
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
                android.widget.Toast.makeText(context, "PIN set successfully", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Security",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup(title = "App Lock") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable App Lock", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Require PIN or Biometric to open the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsItem(
                    title = "Change PIN",
                    icon = Icons.Default.Password,
                    onClick = { showPinDialog = true }
                )
                
                if (canUseBiometric) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Biometrics", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Use Fingerprint or Face ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
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
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 4-Digit PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("Enter PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.width(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (pin.length == 4) onPinSet(pin) },
                enabled = pin.length == 4
            ) {
                Text("Confirm")
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            ScreenHeader(title = "Receipt Settings")
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            SettingsGroup(title = "Business Information") {
                ReceiptTextField(label = "Business Name", value = businessName, onValueChange = { businessName = it }, icon = Icons.Default.Business)
                ReceiptTextField(label = "Address", value = businessAddress, onValueChange = { businessAddress = it }, icon = Icons.Default.LocationOn)
                ReceiptTextField(label = "Phone Number", value = businessPhone, onValueChange = { businessPhone = it }, icon = Icons.Default.Phone)
                ReceiptTextField(label = "NTN No", value = ntnNo, onValueChange = { ntnNo = it }, icon = Icons.Default.Numbers)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "Receipt Customization") {
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
                Text("Save Settings", style = MaterialTheme.typography.titleMedium)
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
            title = { Text("Select Printer") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (pairedDevices.isEmpty()) {
                        Text("No paired devices found. Please pair your printer in Bluetooth settings first.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        pairedDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPrinterAddress = device.address
                                        showPrinterDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    @SuppressLint("MissingPermission")
                                    val deviceName = device.name ?: "Unknown Device"
                                    Text(deviceName, style = MaterialTheme.typography.bodyLarge)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                if (selectedPrinterAddress == device.address) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            ScreenHeader(title = "Printer Settings")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SettingsGroup(title = "Printer Configuration") {
                SettingsItem(
                    title = "Connect Printer",
                    subtitle = if (selectedPrinterAddress.isEmpty()) "Select your Bluetooth printer" else "Connected to: $selectedPrinterAddress",
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
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
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
                        Text("Page Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Choose your paper width", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                Text("Save & Apply", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Data Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsGroup(title = "Storage Actions") {
            SettingsItem(
                title = "Export Backup",
                subtitle = "Create a copy of your data",
                icon = Icons.Default.CloudDownload,
                onClick = { onNavigate(SettingsView.BACKUP) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SettingsItem(
                title = "Import Backup",
                subtitle = "Restore data from a file",
                icon = Icons.Default.CloudUpload,
                onClick = { onNavigate(SettingsView.RESTORE) }
            )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Backup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Export Your Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Create a backup file of all your transactions, products, and records. You can use this file to restore your data later or on another device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onExportData()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export Now")
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Restore",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Restore Your Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Select a previously exported backup file to restore your data. Warning: This will overwrite your current data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onImportData,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Select Backup File")
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (textColor == MaterialTheme.colorScheme.error) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                color = textColor, 
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        if (showChevron && textColor != MaterialTheme.colorScheme.error) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
