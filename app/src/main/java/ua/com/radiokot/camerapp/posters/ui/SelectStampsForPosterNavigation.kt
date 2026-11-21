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

package ua.com.radiokot.camerapp.posters.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.ui.showToast
import ua.com.radiokot.camerapp.ui.DarkAppColors
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.util.DarkStatusAndNavigationBars

fun NavGraphBuilder.selectStampsForPosterDestination(
    onDone: (selectionIndex: Int) -> Unit,
) = dialog(
    route = SelectStampsForPosterRoute,
    arguments = listOf(
        navArgument(MaxCount) {
            type = NavType.IntType
        }
    ),
    dialogProperties = DialogProperties(
        dismissOnClickOutside = false,
        dismissOnBackPress = true,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    )
) { navEntry ->

    DarkStatusAndNavigationBars()

    val viewModel: SelectStampsForPosterDialogViewModel = koinViewModel {
        parametersOf(
            SelectStampsForPosterDialogViewModel.Parameters(
                maxCount =
                    navEntry
                        .arguments
                        ?.getInt(MaxCount)
                        ?: error("No $MaxCount argument passed")
            )
        )
    }

    val context = LocalContext.current
    val colors = DarkAppColors

    CompositionLocalProvider(
        LocalColors provides colors
    ) {
        SelectStampsForPosterDialog(
            stamps = viewModel.items.collectAsState(),
            selectedStampKeys = viewModel.selectedItemKeys.collectAsState(),
            onStampClicked = viewModel::onStampClicked,
            onAddSelectedAction = viewModel::onAddSelectedAction,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SelectStampsForPosterDialogViewModel.Event.Done -> {
                    onDone(event.selectionIndex)
                }

                is SelectStampsForPosterDialogViewModel.Event.ShowTooManyStampsWarning -> {
                    showToast(
                        context = context,
                        text = "That's too many for this poster",
                        colors = colors,
                        durationMs = 1500,
                    )
                }
            }
        }
    }
}

private const val MaxCount = "maxCount"

const val SelectStampsForPosterRoute = "selectStampsForPoster?maxCount={$MaxCount}"

fun SelectStampsForPosterRoute(
    maxCount: Int,
) =
    "selectStampsForPoster?maxCount=$maxCount"
