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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.ui.LocalColors

@Composable
fun Modifier.holdToDeleteAction(
    roundedCornerRadius: Dp,
    areTopCornersRounded: Boolean,
    onDelete: () -> Unit,
): Modifier {
    val deleteAnimationProgress = remember {
        Animatable(0f)
    }
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = LocalColors.current.holdToDeleteBackground

    // When pressed, a growing red background is drawn.
    return this
        .drawWithCache {
            val roundedCornerRadius = CornerRadius(
                x = roundedCornerRadius.toPx(),
                y = roundedCornerRadius.toPx()
            )
            // The top corner radius matches outer corner radius
            // or shrinks to 0 if the divider is above.
            val topCornerRadius =
                if (areTopCornersRounded || deleteAnimationProgress.value < 0.8f)
                    roundedCornerRadius
                else
                    roundedCornerRadius *
                            (1 - deleteAnimationProgress.value) / 0.2f
            val halfWidth = size.width / 2f
            val path = Path()
            path.addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = Offset(
                            x = halfWidth - halfWidth * deleteAnimationProgress.value,
                            y = 0f,
                        ),
                        size = Size(
                            width = size.width * deleteAnimationProgress.value,
                            height = size.height,
                        ),
                    ),
                    topRight = topCornerRadius,
                    topLeft = topCornerRadius,
                    bottomRight = roundedCornerRadius,
                    bottomLeft = roundedCornerRadius,
                )
            )

            onDrawBehind {
                drawPath(
                    path = path,
                    color = backgroundColor,
                    alpha = deleteAnimationProgress.value * 2f,
                )
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.GestureEnd
                    )

                    val pressingJob = coroutineScope.launch {
                        deleteAnimationProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 1200,
                                easing = EaseInOutQuad,
                            ),
                        )
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onDelete()
                    }

                    try {
                        awaitRelease()
                    } finally {
                        // Either released or canceled.
                        if (!pressingJob.isCompleted) {
                            pressingJob.cancel()
                            coroutineScope.launch {
                                deleteAnimationProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessHigh,
                                    ),
                                )
                            }
                        }
                    }
                }
            )
        }
}
