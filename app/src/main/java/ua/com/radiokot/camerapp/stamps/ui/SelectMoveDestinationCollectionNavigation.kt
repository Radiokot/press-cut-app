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
        navArgument(IsSingleStamp) {
            type = NavType.BoolType
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
        isSingleStamp =
            navEntry
                .arguments
                ?.getBoolean(IsSingleStamp)
                ?: false,
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
private const val IsSingleStamp = "isSingleStamp"

const val SelectMoveDestinationCollectionRoute =
    "selectMoveDestinationCollection?from={$SourceCollectionId}&isSingle={$IsSingleStamp}"

fun SelectMoveDestinationCollectionDestinationRoute(
    sourceCollectionId: String?,
    isSingleStamp: Boolean,
) =
    "selectMoveDestinationCollection?from=$sourceCollectionId&isSingle=$isSingleStamp"
