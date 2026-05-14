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

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        SelectMoveDestinationCollectionContract(navEntry)
            .getSelectedCollectionIdFlow()
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
