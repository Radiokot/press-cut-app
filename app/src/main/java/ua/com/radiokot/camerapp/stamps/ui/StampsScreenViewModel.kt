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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import ua.com.radiokot.camerapp.util.map

@Immutable
class StampsScreenViewModel(
    private val stampRepository: StampRepository,
    private val collectionRepository: StampCollectionRepository,
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
        // TODO
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

        val selectedStampIds = selectedStampIds.value
        var anyReadOnlyStamps = false
        val stampToDeleteIds =
            collectionStamps
                .value
                .mapNotNullTo(mutableSetOf()) { stamp ->
                    if (stamp.id !in selectedStampIds) {
                        return@mapNotNullTo null
                    }
                    if (stamp.isReadOnly) {
                        anyReadOnlyStamps = true
                        return@mapNotNullTo null
                    }
                    return@mapNotNullTo stamp.id
                }

        log.debug {
            "deleteStamps(): deleting:" +
                    "\nstampToDeleteIds=${stampToDeleteIds.size}" +
                    "\nanyReadOnlyStamps=$anyReadOnlyStamps" +
                    "\ncollectionId=$collectionId"
        }

        stampRepository.deleteStamps(
            collectionId = collectionId,
            stampIds = stampToDeleteIds,
        )

        if (!anyReadOnlyStamps) {
            log.info {
                "Deleted ${stampToDeleteIds.size} stamps from the collection $collectionId"
            }
        } else {
            log.info {
                "Deleted ${stampToDeleteIds.size} stamps from the collection $collectionId, " +
                        "read-only stamps remained"
            }

            events.emit(Event.ShowNotAllStampsDeletedExplanation)
        }

        log.debug {
            "deleteStamps(): done, clearing selection"
        }

        clearSelection()
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

        object ShowNotAllStampsDeletedExplanation : Event

        object Done : Event
    }

    data class Parameters(
        val collectionId: String,
    )
}
