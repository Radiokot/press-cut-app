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

package ua.com.radiokot.camerapp.cut.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.toPath
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.times
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.cut.render.StampCutCameraImageRenderer
import ua.com.radiokot.camerapp.cut.render.StampCutImageRenderer
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.ui.UiStampShape
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import ua.com.radiokot.camerapp.util.map
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

@Immutable
class StampCutScreenViewModel(
    private val sharedViewModel: NewStampViewModel,
) : ViewModel() {

    private val log by lazyLogger("StampCutScreenVM")

    private val _shape = StampShapeA
    val shape = UiStampShape.fromShape(_shape)

    val imageRenderer: StampCutImageRenderer =
        sharedViewModel.imageRenderer

    private val cutBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val cutImage: StateFlow<ImageBitmap?> =
        cutBitmap
            .map(viewModelScope) { bitmap ->
                bitmap?.asImageBitmap()
            }

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    private var cutJob: Job? = null

    fun onCutAction(
        viewfinderSize: Size,
        shapeRectInViewfinder: Rect,
    ) {
        if (cutJob?.isActive == true) {
            return
        }

        log.debug {
            "onCutAction(): starting the cut sequence:" +
                    "\nviewfinderSize=$viewfinderSize" +
                    "\nshapeRectInViewfinder=$shapeRectInViewfinder"
        }

        cutJob = viewModelScope.launch {
            doCut(
                viewfinderRect =
                    RectF(0f, 0f, viewfinderSize.width, viewfinderSize.height),
                shapeRectInViewfinder =
                    RectF(
                        shapeRectInViewfinder.left,
                        shapeRectInViewfinder.top,
                        shapeRectInViewfinder.right,
                        shapeRectInViewfinder.bottom
                    ),
            )
        }
    }

    private suspend fun doCut(
        viewfinderRect: RectF,
        shapeRectInViewfinder: RectF,
    ) {
        val (stampImageBitmap, cutDuration) = measureTimedValue {

            // A delay gives better visual for camera.
            // Bitmap is slower on its own 🤷‍♂️
            if (imageRenderer is StampCutCameraImageRenderer) {
                delay(50.milliseconds)
            }

            imageRenderer
                .capture()
                .map { capturedBitmap ->
                    frameCapture(
                        capturedBitmap = capturedBitmap,
                        viewfinderRect = viewfinderRect,
                        shapeRectInViewfinder = shapeRectInViewfinder,
                    ).also { capturedBitmap.recycle() }
                }
                .onEach {
                    // Prevent harsh switch from LQ to HQ on the cut piece.
                    if (cutBitmap.value == null) {
                        cutBitmap.value = it
                    }
                }
                .lastOrNull()
        }

        if (stampImageBitmap == null) {
            log.warn {
                "doCut(): did cut but there was no image"
            }
            return
        }

        sharedViewModel.stampImageBitmap = stampImageBitmap

        // Ensure min delay for visually pleasant animation.
        delay(MIN_CUT_DURATION - cutDuration)

        events.emit(Event.ProceedToSave)
    }

    fun onScreenDisposed() {
        log.debug {
            "onScreenDisposed(): clearing the cut"
        }

        cutJob?.cancel()
        cutBitmap.value = null
    }

    private fun frameCapture(
        capturedBitmap: Bitmap,
        viewfinderRect: RectF,
        shapeRectInViewfinder: RectF,
    ): Bitmap {
        val viewfinderScale = min(
            capturedBitmap.width / viewfinderRect.width(),
            capturedBitmap.height / viewfinderRect.height(),
        )
        val scaledViewfinderRect = viewfinderRect * viewfinderScale
        val scaledShapeRect = shapeRectInViewfinder * viewfinderScale

        val resultBitmap = createBitmap(
            width = scaledShapeRect.width().toInt(),
            height = scaledShapeRect.height().toInt(),
        )

        val imageShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                capturedBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP,
            ).apply {
                setLocalMatrix(Matrix().apply {
                    setTranslate(
                        -(capturedBitmap.width - scaledViewfinderRect.width()) / 2f
                                - scaledShapeRect.left,
                        -(capturedBitmap.height - scaledViewfinderRect.height()) / 2f
                                - scaledShapeRect.top,
                    )
                })
            }
            isAntiAlias = true
        }

        val stampPath =
            _shape
                .path
                .toPath()
                .asAndroidPath()
                .apply {
                    transform(Matrix().apply {
                        val scale = resultBitmap.width / StampShapeA.size.width.toFloat()
                        setScale(scale, scale)
                    })
                }

        resultBitmap.applyCanvas {
            drawPath(stampPath, imageShaderPaint)
        }

        return resultBitmap
    }

    sealed interface Event {
        object ProceedToSave : Event
    }

    private companion object {
        val MIN_CUT_DURATION = 750.milliseconds
    }
}
