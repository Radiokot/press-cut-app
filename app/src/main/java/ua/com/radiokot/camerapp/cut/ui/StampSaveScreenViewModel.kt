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

@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package ua.com.radiokot.camerapp.cut.ui

import android.graphics.Bitmap
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.cut.domain.ImageAdjustment
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class StampSaveScreenViewModel(
    private val stampRepository: StampRepository,
    val imageAdjustmentsControllerViewModel: ImageAdjustmentsControllerViewModel,
    private val parameters: Parameters,
) : ViewModel() {

    private val log by lazyLogger("StampSaveScreenVM")

    private val originalStampImageBitmap: Bitmap =
        parameters.stampImageBitmap
    val adjustedStampImage: StateFlow<ImageBitmap> =
        imageAdjustmentsControllerViewModel
            .adjustments
            .mapLatest { adjustments ->
                val width = originalStampImageBitmap.width
                val height = originalStampImageBitmap.height

                val pixels = IntArray(width * height)
                originalStampImageBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                applyImageAdjustments(
                    pixels = pixels,
                    adjustments = adjustments,
                )

                val resultBitmap = createBitmap(
                    width = width,
                    height = height,
                    config = originalStampImageBitmap.config!!,
                )
                resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

                resultBitmap.asImageBitmap()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = createBitmap(1, 1).asImageBitmap(),
            )

    val captionInput: TextFieldState = TextFieldState(initialText = "")

    val isDiscardConfirmationRequired: StateFlow<Boolean> =
        combine(
            snapshotFlow(captionInput::text)
                .map(CharSequence::isNotEmpty),
            imageAdjustmentsControllerViewModel
                .adjustments
                .map { adjustments ->
                    adjustments.fastAny { it.value != 0f }
                },
            transform = { anyCaption, anyAdjustments ->
                anyCaption || anyAdjustments
            }
        )
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    private var saveJob: Job? = null

    fun onSaveAction() {
        if (saveJob?.isActive == true) {
            return
        }

        log.debug {
            "onSaveAction(): saving"
        }

        saveJob = viewModelScope.launch {
            saveStamp()
        }
    }

    private suspend fun saveStamp() = withContext(Dispatchers.IO) {
        val collectionId =
            parameters.collectionId
                ?: StampCollection.PRIMARY_ID

        val caption =
            captionInput
                .text
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)

        log.debug {
            "saveStamp(): saving:" +
                    "\ncollectionId=$collectionId," +
                    "\ncaption=$caption"
        }

        stampRepository.addStamp(
            collectionId = collectionId,
            imageBitmap = adjustedStampImage.value.asAndroidBitmap(),
            caption = caption,
        )

        log.info {
            "Saved a stamp"
        }

        events.emit(Event.DidSave)
    }

    private fun applyImageAdjustments(
        pixels: IntArray,
        adjustments: List<ImageAdjustment>,
    ) {
        for (pixelIndex in pixels.indices) {
            val pixel = pixels[pixelIndex]

            val alpha = pixel shr 24
            if (alpha == 0) {
                continue
            }

            val rgb = intArrayOf(
                (pixel shr 16) and 0xFF,
                (pixel shr 8) and 0xFF,
                pixel and 0xFF,
            )

            adjustments.fastForEach { adjustment ->
                adjustment.apply(rgb)
            }

            pixels[pixelIndex] =
                (alpha shl 24) or
                        (rgb[0] shl 16) or
                        (rgb[1] shl 8) or
                        rgb[2]
        }
    }

    sealed interface Event {
        object DidSave : Event
    }

    data class Parameters(
        val collectionId: String?,
        val stampImageBitmap: Bitmap,
    )
}
