// ui/navigation/NavGraph.kt
package com.mailgw.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Inbox : Screen("inbox", "Inbox", Icons.Default.Email)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.People)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

sealed class DrawerScreen(val route: String, val title: String, val icon: ImageVector) {
    object CreateAccount : DrawerScreen("create_account", "Create Account", Icons.Default.Add)
    object ManageAccounts : DrawerScreen("manage_accounts", "Manage Accounts", Icons.Default.People)
    object Settings : DrawerScreen("settings", "Settings", Icons.Default.Settings)
}