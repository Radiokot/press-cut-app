@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.stampDestination(
    sharedTransitionScope: SharedTransitionScope?,
    onDone: () -> Unit,
) = composable(
    route = StampRoute,
    arguments = listOf(
        navArgument(StampId) {
            type = NavType.StringType
        },
    ),
) { navEntry ->
    val viewModel: StampScreenViewModel = koinViewModel {
        parametersOf(
            StampScreenViewModel.Parameters(
                stampId =
                    navEntry
                        .arguments
                        ?.getString(StampId)
                        ?: error("No $StampId argument passed"),
            )
        )
    }
    val isCaptionInputEnabled by viewModel.isCaptionInputEnabled.collectAsState()

    StampScreen(
        stampId = viewModel.stampId,
        isEditable = viewModel.isEditable,
        captionState = viewModel.caption,
        isCaptionInputEnabled = isCaptionInputEnabled,
        onAddCaptionAction = viewModel::onAddCaptionAction,
        onDeleteAction = viewModel::onDeleteAction,
        imageUri = viewModel.imageUri,
        takenAt = viewModel.takenAt,
        onSwipedToExit = onDone,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is StampScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }
}

private const val StampId = "stampId"

const val StampRoute = "stamp/{$StampId}"

fun StampRoute(
    stampId: String,
) =
    "stamp/$stampId"
