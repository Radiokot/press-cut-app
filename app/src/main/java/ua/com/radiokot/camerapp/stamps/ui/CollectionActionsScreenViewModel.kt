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

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.stamps.domain.GetStampCollectionsWithSamplesUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionWithSamples
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class CollectionActionsScreenViewModel(
    private val collectionRepository: StampCollectionRepository,
    getStampCollectionsWithSamplesUseCase: GetStampCollectionsWithSamplesUseCase,
    parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("CollectionActionsScreenVM")

    private val collectionWithSamples: StampCollectionWithSamples = runBlocking {
        getStampCollectionsWithSamplesUseCase
            .invoke(
                singleCollectionId = parameters.collectionId,
            )
            ?: error("Collection ${parameters.collectionId} not found")
    }
    private val collection: StampCollection =
        collectionWithSamples.collection

    val collectionItem: CollectionListItem =
        CollectionListItem(collectionWithSamples)

    val canDelete: Boolean =
        !collection.isPrimary

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    private var deleteJob: Job? = null

    fun onDeleteAction() {
        check(canDelete) {
            "Can't delete collection when it's not allowed"
        }

        if (deleteJob?.isActive == true) {
            return
        }

        deleteJob = viewModelScope.launch {
            deleteCollection()
        }
    }

    private suspend fun deleteCollection() {
        log.debug {
            "onDeleteAction(): deleting:" +
                    "\ncollection=$collection"
        }

        collectionRepository.deleteStampCollection(
            collection = collection,
        )

        log.info {
            "Collection ${collection.id} deleted"
        }

        events.emit(Event.Done)
    }

    fun onMoveStampsAction() {
        log.debug {
            "onMoveStampsAction(): proceeding to destination collection selection"
        }

        events.tryEmit(
            Event.ProceedToMoveDestinationCollectionSelection(
                currentCollectionId = collection.id,
            )
        )
    }

    fun onMoveDestinationCollectionSelected(
        destinationCollectionId: String,
    ) {
        log.debug {
            "onMoveDestinationCollectionSelected(): proceeding to the move:" +
                    "\ndestinationCollectionId=$destinationCollectionId"
        }

        events.tryEmit(
            Event.ProceedToMoveStamps(
                sourceCollectionId = collection.id,
                destinationCollectionId = destinationCollectionId,
            )
        )
    }

    sealed interface Event {
        class ProceedToMoveDestinationCollectionSelection(
            val currentCollectionId: String,
        ) : Event

        class ProceedToMoveStamps(
            val sourceCollectionId: String,
            val destinationCollectionId: String,
        ) : Event

        object Done : Event
    }

    data class Parameters(
        val collectionId: String,
    )
}
