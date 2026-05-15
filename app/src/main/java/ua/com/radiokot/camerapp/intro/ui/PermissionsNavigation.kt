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

package ua.com.radiokot.camerapp.intro.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.permissionsDestination(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) = composable(
    route = PermissionsRoute,
) {
    val viewModel: PermissionsScreenViewModel = koinViewModel()

    PermissionsScreen(
        permissions = viewModel.requiredPermissions,
        isDocumentTreeAccessRequired = viewModel.isDocumentTreeAccessRequired,
        onGrantAction = viewModel::onGrantAction,
        modifier = modifier
            .fillMaxSize()
    )

    val context = LocalContext.current
    val documentTreeAccessRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { resultUri ->
            if (resultUri == viewModel.requiredDocumentTreeAccessUri) {
                viewModel.onDocumentTreeAccessGranted()
            } else if (resultUri != null) {
                println("OOLEG re $resultUri")
                Toast.makeText(
                    context,
                    "Sorry, but the permission is required for the exact directory",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Sorry, but the app can't work reliably without this permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        },
    )
    val permissionsRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsResult ->
            if (permissionsResult.all(Map.Entry<String, Boolean>::value)) {
                viewModel.onRequestedPermissionsGranted()
            } else {
                Toast.makeText(
                    context,
                    "Sorry, but the app can't work without these permissions",
                    Toast.LENGTH_LONG
                ).show()
            }
        },
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PermissionsScreenViewModel.Event.RequestRequiredPermissions -> {
                    permissionsRequestLauncher.launch(
                        viewModel.requiredPermissions.toTypedArray()
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
            }
        }
    }
}

const val PermissionsRoute = "permissions"
