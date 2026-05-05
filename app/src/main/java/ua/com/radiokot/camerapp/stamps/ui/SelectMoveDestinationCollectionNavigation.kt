@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.selectMoveDestinationCollectionDestination(
    onSelected: (collectionId: String) -> Unit,
    onCancel: () -> Unit,
) = dialog(
    route = SelectMoveDestinationCollectionRoute,
    arguments = listOf(
        navArgument(SourceCollectionId) {
            type = NavType.StringType
        },
    ),
) { navEntry ->
    val viewModel: SelectMoveDestinationCollectionDialogViewModel = koinViewModel {
        parametersOf(
            SelectMoveDestinationCollectionDialogViewModel.Parameters(
                sourceCollectionId =
                    navEntry
                        .arguments
                        ?.getString(SourceCollectionId)
                        ?: error("No $SourceCollectionId argument passed"),
            )
        )
    }

    SelectMoveDestinationCollectionDialog(
        collections = viewModel.collections,
        onCollectionSelected = viewModel::onCollectionSelected,
        onNewCollectionAction = viewModel::onNewCollectionAction,
        onCancel = onCancel,
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SelectMoveDestinationCollectionDialogViewModel.Event.CollectionSelected -> {
                    onSelected(event.collectionId)
                }
            }
        }
    }
}

private const val SourceCollectionId = "sourceCollectionId"

const val SelectMoveDestinationCollectionRoute =
    "selectMoveDestinationCollection?from={$SourceCollectionId}"

fun SelectMoveDestinationCollectionDestinationRoute(
    sourceCollectionId: String?,
) =
    "selectMoveDestinationCollection?from=$sourceCollectionId"
