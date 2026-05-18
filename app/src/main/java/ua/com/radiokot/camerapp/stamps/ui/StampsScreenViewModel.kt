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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.intro.domain.OnboardingPreferences
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.domain.StampSelections
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import ua.com.radiokot.camerapp.util.map
import kotlin.time.Duration.Companion.seconds

@Immutable
class StampsScreenViewModel(
    private val stampRepository: StampRepository,
    private val collectionRepository: StampCollectionRepository,
    private val onboardingPreferences: OnboardingPreferences,
    parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("StampsScreenVM")

    private val collection = runBlocking {
        collectionRepository.getStampCollection(parameters.collectionId)
            ?: error("Stamp collection ${parameters.collectionId} not found")
    }
    val collectionId: String =
        collection.id
    val collectionNameInput: TextFieldState =
        TextFieldState(initialText = collection.name)

    val showGiftMessage: Boolean =
        collection.isPrimary
                && onboardingPreferences.isPrimaryCollectionGiftStampsMessageRequired

    private val selectedStampIds: MutableStateFlow<PersistentSet<String>> =
        MutableStateFlow(persistentSetOf())
    private val isSelecting: Boolean
        get() = selectedStampIds.value.isNotEmpty()

    val selectedStampCount: StateFlow<Int> =
        selectedStampIds.map(viewModelScope, Set<*>::size)

    private val collectionStamps: StateFlow<List<Stamp>> = runBlocking {
        stampRepository
            .getStampsFlow()
            .map { stamps ->
                stamps
                    .filter { it.collectionId == collectionId }
                    .sortedByDescending(Stamp::takenAtLocal)
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope)
    }

    val items: StateFlow<ImmutableList<StampsScreenItem>> = runBlocking {
        combine(
            collectionStamps,
            selectedStampIds,
            ::Pair
        )
            .map { (collectionStamps, selectedStampIds) ->
                collectionStamps
                    .map { stamp ->
                        StampsScreenItem(
                            stamp = stamp,
                            selectedStampIds = selectedStampIds,
                        )
                    }
                    .toPersistentList()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope)
    }

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    init {
        if (showGiftMessage) {
            viewModelScope.launch {
                delay(3.seconds)
                onboardingPreferences.primaryCollectionGiftStampsMessageSeen()
            }
        }
    }

    fun onStampClicked(
        item: StampsScreenItem,
    ) {
        val stampId = item.key

        if (!isSelecting) {
            log.debug {
                "onStampClicked(): proceeding to the stamp:" +
                        "\nstampId=$stampId"
            }

            events.tryEmit(
                Event.ProceedToStamp(
                    stampId = stampId,
                )
            )
        } else {
            toggleStampSelection(stampId)
        }
    }

    fun onStampLongClicked(
        item: StampsScreenItem,
    ) {
        toggleStampSelection(
            stampId = item.key,
        )
    }

    private fun toggleStampSelection(
        stampId: String,
    ) {
        log.debug {
            "toggleSelection(): toggling selection of the stamp:" +
                    "\nstampId=$stampId"
        }

        selectedStampIds.update { selectedStampIds ->
            if (stampId in selectedStampIds)
                selectedStampIds.remove(stampId)
            else
                selectedStampIds.add(stampId)
        }
    }

    fun onMoveSelectedAction() {
        log.debug {
            "onMoveSelectedAction(): proceeding to destination collection selection"
        }

        events.tryEmit(
            Event.ProceedToMoveDestinationCollectionSelection(
                currentCollectionId = collectionId,
                isSingleStamp = selectedStampIds.value.size == 1,
            )
        )
    }

    fun onMoveDestinationCollectionSelected(
        destinationCollectionId: String,
    ) {
        val stampToMoveIds = selectedStampIds.value

        clearSelection()

        if (stampToMoveIds.isEmpty()) {
            return
        }

        if (stampToMoveIds.size <= 10) {
            log.debug {
                "onMoveDestinationCollectionSelected(): moving here and now:" +
                        "\ndestinationCollectionId=$destinationCollectionId" +
                        "\nstampToMoveIds=${stampToMoveIds.size}"
            }

            viewModelScope.launch {
                stampRepository
                    .moveStampsBetweenCollections(
                        sourceCollectionId = collectionId,
                        destinationCollectionId = destinationCollectionId,
                        stampIds = stampToMoveIds,
                    )
                    .collect()
            }
        } else {
            val selectionIndex = StampSelections + stampToMoveIds

            log.debug {
                "onMoveDestinationCollectionSelected(): proceeding to move with selection:" +
                        "\ndestinationCollectionId=$destinationCollectionId" +
                        "\nstampToMoveIds=${stampToMoveIds.size}" +
                        "\nselectionIndex=$selectionIndex"
            }

            events.tryEmit(
                Event.ProceedToMoveStamps(
                    sourceCollectionId = collectionId,
                    destinationCollectionId = destinationCollectionId,
                    stampSelectionIndex = selectionIndex,
                )
            )
        }
    }

    private var deleteJob: Job? = null

    fun onDeleteSelectedAction() {
        if (selectedStampIds.value.isEmpty() || deleteJob?.isActive == true) {
            return
        }

        viewModelScope.launch {
            deleteSelectedStamps()
        }
    }

    private suspend fun deleteSelectedStamps() {

        val stampToDeleteIds = selectedStampIds.value

        clearSelection()

        log.debug {
            "deleteStamps(): deleting:" +
                    "\nstampToDeleteIds=${stampToDeleteIds.size}" +
                    "\ncollectionId=$collectionId"
        }

        stampRepository.deleteStamps(
            collectionId = collectionId,
            stampIds = stampToDeleteIds,
        )

        log.info {
            "Deleted ${stampToDeleteIds.size} stamps from the collection $collectionId"
        }
    }

    fun onNewStampAction() {
        log.debug {
            "onNewStampAction(): proceeding to new stamp creation:" +
                    "\ncollectionId=$collectionId"
        }

        events.tryEmit(
            Event.ProceedToNewStamp(
                collectionId = collectionId,
            )
        )
    }

    private fun clearSelection() {
        selectedStampIds.update {
            persistentSetOf()
        }
    }

    fun onBackAction() {
        if (isSelecting) {
            log.debug {
                "onBackAction(): clearing selection first"
            }

            clearSelection()

            return
        }

        runBlocking {
            saveUpdates()
        }

        events.tryEmit(Event.Done)
    }

    private suspend fun saveUpdates() {
        val newName =
            collectionNameInput
                .text
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)

        if (newName != collection.name) {
            log.debug {
                "saveUpdates(): updating the collection:" +
                        "\nnewName=$newName"
            }

            collectionRepository.updateStampCollection(
                collection = collection,
                newName = newName,
            )

            log.info {
                "Successfully updated collection ${collection.id}"
            }
        } else {
            log.debug {
                "saveUpdates(): no updates"
            }
        }
    }

    sealed interface Event {
        class ProceedToStamp(
            val stampId: String,
        ) : Event

        class ProceedToNewStamp(
            val collectionId: String,
        ) : Event

        class ProceedToMoveDestinationCollectionSelection(
            val currentCollectionId: String,
            val isSingleStamp: Boolean,
        ) : Event

        class ProceedToMoveStamps(
            val sourceCollectionId: String,
            val destinationCollectionId: String,
            val stampSelectionIndex: Int,
        ) : Event

        object Done : Event
    }

    data class Parameters(
        val collectionId: String,
    )
}
