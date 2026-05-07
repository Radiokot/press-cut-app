package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun Modifier.selectionEnvelope(
    animationProgressState: State<Float>,
) = drawWithCache {
    val (width, height) = size
    val overlap = 1.dp.toPx()
    val roundingPathEffect = PathEffect.cornerPathEffect(6.dp.toPx())

    val leftPart = Path().apply {
        moveTo(-overlap, height)
        lineTo(-overlap, height * 0.4f)
        lineTo(overlap + width, height)
        close()
    }
    val rightPart = Path().apply {
        moveTo(overlap + width, height)
        lineTo(overlap + width, height * 0.4f)
        lineTo(-overlap, height)
        close()
    }
    val leftFillPaint = Paint().apply {
        color = Color(0xFFfff9eb)
        style = PaintingStyle.Fill
        pathEffect = roundingPathEffect
        isAntiAlias = true
    }
    val rightFillPaint = Paint().apply {
        color = Color(0xFFF1EBDB)
        style = PaintingStyle.Fill
        pathEffect = roundingPathEffect
        isAntiAlias = true
    }
    val strokePaint = Paint().apply {
        color = Color(0xFF6B624B)
        style = PaintingStyle.Stroke
        strokeWidth = 2.dp.toPx()
        pathEffect = roundingPathEffect
        isAntiAlias = true
    }

    onDrawWithContent {
        drawContent()

        if (animationProgressState.value <= 0.05f) {
            return@onDrawWithContent
        }

        scale(
            scaleX = 1f,
            scaleY = animationProgressState.value,
            pivot = Offset(
                x = 0f,
                y = height,
            )
        ) {
            drawIntoCanvas { canvas ->
                strokePaint.alpha = animationProgressState.value

                canvas.drawOutline(
                    outline = Outline.Generic(rightPart),
                    paint = rightFillPaint
                )
                canvas.drawOutline(
                    outline = Outline.Generic(rightPart),
                    paint = strokePaint
                )
                canvas.drawOutline(
                    outline = Outline.Generic(leftPart),
                    paint = leftFillPaint
                )
                canvas.drawOutline(
                    outline = Outline.Generic(leftPart),
                    paint = strokePaint
                )
            }
        }
    }
}


@Preview
@Composable
private fun Envelope() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(StampShapeA.size * 1.2f)
    ) {
        val shadowColor = Color(0x7447525E)
        val animationProgress = remember {
            Animatable(1f)
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(1000)
                animationProgress.animateTo(
                    targetValue =
                        if (animationProgress.targetValue == 1f)
                            0f
                        else
                            1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                )
            }
        }

        Image(
            painter = rememberVectorPainter(StampShapeA.fill),
            colorFilter = ColorFilter.tint(Color(0xff757a80)),
            contentDescription = null,
            modifier = Modifier
                .size(StampShapeA.size)
                .dropShadow(
                    shape = RectangleShape,
                    shadow = Shadow(
                        radius = 4.dp,
                        color = shadowColor,
                    )
                )
                .selectionEnvelope(
                    animationProgressState = animationProgress.asState(),
                )
        )
    }
}
