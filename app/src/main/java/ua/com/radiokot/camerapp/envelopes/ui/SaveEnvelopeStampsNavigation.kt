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

package ua.com.radiokot.camerapp.envelopes.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.envelopes.domain.OneStampEnvelopePreviewResult

fun NavGraphBuilder.saveEnvelopeStampsDestination(
    envelopePreviewState: State<OneStampEnvelopePreviewResult.Preview?>,
    onDone: () -> Unit,
) = composable(
    route = SaveEnvelopeStampsRoute,
    arguments = listOf(
        navArgument(DestinationCollectionId) {
            type = NavType.StringType
        },
    )
) { navEntry ->

    val viewModel: SaveEnvelopeStampsScreenViewModel = koinViewModel {
        parametersOf(
            SaveEnvelopeStampsScreenViewModel.Parameters(
                destinationCollectionId =
                    navEntry
                        .arguments
                        ?.getString(DestinationCollectionId)
                        ?: error("No $DestinationCollectionId argument passed"),
                envelopePreview =
                    envelopePreviewState
                        .value
                        ?: error("Preview must be available at this moment")
            )
        )
    }

    SaveEnvelopeStampsScreen(
        progressState = viewModel.progress.collectAsState().asFloatState(),
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SaveEnvelopeStampsScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }
}

private const val DestinationCollectionId = "DestinationCollectionId"
private const val EnvelopeContentUri = "EnvelopeContentUri"

const val SaveEnvelopeStampsRoute =
    "saveEnvelopeStamps?destinationCollectionId={$DestinationCollectionId}"

fun SaveEnvelopeStampsRoute(
    destinationCollectionId: String,
): String =
    "saveEnvelopeStamps?destinationCollectionId=$destinationCollectionId"
