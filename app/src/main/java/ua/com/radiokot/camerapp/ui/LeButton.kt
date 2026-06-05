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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LeButton(
    modifier: Modifier = Modifier,
    frontColor: Color = LocalColors.current.leButtonFront,
    cornerRadius: Dp = 10.dp,
    depth: Dp = 10.dp,
    interactionSource: MutableInteractionSource = remember(::MutableInteractionSource),
    hapticFeedbackEnabled: Boolean = true,
    onClick: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalColors.current
    val hapticFeedback by rememberUpdatedState(LocalHapticFeedback.current)
    val isPressed by produceState(false) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                if (hapticFeedbackEnabled) {
                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.VirtualKey
                    )
                }
                value = true
            } else if (interaction is PressInteraction.Release
                || interaction is PressInteraction.Cancel
            ) {
                value = false
            }
        }
    }

    Box(
        modifier = modifier
            .heightIn(
                min = 64.dp,
            )
            .height(IntrinsicSize.Min)
            .run {
                if (onClick == null) {
                    return@run this
                }

                clickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = onClick,
                )
            }
    ) {
        val shape = remember {
            RoundedCornerShape(cornerRadius)
        }
        val depthShape = remember {
            RoundedCornerShape(
                bottomStart = cornerRadius,
                bottomEnd = cornerRadius,
            )
        }

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(depth + cornerRadius + 1.dp)
                .background(
                    color = colors.leButtonDepth,
                    shape = depthShape,
                )
                .border(
                    width = 2.dp,
                    color = colors.componentStroke,
                    shape = depthShape,
                )
        )

        val pressOffset by animateDpAsState(
            targetValue =
                if (isPressed)
                    depth * 0.5f
                else
                    0.dp,
            animationSpec = spring(
                stiffness = Spring.StiffnessHigh,
            ),
        )

        Box(
            contentAlignment = Alignment.Center,
            content = content,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = depth,
                )
                .graphicsLayer {
                    translationY = pressOffset.toPx()
                }
                .background(
                    color = frontColor,
                    shape = shape,
                )
                .border(
                    width = 2.dp,
                    color = colors.componentStroke,
                    shape = shape,
                )
        )
    }
}

@Composable
fun LeTextButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalColors.current

    LeButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                fontFamily = PodkovaFamily,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(10.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun LeButtonPreview(

) {
    var counter by remember {
        mutableIntStateOf(0)
    }

    AppTheme {
        Column(
            modifier = Modifier
                .paperBackground(
                    drawBackgroundColor = true,
                )
                .padding(24.dp)
        ) {
            LeTextButton(
                text = "Me clicked $counter time(s)",
                onClick = { counter++ },
            )
        }
    }
}
