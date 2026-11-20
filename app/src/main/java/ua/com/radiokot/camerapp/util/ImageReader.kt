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

package ua.com.radiokot.camerapp.util

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

inline fun ImageReader.draw(
    density: Density,
    block: DrawScope.() -> Unit,
) {
    val hardwareCanvas = surface.lockHardwareCanvas()

    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(hardwareCanvas),
        size = Size(width.toFloat(), height.toFloat()),
        block = block,
    )

    surface.unlockCanvasAndPost(hardwareCanvas)
}

suspend fun ImageReader.awaitBitmap(
    outConfig: Bitmap.Config,
): Bitmap = withContext(Dispatchers.Default) {

    val bitmap = awaitImage().use { image ->
        val imagePlane = image.planes.first()
        createBitmap(
            width = imagePlane.rowStride / imagePlane.pixelStride,
            height = height,
            config = when (imageFormat) {
                PixelFormat.RGBA_8888 -> Bitmap.Config.ARGB_8888
                else -> error("Unsupported reader pixel format $imageFormat")
            },
        ).apply { copyPixelsFromBuffer(imagePlane.buffer) }
    }

    bitmap.reconfigure(width, height, outConfig)

    return@withContext bitmap
}

private suspend fun ImageReader.awaitImage(

): Image = suspendCancellableCoroutine { continuation ->

    val handler = Handler(Looper.getMainLooper())

    setOnImageAvailableListener({
        setOnImageAvailableListener(null, null)
        continuation.resume(acquireLatestImage()!!) { _, image, _ ->
            image.close()
        }
    }, handler)

    continuation.invokeOnCancellation {
        setOnImageAvailableListener(null, null)
    }
}
