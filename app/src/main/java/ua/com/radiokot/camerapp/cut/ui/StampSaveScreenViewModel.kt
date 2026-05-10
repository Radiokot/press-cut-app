@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package ua.com.radiokot.camerapp.cut.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.FloatRange
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.util.fastCoerceIn
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
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

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
        combine(
            imageAdjustmentsControllerViewModel.contrastValue,
            imageAdjustmentsControllerViewModel.brightnessValue,
            imageAdjustmentsControllerViewModel.vibranceValue,
            transform = ::Triple
        )
            .mapLatest { (contrast, brightness, vibrance) ->
                val width = originalStampImageBitmap.width
                val height = originalStampImageBitmap.height

                val pixels = IntArray(width * height)
                originalStampImageBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                applyImageAdjustments(
                    pixels = pixels,
                    contrast = contrast / 100f,
                    brightness = brightness / 100f,
                    vibrance = vibrance / 100f,
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
            combine(
                imageAdjustmentsControllerViewModel.contrastValue,
                imageAdjustmentsControllerViewModel.brightnessValue,
                imageAdjustmentsControllerViewModel.vibranceValue,
                transform = { contrast, brightness, vibrance ->
                    contrast != 0 || brightness != 0 || vibrance != 0
                }
            ),
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
        @FloatRange(-1.0, 1.0)
        contrast: Float,
        @FloatRange(-1.0, 1.0)
        brightness: Float,
        @FloatRange(-1.0, 1.0)
        vibrance: Float,
    ) {
        // Contrast:
        // https://github.com/fabricjs/fabric.js/blob/e4cd1530ce2e684575e8db5d0a299d23c0c258e8/src/filters/Contrast.ts#L51
        // Brightness:
        // https://github.com/fabricjs/fabric.js/blob/e4cd1530ce2e684575e8db5d0a299d23c0c258e8/src/filters/Brightness.ts#L48
        // Vibrance:
        // https://github.com/fabricjs/fabric.js/blob/e4cd1530ce2e684575e8db5d0a299d23c0c258e8/src/filters/Vibrance.ts#L49

        var contrast = floor(contrast * 255)
        contrast = (259 * (contrast + 255)) / (255 * (259 - contrast))

        val brightness = round(brightness * 255)

        for (pixelIndex in pixels.indices) {
            val pixel = pixels[pixelIndex]

            val alpha = Color.alpha(pixel)
            if (alpha == 0) {
                continue
            }

            var red = Color.red(pixel).toFloat()
            var green = Color.green(pixel).toFloat()
            var blue = Color.blue(pixel).toFloat()

            val max = maxOf(red, green, blue)
            val avg = (red + green + blue) / 3
            val amt = ((abs(max - avg) * 2) / 255) * -vibrance

            red += if (max != red) (max - red) * amt else 0f
            red = contrast * (red - 128) + 128
            red += brightness

            green += if (max != green) (max - green) * amt else 0f
            green = contrast * (green - 128) + 128
            green += brightness

            blue += if (max != blue) (max - blue) * amt else 0f
            blue = contrast * (blue - 128) + 128
            blue += brightness

            pixels[pixelIndex] = Color.argb(
                alpha,
                red.toInt().fastCoerceIn(0, 255),
                green.toInt().fastCoerceIn(0, 255),
                blue.toInt().fastCoerceIn(0, 255),
            )
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
