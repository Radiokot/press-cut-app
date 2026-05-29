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

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ua.com.radiokot.camerapp.util.ShareSheetShareEventReceiver

fun NavGraphBuilder.sendEnvelopeDestination(
    onDone: () -> Unit,
) = composable(
    route = SendEnvelopeRoute,
    arguments = listOf(
        navArgument(StampSelectionIndex) {
            type = NavType.IntType
        }
    ),
    enterTransition = {
        fadeIn() + slideInVertically(
            initialOffsetY = { height -> height / 2 },
        )
    },
    exitTransition = { fadeOut() },
) { navEntry ->

    val viewModel: SendEnvelopeScreenViewModel = koinViewModel {
        parametersOf(
            SendEnvelopeScreenViewModel.Parameters(
                stampSelectionIndex =
                    navEntry
                        .arguments
                        ?.getInt(StampSelectionIndex)
                        ?: error("No $StampSelectionIndex argument passed"),
            )
        )
    }

    SendEnvelopeScreen(
        messageInputState = viewModel.messageInput,
        someStamps = viewModel.someStamps,
        stampCount = viewModel.stampCount,
        onSendAction = viewModel::onSendAction,
        modifier = Modifier
            .fillMaxSize()
    )

    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SendEnvelopeScreenViewModel.Event.ProceedToSendIntent -> {
                    val shareSheetIntentSender =
                        ShareSheetShareEventReceiver
                            .newIntentSender(
                                context = context,
                            )

                    context.startActivity(
                        Intent.createChooser(
                            event.intent,
                            "Send stamps",
                            shareSheetIntentSender,
                        )
                    )
                }

                SendEnvelopeScreenViewModel.Event.Done -> {
                    onDone()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        ShareSheetShareEventReceiver
            .shareEvents
            .collect {
                viewModel.onSent()
            }
    }
}

private const val StampSelectionIndex = "stampSelectionIndex"

const val SendEnvelopeRoute = "sendEnvelope?stampSelectionIndex={$StampSelectionIndex}"

fun SendEnvelopeRoute(
    stampSelectionIndex: Int,
) =
    "sendEnvelope?stampSelectionIndex=$stampSelectionIndex"
