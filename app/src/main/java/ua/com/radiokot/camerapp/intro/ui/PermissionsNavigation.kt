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

package ua.com.radiokot.camerapp.intro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.cut.ui.showToast
import ua.com.radiokot.camerapp.ui.LocalColors

fun NavGraphBuilder.permissionsDestination(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) = composable(
    route = PermissionsRoute,
    arguments = listOf(
        navArgument(IsCameraRequiredNow) {
            type = androidx.navigation.NavType.BoolType
        }
    ),
) { navEntry ->

    val viewModel: PermissionsScreenViewModel = koinViewModel {
        parametersOf(
            PermissionsScreenViewModel.Parameters(
                isCameraRequiredNow =
                    navEntry
                        .arguments
                        ?.getBoolean(IsCameraRequiredNow)
                        ?: error("No $IsCameraRequiredNow argument passed")
            )
        )
    }

    PermissionsScreen(
        permissions = viewModel.requestedPermissions,
        isDocumentTreeAccessRequired = viewModel.isDocumentTreeAccessRequired,
        onGrantAction = viewModel::onGrantAction,
        modifier = modifier
            .fillMaxSize()
    )

    val context = LocalContext.current
    val colors = LocalColors.current

    val documentTreeAccessRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onDocumentTreeAccessRequestResult,
    )
    val permissionsRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = viewModel::onPermissionRequestResult,
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PermissionsScreenViewModel.Event.RequestPermissions -> {
                    permissionsRequestLauncher.launch(
                        event.toRequest.toTypedArray()
                    )
                }

                is PermissionsScreenViewModel.Event.RequestDocumentTreeAccess -> {
                    documentTreeAccessRequestLauncher.launch(
                        event.initialUri
                    )
                }

                is PermissionsScreenViewModel.Event.Done -> {
                    onDone()
                }

                PermissionsScreenViewModel.Event.ShowDocumentTreeAccessRequiredForExactDirectoryExplanation -> {
                    showToast(
                        context = context,
                        text = "Sorry, but the permission is required for the exact directory",
                        durationMs = 3000,
                        colors = colors,
                    )
                }

                PermissionsScreenViewModel.Event.ShowRequiredDocumentTreeAccessExplanation -> {
                    showToast(
                        context = context,
                        text = "Sorry, but the app can't work reliably without this permission",
                        durationMs = 3000,
                        colors = colors,
                    )
                }

                PermissionsScreenViewModel.Event.ShowRequiredPermissionsExplanation -> {
                    showToast(
                        context = context,
                        text = "Sorry, but the app can't work without these permissions",
                        durationMs = 3000,
                        colors = colors,
                    )
                }
            }
        }
    }
}

private const val IsCameraRequiredNow = "isCameraRequiredNow"

const val PermissionsRoute = "permissions?isCameraRequiredNow={$IsCameraRequiredNow}"

fun PermissionsRoute(
    isCameraRequiredNow: Boolean,
) =
    "permissions?isCameraRequiredNow=$isCameraRequiredNow"
