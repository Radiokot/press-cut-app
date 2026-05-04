package ua.com.radiokot.camerapp.stamps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.stamps.domain.GetStampCollectionsWithSamplesUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionWithSamples
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

class CollectionActionsScreenViewModel(
    private val collectionRepository: StampCollectionRepository,
    private val stampRepository: StampRepository,
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

    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events: SharedFlow<Event> = _events

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

        _events.emit(Event.Done)
    }

    fun onMoveStampsAction() {
        log.debug {
            "onMoveStampsAction(): proceeding to destination collection selection"
        }

        _events.tryEmit(
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

        _events.tryEmit(
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
