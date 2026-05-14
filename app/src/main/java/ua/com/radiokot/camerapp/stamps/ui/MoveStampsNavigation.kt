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
