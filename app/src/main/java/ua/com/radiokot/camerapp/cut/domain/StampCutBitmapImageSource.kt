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

package ua.com.radiokot.camerapp.cut.domain

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntSize
import kotlinx.coroutines.flow.Flow
import ua.com.radiokot.camerapp.cut.domain.StampCutBitmapImageSource.Companion.SCENE_SIZE
import kotlin.math.max

/**
 * An image source for a stamp cut based on image bitmap.
 * The image is placed in a square scene of size [SCENE_SIZE].
 * The image can be panned ([imageCenter]), rotated ([imageRotationDegrees]) and scaled ([imageScale]).
 */
class StampCutBitmapImageSource(
    val bitmap: Bitmap,
) : StampCutImageSource {

    val image: ImageBitmap = bitmap.asImageBitmap()

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
        // Initially scale to fill.
        max(
            SCENE_SIZE / image.width,
            SCENE_SIZE / image.height,
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

    override fun capture(): Flow<Bitmap> {
        TODO("Not yet implemented")
    }

    fun draw(drawScope: DrawScope) {
        val size = imageRect.size * drawScope.density
        val topLeft = imageRect.topLeft * drawScope.density

        drawScope.drawContext.canvas.rotate(
            degrees = imageRotationDegrees,
            pivotX = imageCenter.x,
            pivotY = imageCenter.y,
        )

        drawScope.drawImage(
            image = image,
            dstOffset = topLeft.round(),
            dstSize = size.roundToIntSize(),
        )

        drawScope.drawContext.canvas.rotate(
            degrees = -imageRotationDegrees,
            pivotX = imageCenter.x,
            pivotY = imageCenter.y,
        )
    }

    companion object {
        const val SCENE_SIZE = 1920f
    }
}
