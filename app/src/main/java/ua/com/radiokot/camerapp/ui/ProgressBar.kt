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

package ua.com.radiokot.camerapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ua.com.radiokot.camerapp.R

/**
 * @param progressState value in [0..1] range
 */
@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    progressState: FloatState,
) {
    val colors = LocalColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progressState.floatValue,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
        )
    )
    val okContainerSize = 80.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .requiredSizeIn(
                minWidth = okContainerSize,
                minHeight = okContainerSize,
            )
    ) {
        val strokeColor = colors.componentStroke
        val barColor = colors.progressBarProgress

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .drawWithCache {
                    val strokeStyle = Stroke(
                        width = 2.dp.toPx(),
                    )
                    val roundedCornerRadius = CornerRadius(
                        x = size.height / 2f,
                        y = size.height / 2f,
                    )
                    val barOffset = Offset(
                        x = 4.dp.toPx(),
                        y = 4.dp.toPx(),
                    )
                    val barSize = Size(
                        width =
                            ((size.width - 2 * barOffset.x) * animatedProgress)
                                .fastCoerceAtLeast((roundedCornerRadius.x - barOffset.x) * 2f),
                        height =
                            size.height - 2 * barOffset.y,
                    )

                    onDrawBehind {
                        drawRoundRect(
                            color = strokeColor,
                            style = strokeStyle,
                            cornerRadius = roundedCornerRadius,
                        )

                        drawRoundRect(
                            color = barColor,
                            style = Fill,
                            topLeft = barOffset,
                            size = barSize,
                            cornerRadius = roundedCornerRadius,
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .size(okContainerSize)
        ) {
            AnimatedVisibility(
                visible = progressState.floatValue == 1f,
                enter = fadeIn() + scaleIn(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                    ),
                ),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(R.drawable.ok_by_pronto_illustration_from_noun_project),
                        contentDescription = "OK",
                        colorFilter = ColorFilter.tint(strokeColor),
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = barColor,
                                shape = CircleShape,
                            )
                            .border(
                                width = 3.dp,
                                color = colors.screenBackground,
                                shape = CircleShape,
                            )
                            .padding(3.dp)
                            .border(
                                width = 2.dp,
                                color = strokeColor,
                                shape = CircleShape,
                            )
                            .padding(top = 9.dp)
                    )
                }
            }
        }
    }
}

private class ProgressBarPreviewParameterProvider
    : CollectionPreviewParameterProvider<FloatState>(
    collection = listOf(
        mutableFloatStateOf(0f),
        mutableFloatStateOf(0.01f),
        mutableFloatStateOf(0.5f),
        mutableFloatStateOf(0.99f),
        mutableFloatStateOf(1f),
    ),
) {
    override fun getDisplayName(index: Int): String =
        values.toList()[index].floatValue.toString()
}

@PreviewLightDark
@Composable
private fun ProgressBarPreview(
    @PreviewParameter(ProgressBarPreviewParameterProvider::class)
    progressState: FloatState,
) {
    AppTheme {
        ProgressBar(
            progressState = progressState,
            modifier = Modifier
                .paperBackground(
                    drawBackgroundColor = true,
                )
        )
    }
}

@Preview
@Composable
private fun ProgressBarInteractivePreview() {
    val progressState = produceState(0f) {
        while (isActive) {
            repeat(10) {
                delay(400)
                value = (it + 1) / 10f
            }
            delay(1000)
            value = 0f
        }
    }

    ProgressBar(
        progressState = progressState.asFloatState(),
        modifier = Modifier
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
