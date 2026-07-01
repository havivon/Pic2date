package com.pic2date.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.pic2date.ui.Phase

/**
 * Keeps the nav back-stack in sync with the current [Phase]. Each phase maps to
 * exactly one destination; transitions clear intermediate screens so Back does
 * the expected thing (e.g. Confirm -> Back -> Home, not -> Scanning).
 */
@Composable
fun PhaseNavigator(phase: Phase, navController: NavHostController) {
    LaunchedEffect(phase) {
        when (phase) {
            Phase.IDLE, Phase.ERROR -> navController.popToHome()
            Phase.SCANNING -> navController.navigateSingleTop(Routes.SCANNING)
            Phase.CONFIRM -> navController.navigate(Routes.CONFIRM) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
            Phase.SAVED -> navController.navigate(Routes.SUCCESS) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }
}

private fun NavHostController.popToHome() {
    if (currentDestination?.route != Routes.HOME) {
        navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    if (currentDestination?.route != route) {
        navigate(route) { launchSingleTop = true }
    }
}
