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

package ua.com.radiokot.camerapp.cut.render

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.ui.DarkAppColors
import ua.com.radiokot.camerapp.ui.drawPaperBackground
import ua.com.radiokot.camerapp.util.drawToBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * A renderer of a freely transformed image.
 * The image is placed in a square scene of size [SCENE_SIZE].
 * The image can be panned ([imageCenter]), rotated ([imageRotationDegrees]) and scaled ([imageScale]).
 */
@Stable
class StampCutBitmapImageRenderer(
    private val image: ImageBitmap,
    coroutineScope: CoroutineScope,
) : StampCutImageRenderer,
    CoroutineScope by coroutineScope {

    /**
     * Position of the image center in the square scene of size [SCENE_SIZE].
     */
    var imageCenter: Offset by mutableStateOf(
        Offset(
            x = SCENE_SIZE / 2f,
            y = SCENE_SIZE / 2f,
        )
    )

    /**
     * XY scale of the image from [imageCenter].
     */
    var imageScale: Float by mutableFloatStateOf(
        // Initially scale to fit.
        clampImageScale(
            min(
                SCENE_SIZE / image.width,
                SCENE_SIZE / image.height,
            )
        )
    )

    /**
     * Positive for clockwise rotation around [imageCenter].
     */
    var imageRotationDegrees: Float by mutableFloatStateOf(0f)

    /**
     * The result image draw rect in the square scene of size [SCENE_SIZE].
     */
    val imageRect: Rect by derivedStateOf {
        val imageSize = Size(
            width = image.width * imageScale,
            height = image.height * imageScale,
        )
        Rect(
            offset = imageCenter - imageSize.center,
            size = imageSize,
        )
    }

    var screenshotRequest: Any? by mutableStateOf(null)
    private val screenshots = MutableSharedFlow<Bitmap>()

    @Suppress("NOTHING_TO_INLINE")
    inline fun clampImageScale(desiredScale: Float): Float =
        desiredScale.fastCoerceIn(0.1f, 3f)

    fun draw(
        drawScope: DrawScope,
    ) = with(drawScope) {
        // Scale the source to fill from center.
        val sceneSize = SCENE_SIZE * density
        val canvasScale = max(
            size.width / sceneSize,
            size.height / sceneSize,
        )
        drawContext.canvas.scale(
            sx = canvasScale,
            sy = canvasScale,
            pivotX = center.x,
            pivotY = center.y,
        )
        drawContext.canvas.translate(
            dx = (size.width - sceneSize) / 2f,
            dy = (size.height - sceneSize) / 2f,
        )

        drawPaperBackground(
            lineColor = DarkAppColors.paperBackgroundLine,
            backgroundColor = DarkAppColors.componentBackground,
            gridSizePx = (46 * density).fastRoundToInt(),
            gridThicknessPx = 2.6f * density,
            verticalOffsetPx = 0,
            sizePx = Size(
                width = sceneSize,
                height = sceneSize,
            ),
        )

        val center = imageCenter * density
        val size = imageRect.size * density
        val topLeft = imageRect.topLeft * density

        drawContext.canvas.rotate(
            degrees = imageRotationDegrees,
            pivotX = center.x,
            pivotY = center.y,
        )

        drawImage(
            image = image,
            dstOffset = topLeft.round(),
            dstSize = size.roundToIntSize(),
        )

        drawContext.canvas.rotate(
            degrees = -imageRotationDegrees,
            pivotX = center.x,
            pivotY = center.y,
        )
    }

    suspend fun awaitScreenshot(): Bitmap =
        screenshots
            .onStart { screenshotRequest = System.currentTimeMillis() }
            .first()

    fun onScreenshotTaken(
        screenshotGraphicsLayer: GraphicsLayer,
    ) {
        screenshotRequest = null

        launch(Dispatchers.Default) {
            val screenshot =
                createBitmap(
                    width = screenshotGraphicsLayer.size.width,
                    height = screenshotGraphicsLayer.size.height,
                    config = Bitmap.Config.RGB_565,
                )

            drawToBitmap(
                outBitmap = screenshot,
                density = Density(
                    density = 1f,
                    fontScale = 1f,
                ),
                block = {
                    drawLayer(screenshotGraphicsLayer)
                }
            )

            screenshots.emit(screenshot)
        }
    }

    override fun capture(): Flow<Bitmap> = channelFlow {

        coroutineScope {
            var hqSentAt = Long.MAX_VALUE

            launch {
                val screenshot = awaitScreenshot()
                if (System.currentTimeMillis() < hqSentAt) {
                    channel.send(screenshot)
                }
            }

            launch(Dispatchers.Default) {
                val hqCapture =
                    createBitmap(
                        width = (HQ_MULTIPLIER * SCENE_SIZE).toInt(),
                        height = (HQ_MULTIPLIER * SCENE_SIZE).toInt(),
                        config = Bitmap.Config.RGB_565,
                    )

                drawToBitmap(
                    outBitmap = hqCapture,
                    density = Density(
                        density = HQ_MULTIPLIER,
                        fontScale = 1f,
                    ),
                    block = ::draw,
                )

                hqSentAt = System.currentTimeMillis()
                channel.send(hqCapture)
            }
        }
    }

    companion object {
        const val SCENE_SIZE = 1920f
        const val HQ_MULTIPLIER = 2.1f
        const val HQ_SCENE_SIZE = SCENE_SIZE * HQ_MULTIPLIER
    }
}
