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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.envelopes.domain.GetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.envelopes.domain.OneStampEnvelopePreviewResult
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

    private var preview: OneStampEnvelopePreviewResult.Preview?
    var errorMessage: String?
        private set

    init {
        runBlocking {
            val oneStampEnvelopeContentUri = parameters.oneStampEnvelopeContentUri

            log.debug {
                "init: getting the preview:" +
                        "\noneStampEnvelopeContentUri=$oneStampEnvelopeContentUri"
            }

            try {
                val result =
                    getOneStampEnvelopePreviewUseCase(
                        oneStampEnvelopeContentUri = oneStampEnvelopeContentUri,
                        maxPreviewStampCount = 3,
                    )

                preview = result as? OneStampEnvelopePreviewResult.Preview
                errorMessage = when (val error = result as? OneStampEnvelopePreviewResult.Error) {
                    null -> null
                    is OneStampEnvelopePreviewResult.Error.Malformed ->
                        "…yet, the envelope is malformed (${error.reason})"

                    OneStampEnvelopePreviewResult.Error.NoSupportedStamps ->
                        "…yet, none of the stamps are supported"
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                log.error(e) {
                    "init: failed getting the preview:" +
                            "\noneStampEnvelopeContentUri=$oneStampEnvelopeContentUri"
                }

                preview = null
                errorMessage = "…yet, the app failed to read the envelope"
            }
        }
    }

    val message: String? =
        preview?.message

    val stampCount: Int =
        preview?.allStamps?.size ?: 0

    val someStamps: ImmutableList<StampSampleItem> =
        preview
            ?.previewStamps
            ?.map(::StampSampleItem)
            ?.toPersistentList()
            ?: persistentListOf()

    fun onSaveAction() {
        checkNotNull(preview) {
            "Can't save when there's no preview"
        }

        log.debug {
            "onSaveAction(): proceeding to destination collection selection"
        }

        events.tryEmit(Event.ProceedToSaveDestinationCollectionSelection)
    }

    fun onSaveDestinationCollectionSelected(
        collectionId: String,
    ) {
        val preview = checkNotNull(preview) {
            "Can't save when there's no preview"
        }

        log.debug {
            "onSaveDestinationCollectionSelected(): proceeding to save the stamps:" +
                    "\ndestinationCollectionId=$collectionId"
        }

        events.tryEmit(
            Event.ProceedToSaveStamps(
                destinationCollectionId = collectionId,
                envelopePreview = preview,
            )
        )
    }

    sealed interface Event {
        object ProceedToSaveDestinationCollectionSelection : Event

        class ProceedToSaveStamps(
            val destinationCollectionId: String,
            val envelopePreview: OneStampEnvelopePreviewResult.Preview,
        ) : Event
    }

    class Parameters(
        val oneStampEnvelopeContentUri: Uri,
    )
}
