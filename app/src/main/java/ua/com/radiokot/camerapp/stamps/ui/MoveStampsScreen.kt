package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import kotlinx.coroutines.delay
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.ui.podkovaFamily

/**
 * @param progressState in range of 0..1
 */
@Composable
fun MoveStampsScreen(
    modifier: Modifier = Modifier,
    progressState: FloatState,
) = Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .safeContentPadding()
        .padding(24.dp),
) {
    BasicText(
        text = "Moving the stamps…",
        style = TextStyle(
            fontFamily = podkovaFamily,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )
    )

    Spacer(
        modifier = Modifier
            .height(8.dp)
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progressState.floatValue,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
        )
    )

    Box(
        contentAlignment = Alignment.Center,
    ) {
        val strokeColor = Color(0xFF6B624B)
        val barColor = Color(0xFFD7C3AA)

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
                .size(80.dp)
        ) {
            this@Column.AnimatedVisibility(
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
                        colorFilter = ColorFilter.tint(Color(0xFF6B624B)),
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = barColor,
                                shape = CircleShape,
                            )
                            .border(
                                width = 3.dp,
                                color = Color(0xFFfff9eb),
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

    Spacer(
        modifier = Modifier
            .height(56.dp)
    )
}

@Composable
@Preview
private fun MoveStampsScreenPreview() {
    val progressState = produceState(0f) {
        repeat(10) {
            delay(400)
            value = (it + 1) / 10f
        }
    }

    MoveStampsScreen(
        progressState =
            progressState.asFloatState(),
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
