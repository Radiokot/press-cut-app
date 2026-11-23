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

import android.content.Intent
import android.content.res.Configuration
import android.view.Gravity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.ui.showToast
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.util.NavResultContract

fun NavGraphBuilder.createPosterDestination(
    editStampPosterTextContract: NavResultContract<EditStampPosterTextRequest, EditStampPosterTextResult>,
    confirmDiscardChangesContract: NavResultContract<String, Boolean>,
    selectStampsForPosterContract: NavResultContract<Int, Int>,
    onDone: () -> Unit,
) = composable(
    route = CreatePosterRoute,
    arguments = listOf(
        navArgument(FirstStampId) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(StampSelectionIndex) {
            type = NavType.StringType
            nullable = true
        }
    ),
    enterTransition = {
        fadeIn() + slideInVertically(
            initialOffsetY = { height -> height / 2 },
        )
    },
    exitTransition = { fadeOut() },
) { navEntry ->

    val viewModel: CreateStampPosterScreenViewModel = koinViewModel {
        parametersOf(
            CreateStampPosterScreenViewModel.Parameters(
                firstStampId =
                    navEntry
                        .arguments
                        ?.getString(FirstStampId),
                stampSelectionIndex =
                    navEntry
                        .arguments
                        ?.getString(StampSelectionIndex)
                        ?.toInt(),
            )
        )
    }
    val isDiscardConfirmationRequired by viewModel.isDiscardConfirmationRequired.collectAsState()

    val context = LocalContext.current
    val colors = LocalColors.current
    val hapticFeedback = LocalHapticFeedback.current
    val orientation = LocalConfiguration.current.orientation

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateStampPosterScreenViewModel.Event.ProceedToSendIntent -> {
                    context.startActivity(
                        Intent.createChooser(
                            event.intent,
                            "Send a poster",
                        )
                    )
                }

                is CreateStampPosterScreenViewModel.Event.ProceedToEditText -> {
                    editStampPosterTextContract.launch(event.request)
                }

                is CreateStampPosterScreenViewModel.Event.ProceedToSelectStampsToAdd -> {
                    selectStampsForPosterContract.launch(event.maxCount)
                }

                is CreateStampPosterScreenViewModel.Event.ShowLayerDeletedMessage -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    showToast(
                        context = context,
                        text = "${event.layerName} removed",
                        colors = colors,
                        durationMs = 900,
                        gravity =
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                                Gravity.START or Gravity.CENTER
                            else
                                Gravity.CENTER,
                        offset =
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                                IntOffset(
                                    x = 200,
                                    y = 0,
                                )
                            else
                                IntOffset(
                                    x = 0,
                                    y = 200,
                                ),
                    )
                }
            }
        }
    }

    CreateStampPosterScreen(
        layersState = viewModel.layers.collectAsState(),
        onSendAction = viewModel::onSendAction,
        isDarkState = viewModel.isDark.collectAsState(),
        onBeginInteractionWithLayer = viewModel::onBeginInteractionWithLayer,
        onEndInteractionWithLayer = viewModel::onEndInteractionWithLayer,
        onLayerTap = viewModel::onLayerTap,
        onToggleIsDarkAction = viewModel::onToggleIsDarkAction,
        onAddTextAction = viewModel::onAddTextAction,
        canAddStamps = viewModel.canAddStamps.collectAsState().value,
        onAddStampsAction = viewModel::onAddStampsAction,
        modifier = Modifier
            .fillMaxSize()
    )

    LaunchedEffect(editStampPosterTextContract, viewModel, navEntry) {
        editStampPosterTextContract
            .getResultFlow(navEntry)
            .collect(viewModel::onDoneEditingText)
    }

    LaunchedEffect(selectStampsForPosterContract, viewModel, navEntry) {
        selectStampsForPosterContract
            .getResultFlow(navEntry)
            .collect(viewModel::onSelectedStampsToAdd)
    }

    BackHandler(
        enabled = isDiscardConfirmationRequired,
    ) {
        confirmDiscardChangesContract.launch(
            request = "Discard this poster?",
        )
    }

    LaunchedEffect(navEntry, onDone, confirmDiscardChangesContract) {
        confirmDiscardChangesContract
            .getResultFlow(navEntry)
            .collect { toDiscard ->
                if (toDiscard) {
                    onDone()
                }
            }
    }
}

private const val FirstStampId = "firstStampId"
private const val StampSelectionIndex = "stampSelectionIndex"

const val CreatePosterRoute = "createPoster?firstStampId={$FirstStampId}" +
        "&stampSelectionIndex={$StampSelectionIndex}"

fun CreatePosterRoute(
    firstStampId: String,
) =
    "createPoster?firstStampId=$firstStampId"

fun CreatePosterRoute(
    stampSelectionIndex: Int,
) =
    "createPoster?stampSelectionIndex=$stampSelectionIndex"
