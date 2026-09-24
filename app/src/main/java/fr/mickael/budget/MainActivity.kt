package fr.mickael.budget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.mickael.budget.data.TxType
import fr.mickael.budget.ui.MainViewModel
import fr.mickael.budget.ui.components.OperationRequest
import fr.mickael.budget.ui.components.OperationSheet
import fr.mickael.budget.ui.components.label
import fr.mickael.budget.ui.history.HistoryScreen
import fr.mickael.budget.ui.history.MonthDetailScreen
import fr.mickael.budget.ui.home.HomeScreen
import fr.mickael.budget.ui.operations.OperationsScreen
import fr.mickael.budget.ui.recurring.RecurringScreen
import fr.mickael.budget.ui.settings.SettingsScreen
import fr.mickael.budget.ui.theme.BudgetTheme
import java.time.YearMonth

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Accueil", Icons.Default.Home),
    Operations("operations", "Opérations", Icons.AutoMirrored.Filled.ReceiptLong),
    Recurring("recurring", "Fixes", Icons.Default.Repeat),
    History("history", "Historique", Icons.Default.BarChart),
    Settings("settings", "Réglages", Icons.Default.Settings),
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var operationRequest by mutableStateOf<OperationRequest?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        askNotificationPermission()

        setContent {
            BudgetTheme {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route
                val categories by vm.categories.collectAsStateWithLifecycle()
                val detailMonth = backStack?.arguments?.getString("month")

                Scaffold(
                    topBar = {
                        if (route == "month/{month}" && detailMonth != null) {
                            TopAppBar(
                                title = { Text(YearMonth.parse(detailMonth).label()) },
                                navigationIcon = {
                                    IconButton(onClick = { nav.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                                    }
                                },
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEach { tab ->
                                val selected = route == tab.route || (tab == Tab.History && route == "month/{month}")
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        nav.navigate(tab.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (route == Tab.Home.route || route == Tab.Operations.route) {
                            ExtendedFloatingActionButton(
                                onClick = { operationRequest = OperationRequest(TxType.EXPENSE) },
                                icon = { Icon(Icons.Default.Add, null) },
                                text = { Text("Ajouter") },
                            )
                        }
                    },
                ) { padding ->
                    val edit: (fr.mickael.budget.data.Operation) -> Unit = { operationRequest = OperationRequest(existing = it) }
                    NavHost(nav, startDestination = Tab.Home.route) {
                        composable(Tab.Home.route) {
                            HomeScreen(
                                vm, padding,
                                onAdd = { operationRequest = OperationRequest(it) },
                                onEdit = edit,
                                onSeeAll = { nav.navigate(Tab.Operations.route) { launchSingleTop = true } },
                            )
                        }
                        composable(Tab.Operations.route) { OperationsScreen(vm, padding, edit) }
                        composable(Tab.Recurring.route) { RecurringScreen(vm, padding) }
                        composable(Tab.History.route) {
                            HistoryScreen(vm, padding) { nav.navigate("month/$it") }
                        }
                        composable("month/{month}") { entry ->
                            val month = YearMonth.parse(entry.arguments?.getString("month"))
                            MonthDetailScreen(vm, month, padding, edit)
                        }
                        composable(Tab.Settings.route) { SettingsScreen(vm, padding) }
                    }
                }

                operationRequest?.let { req ->
                    OperationSheet(
                        request = req,
                        categories = categories,
                        onSave = { vm.saveOperation(it); operationRequest = null },
                        onDelete = { vm.deleteOperation(it); operationRequest = null },
                        onDismiss = { operationRequest = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Génère les récurrents si on a changé de mois depuis la dernière ouverture.
        vm.onAppStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_ADD_EXPENSE) {
            operationRequest = OperationRequest(TxType.EXPENSE)
            intent.action = null
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val ACTION_ADD_EXPENSE = "fr.mickael.budget.ADD_EXPENSE"

        fun openIntent(context: Context) = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        fun addExpenseIntent(context: Context) = openIntent(context).setAction(ACTION_ADD_EXPENSE)
    }
}
