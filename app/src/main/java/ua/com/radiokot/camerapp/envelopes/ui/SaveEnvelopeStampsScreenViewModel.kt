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

@file:OptIn(FlowPreview::class)

package ua.com.radiokot.camerapp.envelopes.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.camerapp.envelopes.domain.AddStampsFromOneStampEnvelopeUseCase
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class SaveEnvelopeStampsScreenViewModel(
    addStampsFromOneStampEnvelopeUseCase: AddStampsFromOneStampEnvelopeUseCase,
    private val parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("SaveEnvelopeStampsScreenVM")

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    private val addProgressFlow =
        addStampsFromOneStampEnvelopeUseCase(
            collectionId = parameters.destinationCollectionId,
            oneStampEnvelopeContentUri = parameters.oneStampEnvelopeContentUri,
        )

    val progress: StateFlow<Float> =
        addProgressFlow
            .sample(100.milliseconds)
            .map { (addedStampCount, totalStampCount) ->
                addedStampCount.toFloat() / totalStampCount
            }
            .onCompletion { error ->
                if (error != null) {
                    if (error !is CancellationException) {
                        log.error(error) {
                            "process: failed adding:" +
                                    "\ndestinationCollectionId=${parameters.destinationCollectionId}" +
                                    "\noneStampEnvelopeContentUri=${parameters.oneStampEnvelopeContentUri}"
                        }
                    }
                    return@onCompletion
                }

                emit(1f)
                delay(800)
                events.emit(Event.Done)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    sealed interface Event {
        object Done : Event
    }

    class Parameters(
        val oneStampEnvelopeContentUri: Uri,
        val destinationCollectionId: String,
    )
}
