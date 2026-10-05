// MainActivity.kt
package com.mailgw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mailgw.data.api.RetrofitClient
import com.mailgw.data.repository.MailRepository
import com.mailgw.ui.components.MainDrawer
import com.mailgw.ui.navigation.Screen
import com.mailgw.ui.screens.*
import com.mailgw.ui.theme.MailGWTheme
import com.mailgw.viewmodel.MailViewModel
import com.mailgw.viewmodel.MailViewModelFactory
import com.mailgw.workers.MessagePollingWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start periodic message polling
        MessagePollingWorker.startPeriodic(this)

        setContent {
            MailGWTheme {
                MailGWApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailGWApp(
    viewModel: MailViewModel = viewModel(
        factory = MailViewModelFactory(
            repository = MailRepository(
                context = LocalContext.current,
                apiService = RetrofitClient.apiService
            )
        )
    )
) {
    val navController = rememberNavController()
    val scaffoldState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isDrawerOpen by remember { mutableStateOf(false) }

    val activeAccount by viewModel.activeAccount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    // Auto-navigate to inbox if there's an active account
    LaunchedEffect(activeAccount) {
        if (activeAccount != null) {
            navController.navigate(Screen.Inbox.route) {
                popUpTo(Screen.Inbox.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (navController.currentDestination?.route) {
                            Screen.Inbox.route -> "Inbox"
                            Screen.Accounts.route -> "Accounts"
                            Screen.Settings.route -> "Settings"
                            else -> "Mail.gw"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { isDrawerOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (navController.currentDestination?.route == Screen.Inbox.route) {
                        IconButton(onClick = { viewModel.refreshMessages() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (activeAccount != null) {
                NavigationBar {
                    listOf(Screen.Inbox, Screen.Accounts, Screen.Settings).forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = navController.currentDestination?.route == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(screen.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            MainDrawer(
                isOpen = isDrawerOpen,
                onClose = { isDrawerOpen = false },
                onNavigate = { route ->
                    scope.launch {
                        isDrawerOpen = false
                        when (route) {
                            "create_account" -> navController.navigate("create_account")
                            "manage_accounts" -> navController.navigate(Screen.Accounts.route)
                            "settings" -> navController.navigate(Screen.Settings.route)
                        }
                    }
                }
            )

            NavHost(
                navController = navController,
                startDestination = if (activeAccount != null) Screen.Inbox.route else "create_account"
            ) {
                composable(Screen.Inbox.route) {
                    InboxScreen(
                        viewModel = viewModel,
                        onMessageClick = { messageId ->
                            navController.navigate("message_detail/$messageId")
                        }
                    )
                }

                composable(Screen.Accounts.route) {
                    AccountScreen(
                        viewModel = viewModel,
                        onAccountSelected = {
                            navController.navigate(Screen.Inbox.route) {
                                popUpTo(Screen.Inbox.route) { inclusive = true }
                            }
                        },
                        onCreateAccountClick = {
                            navController.navigate("create_account")
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("create_account") {
                    CreateAccountScreen(
                        viewModel = viewModel,
                        onAccountCreated = {
                            navController.popBackStack()
                            navController.navigate(Screen.Inbox.route)
                        }
                    )
                }

                composable("message_detail/{messageId}") { backStackEntry ->
                    val messageId = backStackEntry.arguments?.getString("messageId") ?: return@composable
                    MessageDetailScreen(
                        viewModel = viewModel,
                        messageId = messageId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}