@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * @param onProceedToMoveDestinationCollectionSelection set [SelectedMoveDestinationCollectionId] in the state handle when selected
 */
fun NavGraphBuilder.collectionActionsDestination(
    sharedTransitionScope: SharedTransitionScope?,
    onProceedToMoveDestinationCollectionSelection: (currentCollectionId: String) -> Unit,
    onProceedToMoveStamps: (sourceCollectionId: String, destinationCollectionId: String) -> Unit,
    onDone: () -> Unit,
) = composable(
    route = CollectionActionsRoute,
    arguments = listOf(
        navArgument(CollectionId) {
            type = NavType.StringType
        },
    ),
) { navEntry ->
    val viewModel: CollectionActionsScreenViewModel = koinViewModel {
        parametersOf(
            CollectionActionsScreenViewModel.Parameters(
                collectionId =
                    navEntry
                        .arguments
                        ?.getString(CollectionId)
                        ?: error("No $CollectionId argument passed"),
            )
        )
    }

    CollectionActionsScreen(
        collection = viewModel.collectionItem,
        canDelete = viewModel.canDelete,
        onMoveStampsAction = viewModel::onMoveStampsAction,
        onDeleteAction = viewModel::onDeleteAction,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionActionsScreenViewModel.Event.ProceedToMoveDestinationCollectionSelection -> {
                    onProceedToMoveDestinationCollectionSelection(
                        event.currentCollectionId,
                    )
                }

                is CollectionActionsScreenViewModel.Event.ProceedToMoveStamps -> {
                    onProceedToMoveStamps(
                        event.sourceCollectionId,
                        event.destinationCollectionId
                    )
                }

                is CollectionActionsScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }

    LaunchedEffect(viewModel,navEntry) {
        navEntry
            .savedStateHandle
            .getStateFlow(
                key = SelectedMoveDestinationCollectionId,
                initialValue = null,
            )
            .filterNotNull()
            .distinctUntilChanged()
            .collect(viewModel::onMoveDestinationCollectionSelected)
    }
}

private const val CollectionId = "collectionId"

const val CollectionActionsRoute =
    "collectionActions/{$CollectionId}"

fun CollectionActionsRoute(
    collectionId: String,
) =
    "collectionActions/$collectionId"
