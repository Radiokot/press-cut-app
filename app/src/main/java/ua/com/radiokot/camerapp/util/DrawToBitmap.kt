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
import android.graphics.PorterDuff
import android.graphics.Rect
import android.media.ImageReader
import android.os.Looper
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

// Close copy of androidx\compose\ui\graphics\layer\LayerSnapshot.android.kt

suspend fun drawToBitmap(
    outBitmap: Bitmap,
    density: Density,
    block: DrawScope.() -> Unit,
) = withContext(Dispatchers.Default) {

    val looper = Looper.myLooper() ?: Looper.getMainLooper()
    val width = outBitmap.width
    val height = outBitmap.height

    ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        1,
    ).use { reader ->
        val image = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                reader.setOnImageAvailableListener(
                    { continuation.resume(it.acquireNextImage()) },
                    HandlerCompat.createAsync(looper),
                )

                val surface = reader.surface
                val hardwareCanvas = surface.lockHardwareCanvas()
                try {
                    hardwareCanvas.drawColor(Color.Black.toArgb(), PorterDuff.Mode.CLEAR)
                    CanvasDrawScope().draw(
                        density = density,
                        layoutDirection = LayoutDirection.Ltr,
                        canvas = Canvas(hardwareCanvas),
                        size = Size(width.toFloat(), height.toFloat()),
                        block = block,
                    )
                } finally {
                    surface.unlockCanvasAndPost(hardwareCanvas)
                }
            }
        }

        val rawBitmap = image.use { image ->
            val plane = image.planes!![0]
            val buffer = plane.buffer
            buffer.rewind()

            createBitmap(
                width = plane.rowStride / plane.pixelStride,
                height = height,
                config = Bitmap.Config.ARGB_8888,
            ).apply { copyPixelsFromBuffer(buffer) }
        }
        val resultRect = Rect(0, 0, width, height)
        android.graphics.Canvas(outBitmap).drawBitmap(
            rawBitmap,
            resultRect,
            resultRect,
            null,
        )
        rawBitmap.recycle()
    }
}
