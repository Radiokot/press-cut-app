package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.stamps.domain.GetSortedStampCollectionsUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class SelectMoveDestinationCollectionDialogViewModel(
    private val collectionRepository: StampCollectionRepository,
    getSortedStampCollectionsUseCase: GetSortedStampCollectionsUseCase,
    parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("SelectMoveDestinationCollectionDialogVM")

    private val _collections: List<StampCollection> = runBlocking {
        getSortedStampCollectionsUseCase()
            .first()
            .mapNotNull { collection ->
                if (collection.id == parameters.sourceCollectionId) {
                    return@mapNotNull null
                }
                collection
            }
    }
    val collections: ImmutableList<String> =
        _collections
            .map(StampCollection::name)
            .toPersistentList()

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    fun onCollectionSelected(
        index: Int,
    ) {
        val collectionId =
            _collections
                .getOrNull(index)
                ?.id
                ?: return

        log.debug {
            "onCollectionSelected(): proceeding with the selected collection:" +
                    "\ncollectionId=$collectionId"
        }

        events.tryEmit(
            Event.CollectionSelected(
                collectionId = collectionId,
            )
        )
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
            "addCollection(): proceeding with the newly added collection:" +
                    "\naddedCollectionId=$addedCollectionId"
        }
        log.info {
            "Added a collection $addedCollectionId"
        }

        events.emit(
            Event.CollectionSelected(
                collectionId = addedCollectionId,
            )
        )
    }

    sealed interface Event {
        data class CollectionSelected(
            val collectionId: String,
        ) : Event
    }

    data class Parameters(
        val sourceCollectionId: String,
    )
}
