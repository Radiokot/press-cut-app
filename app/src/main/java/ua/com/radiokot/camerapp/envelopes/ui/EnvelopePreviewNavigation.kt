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

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionContract
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionRequest

fun NavGraphBuilder.envelopePreviewDestination(
    selectDestinationCollectionContract: SelectDestinationCollectionContract,
    onProceedToSaveStamps: (collectionId: String) -> Unit,
) = composable(
    route = EnvelopePreviewRoute,
    arguments = listOf(
        navArgument(PackageContentUri) {
            type = NavType.StringType
        },
    )
) { navEntry ->
    val viewModel: EnvelopePreviewViewModel = koinViewModel {
        parametersOf(
            EnvelopePreviewViewModel.Parameters(
                oneStampPackageContentUri =
                    navEntry
                        .arguments
                        ?.getString(PackageContentUri)
                        ?.toUri()
                        ?: error("No $PackageContentUri argument passed"),
            )
        )
    }

    EnvelopePreviewScreen(
        someStamps = viewModel.someStamps,
        message = viewModel.message,
        stampCount = viewModel.stampCount,
        onSaveAction = viewModel::onSaveAction,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EnvelopePreviewViewModel.Event.ProceedToSaveDestinationCollectionSelection -> {
                    selectDestinationCollectionContract.proceedToCollectionSelection(
                        request = SelectDestinationCollectionRequest.SaveStamps,
                    )
                }

                is EnvelopePreviewViewModel.Event.ProceedToSaveStamps -> {
                    onProceedToSaveStamps(event.collectionId)
                }
            }
        }
    }

    LaunchedEffect(selectDestinationCollectionContract) {
        selectDestinationCollectionContract
            .getSelectedCollectionIdFlow()
            .collect(viewModel::onSaveDestinationCollectionSelected)
    }
}

private const val PackageContentUri = "packageContentUri"

const val EnvelopePreviewRoute = "envelopePreview?packageContentUri={$PackageContentUri}"

fun EnvelopePreviewRoute(
    oneStampPackageContentUri: Uri,
) =
    "envelopePreview?packageContentUri=${Uri.encode(oneStampPackageContentUri.toString())}"
