package com.dotagency.cactusc2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dotagency.cactusc2.ui.navigation.CactusNavGraph
import com.dotagency.cactusc2.ui.navigation.Routes
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Single-activity entry point for Cactus C2.
 * Sets up the Compose content root with the dark Cactus theme and bottom navigation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CactusC2Theme {
                CactusApp()
            }
        }
    }
}

/** Metadata for a bottom navigation tab. */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/** Root composable: Scaffold with bottom nav bar, snackbar host, and nav graph. */
@Composable
fun CactusApp() {
    val vm: MainViewModel = viewModel()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.snackbar.collect { message ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }

    val bottomItems = listOf(
        BottomNavItem(Routes.DASHBOARD, Icons.Filled.Dashboard, "Dashboard"),
        BottomNavItem(Routes.DEVICES, Icons.Filled.Router, "Devices"),
        BottomNavItem(Routes.LIVE_PAYLOAD, Icons.Filled.Terminal, "Live"),
        BottomNavItem(Routes.PAYLOADS, Icons.Filled.Folder, "Payloads"),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on edit/detail screens
    val showBottomBar = currentRoute in listOf(
        Routes.DASHBOARD, Routes.DEVICES, Routes.LIVE_PAYLOAD, Routes.PAYLOADS
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CactusSurface,
                    contentColor = CactusText,
                    actionColor = CactusAccent
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = CactusSurface) {
                    bottomItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = {
                                Text(item.label, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CactusAccent,
                                selectedTextColor = CactusAccent,
                                unselectedIconColor = CactusTextDim,
                                unselectedTextColor = CactusTextDim,
                                indicatorColor = CactusAccent.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = CactusBg
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            CactusNavGraph(navController = navController, vm = vm)
        }
    }
}
