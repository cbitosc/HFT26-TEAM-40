package com.sunless.lynq.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunless.lynq.ui.components.CmdBar
import com.sunless.lynq.ui.screens.DashboardScreen
import com.sunless.lynq.ui.screens.EmergencyScreen
import com.sunless.lynq.ui.screens.MeshExplorerScreen
import com.sunless.lynq.ui.screens.MessagesScreen
import com.sunless.lynq.ui.screens.SettingsScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless.lynq.ui.LynqViewModel
import com.sunless.lynq.ui.theme.Lynq

private val TabOrder = mapOf(
    Screen.Dashboard.route to 0,
    Screen.Messages.route to 1,
    Screen.Emergency.route to 2,
    Screen.MeshExplorer.route to 3,
    Screen.Settings.route to 4,
)

private fun getSlideDirection(fromRoute: String?, toRoute: String?): Int {
    val fromIdx = TabOrder[fromRoute?.substringBefore("/")] ?: 0
    val toIdx = TabOrder[toRoute?.substringBefore("/")] ?: 0
    return if (toIdx >= fromIdx) 1 else -1
}

// Buttery smooth synchronized slide duration
private const val SLIDE_DURATION = 300
private val SlideEasing = FastOutSlowInEasing

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: LynqViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route
    val openPeerId by viewModel.openPeerId.collectAsStateWithLifecycle()
    val showCmdBar = currentRoute != Screen.Messages.route || openPeerId == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pal.bg),
    ) {
        NavHost(
            navController = navController,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            startDestination = Screen.Dashboard.route,
            enterTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideInHorizontally(
                    animationSpec = tween(SLIDE_DURATION, easing = SlideEasing),
                ) { fullWidth -> dir * fullWidth }
            },
            exitTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideOutHorizontally(
                    animationSpec = tween(SLIDE_DURATION, easing = SlideEasing),
                ) { fullWidth -> -dir * fullWidth }
            },
            popEnterTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideInHorizontally(
                    animationSpec = tween(SLIDE_DURATION, easing = SlideEasing),
                ) { fullWidth -> dir * fullWidth }
            },
            popExitTransition = {
                val dir = getSlideDirection(initialState.destination.route, targetState.destination.route)
                slideOutHorizontally(
                    animationSpec = tween(SLIDE_DURATION, easing = SlideEasing),
                ) { fullWidth -> -dir * fullWidth }
            },
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController, viewModel) }
            composable(Screen.Messages.route) { MessagesScreen(navController, viewModel) }
            composable(Screen.Emergency.route) { EmergencyScreen(navController, viewModel) }
            composable(Screen.MeshExplorer.route) { MeshExplorerScreen(navController, viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(navController, viewModel) }
        }

        // Stationary Apple Island Dock anchored at the bottom
        AnimatedVisibility(
            visible = showCmdBar,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pal.bg),
            ) {
                CmdBar(
                    currentRoute = currentRoute,
                    onSelect = { navController.navigateTab(it) },
                )
            }
        }
    }
}

/**
 * Tab-style navigation: single instance per tab, state restored when
 * returning to a tab, and back from any tab exits the app instead of
 * rewinding through every screen you ever visited.
 */
fun NavHostController.navigateTab(route: String) {
    val startDest = graph.findStartDestination()
    if (route == startDest.route || route == Screen.Dashboard.route) {
        popBackStack(startDest.id, inclusive = false)
    } else {
        navigate(route) {
            popUpTo(startDest.id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Messages : Screen("messages")
    object Emergency : Screen("emergency")
    object MeshExplorer : Screen("mesh_explorer")
    object Settings : Screen("settings")

    companion object {
        fun fromRoute(route: String): Screen = when (route.substringBefore("/")) {
            Messages.route -> Messages
            Emergency.route -> Emergency
            MeshExplorer.route -> MeshExplorer
            Settings.route -> Settings
            else -> Dashboard
        }
    }
}