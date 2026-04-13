package com.apislens.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.apislens.ui.screens.*

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object DeviceList : Screen("devices")
    object DeviceDetail : Screen("device/{deviceId}")
    object AddDevice : Screen("add_device")
    object EditDevice : Screen("edit_device/{deviceId}")
    object AddChargeRecord : Screen("add_charge/{deviceId}")
    object AddUsageRecord : Screen("add_usage/{deviceId}")
    object Settings : Screen("settings")

    fun withArgs(vararg args: Any): String = buildString {
        append(route.substringBefore("/"))
        args.forEach { append("/$it") }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "仪表盘", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(Screen.DeviceList, "设备", Icons.Filled.Devices, Icons.Outlined.Devices),
    BottomNavItem(Screen.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
)

val bottomNavRoutes = bottomNavItems.map { it.screen.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApisLensNavGraph(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val animatedContainerColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.surfaceContainer,
                    animationSpec = tween(durationMillis = 250),
                    label = "navBarContainer"
                )
                val animatedSelectedIconColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.primary,
                    animationSpec = tween(durationMillis = 250),
                    label = "navSelectedIcon"
                )
                val animatedSelectedTextColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.primary,
                    animationSpec = tween(durationMillis = 250),
                    label = "navSelectedText"
                )
                val animatedIndicatorColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.primaryContainer,
                    animationSpec = tween(durationMillis = 250),
                    label = "navIndicator"
                )
                val animatedUnselectedIconColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 250),
                    label = "navUnselectedIcon"
                )
                val animatedUnselectedTextColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 250),
                    label = "navUnselectedText"
                )

                NavigationBar(
                    containerColor = animatedContainerColor
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (currentDestination?.route != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = animatedSelectedIconColor,
                                selectedTextColor = animatedSelectedTextColor,
                                indicatorColor = animatedIndicatorColor,
                                unselectedIconColor = animatedUnselectedIconColor,
                                unselectedTextColor = animatedUnselectedTextColor
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToDeviceList = {
                        navController.navigate(Screen.DeviceList.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToDeviceDetail = { id -> navController.navigate(Screen.DeviceDetail.withArgs(id)) },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.DeviceList.route) {
                DeviceListScreen(
                    onDeviceClick = { id -> navController.navigate(Screen.DeviceDetail.withArgs(id)) },
                    onAddDevice = { navController.navigate(Screen.AddDevice.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.DeviceDetail.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
            ) { entry ->
                val deviceId = entry.arguments?.getLong("deviceId") ?: 0L
                DeviceDetailScreen(
                    deviceId = deviceId,
                    onBack = { navController.popBackStack() },
                    onAddChargeRecord = { navController.navigate(Screen.AddChargeRecord.withArgs(deviceId)) },
                    onAddUsageRecord = { navController.navigate(Screen.AddUsageRecord.withArgs(deviceId)) },
                    onEditDevice = { id -> navController.navigate(Screen.EditDevice.withArgs(id)) }
                )
            }
            composable(Screen.AddDevice.route) {
                AddEditDeviceScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditDevice.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
            ) { entry ->
                val deviceId = entry.arguments?.getLong("deviceId") ?: 0L
                AddEditDeviceScreen(
                    editDeviceId = deviceId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AddChargeRecord.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
            ) { entry ->
                val deviceId = entry.arguments?.getLong("deviceId") ?: 0L
                AddChargeRecordScreen(
                    deviceId = deviceId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AddUsageRecord.route,
                arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
            ) { entry ->
                val deviceId = entry.arguments?.getLong("deviceId") ?: 0L
                AddUsageRecordScreen(
                    deviceId = deviceId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
