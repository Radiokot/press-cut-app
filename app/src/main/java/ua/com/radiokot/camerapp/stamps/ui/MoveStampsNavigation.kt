@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.moveStampsDestination(
    onDone: () -> Unit,
) = composable(
    route = MoveStampsRoute,
    arguments = listOf(
        navArgument(SourceCollectionId) {
            type = NavType.StringType
        },
        navArgument(DestinationCollectionId) {
            type = NavType.StringType
        },
        navArgument(StampSelectionIndex) {
            type = NavType.IntType
        },
    ),
) { navEntry ->
    val viewModel: MoveStampsScreenViewModel = koinViewModel {
        parametersOf(
            MoveStampsScreenViewModel.Parameters(
                sourceCollectionId =
                    navEntry
                        .arguments
                        ?.getString(SourceCollectionId)
                        ?: error("No $SourceCollectionId argument passed"),
                destinationCollectionId =
                    navEntry
                        .arguments
                        ?.getString(DestinationCollectionId)
                        ?: error("No $DestinationCollectionId argument passed"),
                stampSelectionIndex =
                    navEntry
                        .arguments
                        ?.getInt(StampSelectionIndex, -1)
                        ?.takeUnless { it < 0 },
            )
        )
    }

    MoveStampsScreen(
        progressState = viewModel.progress.collectAsState().asFloatState(),
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MoveStampsScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }
}

private const val SourceCollectionId = "sourceCollectionId"
private const val DestinationCollectionId = "destinationCollectionId"
private const val StampSelectionIndex = "stampSelectionIndex"

const val MoveStampsRoute =
    "moveStamps?from={$SourceCollectionId}&to={$DestinationCollectionId}&selection={$StampSelectionIndex}"

fun MoveStampsRoute(
    sourceCollectionId: String,
    destinationCollectionId: String,
    stampSelectionIndex: Int = -1,
) =
    "moveStamps?from=$sourceCollectionId&to=$destinationCollectionId&selection=$stampSelectionIndex"
