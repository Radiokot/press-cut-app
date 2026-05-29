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

package ua.com.radiokot.camerapp.envelopes.ui

import android.content.Intent
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.envelopes.domain.CreateSendEnvelopeIntentUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.domain.StampSelections
import ua.com.radiokot.camerapp.stamps.ui.StampSampleItem
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class SendEnvelopeScreenViewModel(
    private val stampRepository: StampRepository,
    private val createSendEnvelopeIntentUseCase: CreateSendEnvelopeIntentUseCase,
    parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("SendEnvelopeScreenVM")

    private val stampIds = StampSelections[parameters.stampSelectionIndex]

    val messageInput: TextFieldState = TextFieldState(initialText = "")
    val stampCount = stampIds.size
    val someStamps: ImmutableList<StampSampleItem> = runBlocking {
        stampRepository
            .getStamps()
            .filter { it.id in stampIds }
            .take(3)
            .map(::StampSampleItem)
            .toPersistentList()
    }

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    fun onSendAction() {
        val message =
            messageInput
                .text
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)

        log.debug {
            "onSendAction(): creating an intent:" +
                    "\nmessage=$message" +
                    "\nstampIds=${stampIds.size}"
        }

        val intent = createSendEnvelopeIntentUseCase(
            message = message,
            stampIds = stampIds,
        )

        log.debug {
            "onSendAction(): proceeding to send intent:" +
                    "\nintent=$intent"
        }

        events.tryEmit(Event.ProceedToSendIntent(intent))
    }

    fun onSent() {
        log.debug {
            "onSent(): done"
        }

        events.tryEmit(Event.Done)
    }

    sealed interface Event {

        /**
         * Call [onSent] on success.
         */
        class ProceedToSendIntent(
            val intent: Intent,
        ) : Event

        object Done : Event
    }

    data class Parameters(
        val stampSelectionIndex: Int,
    )
}
