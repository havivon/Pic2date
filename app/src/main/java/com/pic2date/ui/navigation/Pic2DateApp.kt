package com.pic2date.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pic2date.ui.Phase
import com.pic2date.ui.ScanViewModel
import com.pic2date.ui.confirm.ConfirmScreen
import com.pic2date.ui.home.HomeScreen
import com.pic2date.ui.scanning.ScanningScreen
import com.pic2date.ui.success.SuccessScreen

object Routes {
    const val HOME = "home"
    const val SCANNING = "scanning"
    const val CONFIRM = "confirm"
    const val SUCCESS = "success"
}

@Composable
fun AppRoot(
    viewModel: ScanViewModel,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Drive navigation from the single source of truth: the scan phase.
    PhaseNavigator(phase = state.phase, navController = navController)

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                errorRes = state.errorRes,
                onErrorShown = viewModel::errorShown,
                onPickImage = onPickImage,
                onTakePhoto = onTakePhoto,
            )
        }
        composable(Routes.SCANNING) {
            ScanningScreen(imageUri = state.imageUri)
        }
        composable(Routes.CONFIRM) {
            val draft = state.draft
            if (draft != null) {
                ConfirmScreen(
                    draft = draft,
                    errorRes = state.errorRes,
                    calendarRepository = viewModel.calendarRepository(),
                    onErrorShown = viewModel::errorShown,
                    onDraftChange = viewModel::updateDraft,
                    onSave = viewModel::save,
                    onCancel = viewModel::reset,
                    onRescan = viewModel::reset,
                )
            }
        }
        composable(Routes.SUCCESS) {
            SuccessScreen(
                title = state.savedTitle,
                eventId = state.savedEventId,
                calendarRepository = viewModel.calendarRepository(),
                onScanAnother = viewModel::reset,
                onDone = viewModel::reset,
            )
        }
    }
}
