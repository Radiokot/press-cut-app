package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.stamps.domain.GetStampCollectionsWithSamplesUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class CollectionsScreenViewModel(
    private val collectionRepository: StampCollectionRepository,
    private val getStampCollectionsWithSamplesUseCase: GetStampCollectionsWithSamplesUseCase,
) : ViewModel() {

    private val log by lazyLogger("CollectionsScreenVM")

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    val items: StateFlow<ImmutableList<CollectionListItem>> = runBlocking {
        getStampCollectionsWithSamplesUseCase()
            .map { collectionsWithSamples ->
                collectionsWithSamples
                    .map(::CollectionListItem)
                    .toPersistentList()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope)
    }

    fun onItemClicked(
        item: CollectionListItem,
    ) {
        val collectionId = item.key

        log.debug {
            "onItemClicked(): proceeding to collection:" +
                    "\ncollectionId = $collectionId"
        }

        events.tryEmit(
            Event.ProceedToCollection(
                collectionId = collectionId,
                focusNameInput = false,
            )
        )
    }

    fun onItemLongClicked(
        item: CollectionListItem,
    ) {
        val collectionId = item.key

        log.debug {
            "onItemLongClicked(): proceeding to collection actions:" +
                    "\ncollectionId = $collectionId"
        }

        events.tryEmit(
            Event.ProceedToCollectionActions(
                collectionId = collectionId,
            )
        )
    }

    fun onNewStampAction() {
        log.debug {
            "onNewStampAction(): proceeding to new stamp creation"
        }

        events.tryEmit(Event.ProceedToNewStamp)
    }

    private var addCollectionJob: Job? = null
    fun onNewCollectionAction() {
        if (addCollectionJob?.isActive == true) {
            return
        }

        addCollectionJob = viewModelScope.launch {
            addCollection()
        }
    }

    private suspend fun addCollection() {
        log.debug {
            "addCollection(): adding new collection"
        }

        val addedCollectionId =
            collectionRepository.addStampCollection(
                name = "New Collection",
            )

        log.debug {
            "addCollection(): proceeding to the newly added collection:" +
                    "\naddedCollectionId=$addedCollectionId"
        }
        log.info {
            "Added a collection $addedCollectionId"
        }

        events.emit(
            Event.ProceedToCollection(
                collectionId = addedCollectionId,
                focusNameInput = true,
            )
        )
    }

    fun onMoreClicked() {
        log.debug {
            "onMoreClicked(): proceeding to the about"
        }

        events.tryEmit(Event.ProceedToAbout)
    }

    sealed interface Event {
        class ProceedToCollection(
            val collectionId: String,
            val focusNameInput: Boolean,
        ) : Event

        class ProceedToCollectionActions(
            val collectionId: String,
        ) : Event

        object ProceedToNewStamp : Event

        object ProceedToAbout : Event
    }
}
