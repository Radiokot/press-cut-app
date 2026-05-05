@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.stampsDestination(
    sharedTransitionScope: SharedTransitionScope?,
    onProceedToStamp: (stampId: String) -> Unit,
    onProceedToNewStamp: (collectionId: String) -> Unit,
    onDone: () -> Unit,
) = composable(
    route = StampsRoute,
    arguments = listOf(
        navArgument(CollectionId) {
            type = NavType.StringType
        },
        navArgument(FocusNameInput) {
            type = NavType.BoolType
        }
    ),
) { navEntry ->
    val viewModel: StampsScreenViewModel = koinViewModel {
        parametersOf(
            StampsScreenViewModel.Parameters(
                collectionId =
                    navEntry
                        .arguments
                        ?.getString(CollectionId)
                        ?: error("No $CollectionId argument passed"),
            )
        )
    }
    val stamps = viewModel.stamps.collectAsState()
    val focusCollectionNameInput =
        navEntry
            .arguments
            ?.getBoolean(FocusNameInput)
            ?: false

    StampsScreen(
        collectionId = viewModel.collectionId,
        collectionNameInputState = viewModel.collectionNameInput,
        focusCollectionNameInput = focusCollectionNameInput,
        stamps = stamps,
        onStampClicked = viewModel::onStampClicked,
        onNewStampAction = viewModel::onNewStampAction,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is StampsScreenViewModel.Event.ProceedToStamp -> {
                    onProceedToStamp(
                        event.stampId,
                    )
                }

                is StampsScreenViewModel.Event.ProceedToNewStamp -> {
                    onProceedToNewStamp(
                        event.collectionId,
                    )
                }

                is StampsScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }

    BackHandler(
        onBack = viewModel::onBackAction,
    )
}

private const val CollectionId = "collectionId"
private const val FocusNameInput = "focusNameInput"

const val StampsRoute = "stamps?collectionId={$CollectionId}&focusNameInput={$FocusNameInput}"

fun StampsRoute(
    collectionId: String,
    focusNameInput: Boolean = false,
) =
    "stamps?collectionId=$collectionId&focusNameInput=$focusNameInput"
