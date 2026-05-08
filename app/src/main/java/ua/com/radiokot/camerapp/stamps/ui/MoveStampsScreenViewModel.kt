@file:OptIn(FlowPreview::class)

package ua.com.radiokot.camerapp.stamps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.time.Duration.Companion.milliseconds

class MoveStampsScreenViewModel(
    stampRepository: StampRepository,
    parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("MoveStampsScreenVM")

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    val progress: StateFlow<Float> =
        stampRepository
            .moveStampsBetweenCollections(
                sourceCollectionId = parameters.sourceCollectionId,
                destinationCollectionId = parameters.destinationCollectionId,
            )
            .sample(100.milliseconds)
            .map { (movedFileCount, totalFileCount) ->
                movedFileCount.toFloat() / totalFileCount
            }
            .onStart {
                log.debug {
                    "progress: starting:" +
                            "\nsourceCollectionId=${parameters.sourceCollectionId}" +
                            "\ndestinationCollectionId=${parameters.destinationCollectionId}"
                }
            }
            .onCompletion { error ->
                if (error != null) {
                    if (error !is CancellationException) {
                        log.error(error) {
                            "process: failed moving:" +
                                    "\nsourceCollectionId=${parameters.sourceCollectionId}" +
                                    "\ndestinationCollectionId=${parameters.destinationCollectionId}"
                        }
                    }
                    return@onCompletion
                }

                log.info {
                    "Moved stamps from the collection ${parameters.sourceCollectionId} " +
                            "to the collection ${parameters.destinationCollectionId}"
                }

                emit(1f)
                delay(800)
                events.emit(Event.Done)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    sealed interface Event {
        object Done : Event
    }

    data class Parameters(
        val sourceCollectionId: String,
        val destinationCollectionId: String,
    )
}
