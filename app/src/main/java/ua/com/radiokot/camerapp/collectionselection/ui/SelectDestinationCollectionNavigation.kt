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

package ua.com.radiokot.camerapp.collectionselection.ui

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.selectDestinationCollectionDestination(
    contract: SelectDestinationCollectionContract,
) = dialog(
    route = SelectDestinationCollectionRoute,
    arguments = listOf(
        navArgument(RequestJson) {
            type = NavType.StringType
        }
    )
) { navEntry ->

    val viewModel: SelectDestinationCollectionDialogViewModel = koinViewModel {
        parametersOf(
            SelectDestinationCollectionDialogViewModel.Parameters(
                request =
                    navEntry
                        .arguments
                        ?.getString(RequestJson)
                        ?.let(Uri::decode)
                        ?.let(Json::decodeFromString)
                        ?: error("No $RequestJson argument passed")
            )
        )
    }

    SelectDestinationCollectionDialog(
        message = viewModel.message,
        collections = viewModel.collections,
        onCollectionSelected = viewModel::onCollectionSelected,
        onNewCollectionAction = viewModel::onNewCollectionAction,
        onCancel = contract::onCancel,
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SelectDestinationCollectionDialogViewModel.Event.CollectionSelected -> {
                    contract.onCollectionSelected(event.collectionId)
                }
            }
        }
    }
}

private const val RequestJson = "requestJson"

const val SelectDestinationCollectionRoute = "selectDestCollection?request={$RequestJson}"

fun SelectDestinationCollectionRoute(
    request: SelectDestinationCollectionRequest,
) =
    "selectDestCollection?request=${Uri.encode(Json.encodeToString(request))}"
