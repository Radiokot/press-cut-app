/* Copyright 2026 Oleg Koretsky

   This file is part of the Press-Cut,
   a digital postage stamp cutter Android app.

   Press-Cut is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   Press-Cut is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Press-Cut. If not, see <http://www.gnu.org/licenses/>.
*/

@file:Suppress("FunctionName")

package ua.com.radiokot.camerapp.stamps.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionContract
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionRequest

fun NavGraphBuilder.stampsDestination(
    sharedTransitionScope: SharedTransitionScope?,
    selectDestinationCollectionContract: SelectDestinationCollectionContract,
    onProceedToStamp: (stampId: String) -> Unit,
    onProceedToNewStamp: (collectionId: String) -> Unit,
    onProceedToMoveStamps: (
        sourceCollectionId: String,
        destinationCollectionId: String,
        stampSelectionIndex: Int,
    ) -> Unit,
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
    val stamps = viewModel.items.collectAsState()
    val focusCollectionNameInput =
        navEntry
            .arguments
            ?.getBoolean(FocusNameInput)
            ?: false

    StampsScreen(
        collectionId = viewModel.collectionId,
        collectionNameInputState = viewModel.collectionNameInput,
        focusCollectionNameInput = focusCollectionNameInput,
        showGiftMessage = viewModel.showGiftMessage,
        stamps = stamps,
        onStampClicked = viewModel::onStampClicked,
        onStampLongClicked = viewModel::onStampLongClicked,
        selectedCountState =
            viewModel
                .selectedStampCount
                .collectAsState()
                .asIntState(),
        onMoveSelectedAction = viewModel::onMoveSelectedAction,
        onShareSelectedAction = viewModel::onShareSelectedAction,
        onDeleteSelectedAction = viewModel::onDeleteSelectedAction,
        onNewStampAction = viewModel::onNewStampAction,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = this@composable,
        modifier = Modifier
            .fillMaxSize()
    )

    val context = LocalContext.current

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

                is StampsScreenViewModel.Event.ProceedToMoveDestinationCollectionSelection -> {
                    selectDestinationCollectionContract.proceedToCollectionSelection(
                        request = SelectDestinationCollectionRequest.MoveStamps(
                            currentCollectionId = event.currentCollectionId,
                            isSingleStamp = event.isSingleStamp,
                        )
                    )
                }

                is StampsScreenViewModel.Event.ProceedToMoveStamps -> {
                    onProceedToMoveStamps(
                        event.sourceCollectionId,
                        event.destinationCollectionId,
                        event.stampSelectionIndex,
                    )
                }

                is StampsScreenViewModel.Event.ProceedToEnvelopeShare -> {
                    context.startActivity(
                        Intent.createChooser(
                            event.intent,
                            "Stamps",
                        )
                    )
                }

                is StampsScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }

    LaunchedEffect(viewModel, selectDestinationCollectionContract) {
        selectDestinationCollectionContract
            .getSelectedCollectionIdFlow()
            .collect(viewModel::onMoveDestinationCollectionSelected)
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
