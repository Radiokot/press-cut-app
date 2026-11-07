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

import android.annotation.SuppressLint
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.cut.domain.StampCutCameraImageSource
import ua.com.radiokot.camerapp.cut.domain.StampCutImageSource
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import ua.com.radiokot.camerapp.util.map
import kotlin.math.min

@Immutable
class StampCutScreenViewModel : ViewModel() {

    private val log by lazyLogger("StampCutScreenVM")

    val imageSource: StampCutImageSource =
        StampCutCameraImageSource(
            coroutineScope = viewModelScope,
        )

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
        visibleViewfinderSize: Size,
        visibleFrameRect: Rect,
    ) {
        if (cutJob?.isActive == true) {
            return
        }

        log.debug {
            "onCutAction(): starting the cut sequence:" +
                    "\nvisibleViewfinderSize: $visibleViewfinderSize" +
                    "\nvisibleFrameRect: $visibleFrameRect"
        }

        cutJob = viewModelScope.launch {
            doCut(
                visibleViewfinderRect =
                    RectF(0f, 0f, visibleViewfinderSize.width, visibleViewfinderSize.height),
                visibleFrameRect =
                    RectF(
                        visibleFrameRect.left,
                        visibleFrameRect.top,
                        visibleFrameRect.right,
                        visibleFrameRect.bottom
                    ),
            )
        }
    }


    @SuppressLint("RestrictedApi")
    private suspend fun doCut(
        visibleViewfinderRect: RectF,
        visibleFrameRect: RectF,
    ) {
        imageSource
            .capture()
            .onEach { capturedBitmap ->
                println("OOLEG meow ${capturedBitmap.width}x${capturedBitmap.height}")
                cutBitmap.value =
                    frameImage(
                        image = capturedBitmap,
                        visibleViewfinderRect = visibleViewfinderRect,
                        visibleFrameRect = visibleFrameRect,
                    )
                capturedBitmap.recycle()
            }
            .onCompletion {
                events.emit(
                    Event.DidCut(
                        stampImageBitmap = cutBitmap.value!!,
                    )
                )
            }
            .collect()
    }

    fun onScreenDisposed() {
        log.debug {
            "onScreenDisposed(): clearing the cut"
        }

        cutJob?.cancel()
        cutBitmap.value?.recycle()
        cutBitmap.value = null
    }

    private fun frameImage(
        image: Bitmap,
        visibleViewfinderRect: RectF,
        visibleFrameRect: RectF,
    ): Bitmap {
        val frameScale = min(
            image.width / visibleViewfinderRect.width(),
            image.height / visibleViewfinderRect.height(),
        )
        val scaledViewfinderRect = visibleViewfinderRect * frameScale
        val scaledFrameRect = visibleFrameRect * frameScale

        val resultBitmap = createBitmap(
            width = scaledFrameRect.width().toInt(),
            height = scaledFrameRect.height().toInt(),
        )

        val imageShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                image,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP,
            ).apply {
                setLocalMatrix(Matrix().apply {
                    setTranslate(
                        -(image.width - scaledViewfinderRect.width()) / 2f
                                - scaledFrameRect.left,
                        -(image.height - scaledViewfinderRect.height()) / 2f
                                - scaledFrameRect.top,
                    )
                })
            }
            isAntiAlias = true
        }

        val stampPath =
            StampShapeA
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
        class DidCut(
            val stampImageBitmap: Bitmap,
        ) : Event
    }
}
