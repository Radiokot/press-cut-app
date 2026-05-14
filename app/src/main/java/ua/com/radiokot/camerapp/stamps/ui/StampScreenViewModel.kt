package ua.com.radiokot.camerapp.stamps.ui

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import java.time.LocalDate
import java.util.Optional

@Immutable
class StampScreenViewModel(
    private val parameters: Parameters,
    private val stampRepository: StampRepository,
) : ViewModel() {

    private val log by lazyLogger("StampScreenVM")

    private val stamp: Stamp = runBlocking {
        stampRepository.getStamp(parameters.stampId)
            ?: error("Stamp ${parameters.stampId} not found")
    }

    val stampId: String =
        stamp.id
    val imageUri: Uri =
        stamp.imageUri.toUri()
    val takenAt: LocalDate =
        stamp.takenAtLocal.toLocalDate()

    val caption: TextFieldState = TextFieldState(initialText = stamp.caption ?: "")
    val isCaptionInputEnabled: StateFlow<Boolean>
        field = MutableStateFlow(stamp.caption != null)

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    private var isDeleted = false
    private var isMoved = false

    fun onAddCaptionAction() {
        log.debug {
            "onAddCaptionAction(): enabling caption input"
        }

        isCaptionInputEnabled.value = true
    }

    fun onDeleteAction() {
        log.debug {
            "onDeleteAction(): deleting"
        }

        viewModelScope.launch {
            stampRepository.deleteStamp(stamp)
            isDeleted = true
            events.emit(Event.Done)
        }
    }

    fun onMoveAction() {
        log.debug {
            "onMoveAction(): proceeding to destination collection selection"
        }

        events.tryEmit(
            Event.ProceedToMoveDestinationCollectionSelection(
                currentCollectionId = stamp.collectionId,
            )
        )
    }

    private var moveJob: Job? = null

    fun onMoveDestinationCollectionSelected(
        destinationCollectionId: String,
    ) {
        if (moveJob?.isActive == true) {
            return
        }

        moveJob = viewModelScope.launch {
            move(
                destinationCollectionId = destinationCollectionId,
            )
        }
    }

    private suspend fun move(
        destinationCollectionId: String,
    ) {
        log.debug {
            "move(): saving updates and moving:" +
                    "\ndestinationCollectionId=$destinationCollectionId"
        }

        saveUpdates()

        stampRepository
            .moveStampsBetweenCollections(
                sourceCollectionId = stamp.collectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf(stamp.id),
            )
            .collect()

        log.info {
            "Stamp ${stamp.id} moved to the collection $destinationCollectionId"
        }

        isMoved = true
        events.emit(Event.Done)
    }

    private suspend fun saveUpdates() {
        val newCaption =
            caption
                .text
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)

        if (newCaption != stamp.caption) {
            log.debug {
                "saveUpdates(): updating the stamp:" +
                        "\nnewCaption=$newCaption"
            }

            stampRepository.updateStamp(
                stamp = stamp,
                newCaption = Optional.ofNullable(newCaption),
            )

            log.info {
                "Successfully updated stamp ${stamp.id}"
            }
        } else {
            log.debug {
                "saveUpdates(): no updates"
            }
        }
    }

    override fun onCleared() {
        if (!(isDeleted || isMoved)) {
            runBlocking {
                saveUpdates()
            }
        }
        super.onCleared()
    }

    data class Parameters(
        val stampId: String,
    )

    sealed interface Event {
        class ProceedToMoveDestinationCollectionSelection(
            val currentCollectionId: String,
        ) : Event

        object Done : Event
    }
}
