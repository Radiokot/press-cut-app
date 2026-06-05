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

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ua.com.radiokot.camerapp.ui.LeButton
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground

@Composable
fun StampCutter(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember(::MutableInteractionSource),
    frameSize: DpSize,
) {
    val colors = LocalColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        val stampPainter = rememberVectorPainter(UiStampShapeA.fill)
        val stampStrokePainter = rememberVectorPainter(UiStampShapeA.stroke)
        val strokeColor = colors.componentStroke

        Spacer(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val stampPaint = Paint().apply {
                        blendMode = BlendMode.DstOut
                    }
                    val stampSize = frameSize.toSize()
                    val stampRect = Rect(
                        offset = Offset.Zero,
                        size = stampSize,
                    )
                    val stampOffset = Offset(
                        x = (size.width - stampSize.width) / 2,
                        y = (size.height - stampSize.height) / 2,
                    )
                    val brush = Brush.linearGradient(
                        0f to Color(0xFFABA59C),
                        0.3f to Color(0xFFE3DDD3),
                        0.6f to Color(0xFF8C827B),
                        start = Offset.Zero,
                        end = Offset(
                            x = 0f,
                            y = size.height,
                        )
                    )
                    val strokeColorFilter = ColorFilter.tint(strokeColor)

                    onDrawBehind {
                        drawRect(brush)
                        drawIntoCanvas { canvas ->
                            canvas.translate(stampOffset.x, stampOffset.y)
                            canvas.withSaveLayer(stampRect, stampPaint) {
                                with(stampPainter) {
                                    draw(stampSize)
                                }
                            }
                            with(stampStrokePainter) {
                                draw(
                                    size = stampSize,
                                    colorFilter = strokeColorFilter,
                                )
                            }
                            canvas.translate(-stampOffset.x, -stampOffset.y)
                        }
                    }
                }
        )

        val outerCornerRadius = 24.dp

        LeButton(
            frontColor = Color.Transparent,
            cornerRadius = outerCornerRadius,
            depth = 18.dp,
            interactionSource = interactionSource,
            onClick = null,
            hapticFeedbackEnabled = false,
            modifier = Modifier
                .fillMaxSize()
        ) {
            val textMeasurer = rememberTextMeasurer()

            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithCache {
                        val outerColor = colors.componentBackground
                        val outerCornerRadius = CornerRadius(
                            x = outerCornerRadius.toPx(),
                            y = outerCornerRadius.toPx(),
                        )
                        val outerPadding = Offset(
                            x = 1.dp.toPx(),
                            y = 1.dp.toPx(),
                        )
                        val outerSize = Size(
                            width = size.width - outerPadding.x * 2,
                            height = size.height - outerPadding.y * 2,
                        )
                        val innerCornerRadius = CornerRadius(
                            x = 6.dp.toPx(),
                            y = 6.dp.toPx(),
                        )
                        val innerPadding = outerPadding + Offset(
                            x = size.width * 0.15f,
                            y = size.height * 0.15f,
                        )
                        val innerSize = Size(
                            width = size.width - innerPadding.x * 2,
                            height = size.height - innerPadding.y * 2,
                        )
                        val outerDrawStyle = Stroke(
                            width = 2.dp.toPx(),
                        )
                        val textStyle = TextStyle(
                            fontFamily = PodkovaFamily,
                            color = Color(0xFFcbc4bb),
                            fontSize = (innerPadding.y * 0.33f).toSp(),
                            textAlign = TextAlign.Center,
                        )

                        onDrawBehind {
                            drawRoundRect(
                                color = outerColor,
                                cornerRadius = outerCornerRadius,
                                topLeft = outerPadding,
                                size = outerSize,
                            )
                            drawRoundRect(
                                color = Color.Black,
                                cornerRadius = innerCornerRadius,
                                topLeft = innerPadding,
                                size = innerSize,
                                blendMode = BlendMode.Clear,
                            )
                            drawRoundRect(
                                color = strokeColor,
                                cornerRadius = innerCornerRadius,
                                topLeft = innerPadding,
                                size = innerSize,
                                style = outerDrawStyle,
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "tap to focus, hold to cut",
                                style = textStyle,
                                topLeft = Offset(
                                    x = 0f,
                                    y = size.height * 0.90f,
                                ),
                                size = Size(
                                    width = size.width,
                                    height = textStyle.fontSize.toPx(),
                                )
                            )
                        }
                    }
            )
        }
    }
}

@Preview
@Composable
private fun StampCutterPreview(

) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .paperBackground()
    ) {
        StampCutter(
            frameSize = UiStampShapeA.size * 1.5f,
            modifier = Modifier
                .requiredWidth(UiStampShapeA.size.width * 2.5f)
                .requiredHeight(UiStampShapeA.size.height * 2.8f)
        )
    }
}
