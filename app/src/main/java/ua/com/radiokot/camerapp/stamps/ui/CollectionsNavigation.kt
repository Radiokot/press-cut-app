package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.collectionsDestination(
    sharedTransitionScope: SharedTransitionScope?,
    onProceedToCollection: (
        collectionId: String,
        focusNameInput: Boolean,
    ) -> Unit,
    onProceedToCollectionActions: (collectionId: String) -> Unit,
    onProceedToNewStamp: () -> Unit,
) = composable(
    route = CollectionsRoute,
) {
    val viewModel: CollectionsScreenViewModel = koinViewModel()
    val items = viewModel.items.collectAsState()

    CollectionsScreen(
        itemsState = items,
        onItemClicked = viewModel::onItemClicked,
        onItemLongClicked = viewModel::onItemLongClicked,
        onNewStampAction = viewModel::onNewStampAction,
        onNewCollectionAction = viewModel::onNewCollectionAction,
        onMoreClicked = viewModel::onMoreClicked,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionsScreenViewModel.Event.ProceedToCollection -> {
                    onProceedToCollection(
                        event.collectionId,
                        event.focusNameInput,
                    )
                }

                is CollectionsScreenViewModel.Event.ProceedToCollectionActions -> {
                    onProceedToCollectionActions(
                        event.collectionId,
                    )
                }

                is CollectionsScreenViewModel.Event.ProceedToNewStamp -> {
                    onProceedToNewStamp()
                }
            }
        }
    }
}

const val CollectionsRoute = "collections"
