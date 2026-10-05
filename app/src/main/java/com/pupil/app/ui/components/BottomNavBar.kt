package com.pupil.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pupil.app.ui.theme.PupilPrimaryLight

enum class PupilTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Home),
    LIBRARY("library", "Library", Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun PupilBottomNavBar(
    currentRoute: String?,
    onTabSelected: (PupilTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        PupilTab.values().forEach { tab ->
            val isSelected = when (tab) {
                PupilTab.HOME -> currentRoute == "home"
                PupilTab.LIBRARY -> currentRoute == "library" || currentRoute?.startsWith("selection") == true
                PupilTab.SETTINGS -> currentRoute == "settings"
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PupilPrimaryLight,
                    selectedTextColor = PupilPrimaryLight,
                    indicatorColor = PupilPrimaryLight.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
