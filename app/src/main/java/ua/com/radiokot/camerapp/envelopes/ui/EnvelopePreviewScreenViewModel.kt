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

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.envelopes.domain.GetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.stamps.ui.StampSampleItem
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class EnvelopePreviewScreenViewModel(
    getOneStampEnvelopePreviewUseCase: GetOneStampEnvelopePreviewUseCase,
    private val parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("EnvelopePreviewScreenVM")

    val events: SharedFlow<Event>
        field = eventSharedFlow<Event>()

    private val envelopePreview = runBlocking {
        val oneStampEnvelopeContentUri = parameters.oneStampEnvelopeContentUri

        log.debug {
            "envelopePreview: getting the preview:" +
                    "\noneStampEnvelopeContentUri=$oneStampEnvelopeContentUri"
        }

        getOneStampEnvelopePreviewUseCase(
            oneStampEnvelopeContentUri = oneStampEnvelopeContentUri,
        )
    }

    val message: String? =
        envelopePreview.message

    val stampCount: Int =
        envelopePreview.stampCount

    val someStamps: ImmutableList<StampSampleItem> =
        envelopePreview
            .someStamps
            .map(::StampSampleItem)
            .toPersistentList()

    fun onSaveAction() {
        log.debug {
            "onSaveAction(): proceeding to destination collection selection"
        }

        events.tryEmit(Event.ProceedToSaveDestinationCollectionSelection)
    }

    fun onSaveDestinationCollectionSelected(
        collectionId: String,
    ) {
        log.debug {
            "onSaveDestinationCollectionSelected(): proceeding to save the stamps:" +
                    "\ncollectionId=$collectionId"
        }

        events.tryEmit(
            Event.ProceedToSaveStamps(
                collectionId = collectionId,
            )
        )
    }

    sealed interface Event {
        object ProceedToSaveDestinationCollectionSelection : Event

        class ProceedToSaveStamps(
            val collectionId: String,
        ) : Event
    }

    class Parameters(
        val oneStampEnvelopeContentUri: Uri,
    )
}
