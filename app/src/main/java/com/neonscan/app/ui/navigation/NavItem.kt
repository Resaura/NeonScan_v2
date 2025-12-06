package com.neonscan.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : NavItem("home", "Accueil", Icons.Filled.Home)
    data object Files : NavItem("files", "Fichiers", Icons.Filled.Folder)
    data object Tools : NavItem("tools", "Outils", Icons.Filled.Apps)
    data object Account : NavItem("account", "Compte", Icons.Filled.Person)

    companion object {
        val bottomItems = listOf(Home, Files, Tools, Account)
    }
}
