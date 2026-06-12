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

package ua.com.radiokot.camerapp.stamps.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.collection.mutableFloatListOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.model.ImageResult
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.AppColors

class CreateStampPosterUseCase(
    private val landscapist: Landscapist,
    private val context: Context,
) {
    suspend operator fun invoke(
        stamp: Stamp,
        colors: AppColors,
    ): Bitmap {
        val posterBitmap = createBitmap(
            width = 900,
            height = 1600,
        )

        val stampBitmap =
            landscapist
                .load(
                    ImageRequest
                        .builder()
                        .model(stamp.imageUri.toUri())
                        .progressiveEnabled(false)
                        .build()
                )
                .filterIsInstance<ImageResult.Success>()
                .firstOrNull()
                ?.data
                ?.let { it as? Bitmap }
                ?.let {
                    if (it.config == Bitmap.Config.HARDWARE)
                        it.copy(Bitmap.Config.ARGB_8888, false)
                    else
                        it
                }
                ?: error("Failed loading stamp bitmap")

        val stampDrawScale = posterBitmap.width * 0.5f / stampBitmap.width
        val stampDrawWidth = (stampBitmap.width * stampDrawScale).fastRoundToInt()
        val stampDrawHeight = (stampBitmap.height * stampDrawScale).fastRoundToInt()
        val stampDrawLeft = (posterBitmap.width - stampDrawWidth) / 2
        val stampDrawTop = (posterBitmap.height - stampDrawHeight) / 2
        val stampDrawRect =
            Rect(
                stampDrawLeft,
                stampDrawTop,
                stampDrawLeft + stampDrawWidth,
                stampDrawTop + stampDrawHeight,
            )

        posterBitmap.applyCanvas {
            drawRect(
                Rect(
                    0,
                    0,
                    posterBitmap.width,
                    posterBitmap.height,
                ),
                Paint().apply {
                    style = Paint.Style.FILL
                    color = colors.componentBackground.toArgb()
                }
            )

            val paperBackgroundGridSize = 48
            val paperBackgroundLines = mutableFloatListOf()

            for (x in (paperBackgroundGridSize - 6..posterBitmap.width step paperBackgroundGridSize)) {
                paperBackgroundLines += x.toFloat()
                paperBackgroundLines += 0f
                paperBackgroundLines += x.toFloat()
                paperBackgroundLines += posterBitmap.height.toFloat()
            }
            for (y in (paperBackgroundGridSize - 8..posterBitmap.height step paperBackgroundGridSize)) {
                paperBackgroundLines += 0f
                paperBackgroundLines += y.toFloat()
                paperBackgroundLines += posterBitmap.width.toFloat()
                paperBackgroundLines += y.toFloat()
            }
            drawLines(
                FloatArray(paperBackgroundLines.size, paperBackgroundLines::get),
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = colors.paperBackgroundLine.toArgb()
                }
            )

            drawRect(
                stampDrawRect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(42f, 0f, 0f, colors.stampShadow.toArgb())
                }
            )

            drawBitmap(
                stampBitmap,
                null,
                stampDrawRect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isFilterBitmap = true
                }
            )

            if (stamp.caption != null) {
                val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.textPrimary.toArgb()
                    textSize = 60f
                    typeface = ResourcesCompat.getFont(context, R.font.podkova_regular)
                }
                val captionLayout =
                    StaticLayout
                        .Builder
                        .obtain(
                            stamp.caption,
                            0,
                            stamp.caption.length,
                            captionPaint,
                            (posterBitmap.width * 0.7f).fastRoundToInt()
                        )
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build()

                withTranslation(
                    x = (width - captionLayout.width) / 2f,
                    y = stampDrawRect.top - captionLayout.height - 56f,
                ) {
                    captionLayout.draw(this)
                }
            }
        }

        return posterBitmap
    }
}
