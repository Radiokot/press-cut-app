package ua.com.radiokot.camerapp.stamps.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.image.LandscapistImage
import ua.com.radiokot.camerapp.ui.podkovaFamily
import ua.com.radiokot.camerapp.util.EmptyImageComponent
import ua.com.radiokot.camerapp.util.noProgressive
import kotlin.math.absoluteValue

@Composable
fun CollectionView(
    modifier: Modifier = Modifier,
    item: CollectionListItem,
    onClicked: (CollectionListItem) -> Unit,
    onLongClicked: (CollectionListItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = Box(
    modifier = modifier
        .requiredSize(CollectionViewSize)
        .combinedClickable(
            indication = null,
            interactionSource = null,
            onClick = {
                onClicked(item)
            },
            onLongClick = {
                onLongClicked(item)
            },
        )
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(CollectionViewSize.height * 0.7f)
            .run {
                if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                    return@run this
                }

                with(sharedTransitionScope) {
                    sharedElement(
                        sharedContentState = rememberSharedContentState("${item.key}-box-back"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            }
            .background(
                color = Color(0xFFCBC4BB),
                shape = CollectionViewShape,
            )
            .border(
                width = 2.dp,
                color = Color(0xFF6B624B),
                shape = CollectionViewShape,
            )
            .align(Alignment.BottomCenter)
    )

    // To avoid flicker when opening the stamps screen,
    // make the library load the image in a size
    // that matches the stamp size on the stamps screen.
    val density = LocalDensity.current
    val sampleImageOptions = retain(density) {
        with(density) {
            ImageOptions(
                requestSize = IntSize(
                    width = StampSize.width.roundToPx(),
                    height = StampSize.height.roundToPx(),
                )
            )
        }
    }
    val sampleImageRequestBuilder = retain(sampleImageOptions) {
        noProgressive(
            size = sampleImageOptions.requestSize,
        )
    }

    when (item.someStamps.size) {
        1 -> {
            StampSampleView(
                sample = item.someStamps[0],
                order = 0,
                possibleRotationAngles = CenterSampleRotationAngles,
                fallbackColor = Color.Yellow,
                imageOptions = sampleImageOptions,
                imageRequestBuilder = sampleImageRequestBuilder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        y = -(8.dp),
                    )
            )
        }

        2 -> {
            StampSampleView(
                sample = item.someStamps[1],
                order = 0,
                possibleRotationAngles = RightSampleRotationAngles,
                fallbackColor = Color.Red,
                imageOptions = sampleImageOptions,
                imageRequestBuilder = sampleImageRequestBuilder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = StampSize.width * 0.2f,
                        y = -(8.dp),
                    )
            )
            StampSampleView(
                sample = item.someStamps[0],
                order = 1,
                possibleRotationAngles = LeftSampleRotationAngles,
                fallbackColor = Color.Yellow,
                imageOptions = sampleImageOptions,
                imageRequestBuilder = sampleImageRequestBuilder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -StampSize.width * 0.2f,
                        y = -(4.dp),
                    )
            )
        }

        3 -> {
            StampSampleView(
                sample = item.someStamps[2],
                order = 0,
                possibleRotationAngles = RightSampleRotationAngles,
                fallbackColor = Color.Yellow,
                imageOptions = sampleImageOptions,
                imageRequestBuilder = sampleImageRequestBuilder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = StampSize.width * 0.25f,
                        y = -(2.dp),
                    )
            )
            StampSampleView(
                sample = item.someStamps[1],
                order = 1,
                possibleRotationAngles = CenterSampleRotationAngles,
                fallbackColor = Color.Red,
                imageOptions = sampleImageOptions,
                imageRequestBuilder = sampleImageRequestBuilder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        y = -(4.dp),
                    )
            )
            StampSampleView(
                sample = item.someStamps[0],
                order = 2,
                possibleRotationAngles = LeftSampleRotationAngles,
                fallbackColor = Color.Magenta,
                imageOptions = sampleImageOptions,
                imageRequestBuilder = sampleImageRequestBuilder,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -StampSize.width * 0.25f,
                        y = -(8.dp),
                    )
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(StampSize.height * 0.5f)
            .align(Alignment.BottomCenter)
            .run {
                if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                    return@run this
                }

                with(sharedTransitionScope) {
                    sharedBounds(
                        sharedContentState = rememberSharedContentState("${item.key}-box-front"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        exit = fadeOut(
                            animationSpec = snap(),
                        ),
                        zIndexInOverlay = 10f,
                    )
                }
            }
            .background(
                color = Color(0xFFFFF9EB),
                shape = CollectionViewShape,
            )
            .border(
                width = 2.dp,
                color = Color(0xFF6B624B),
                shape = CollectionViewShape,
            )
            .padding(8.dp)
    ) {
        BasicText(
            text = item.name,
            style = CollectionViewNameStyle,
            modifier = Modifier
                .fillMaxWidth()
                .run {
                    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                        return@run this
                    }

                    with(sharedTransitionScope) {
                        sharedElement(
                            sharedContentState = rememberSharedContentState("${item.key}-name"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            zIndexInOverlay = 20f,
                        )
                    }
                }
        )
    }
}

private val LeftSampleRotationAngles = floatArrayOf(-4f, -5f, -6f)
private val CenterSampleRotationAngles = floatArrayOf(3f, 2f, -2f, -3f)
private val RightSampleRotationAngles = floatArrayOf(6f, 5f, 4f)

val CollectionViewSize = DpSize(
    width = StampSize.width * 1.55f,
    height = StampSize.height
)
val CollectionViewNameStyle = TextStyle(
    fontFamily = podkovaFamily,
    fontSize = 20.sp,
    textAlign = TextAlign.Center,
)
val CollectionViewShape = RoundedCornerShape(10.dp)

@Composable
private fun StampSampleView(
    modifier: Modifier = Modifier,
    fallbackColor: Color,
    sample: CollectionListItem.StampSampleItem,
    possibleRotationAngles: FloatArray,
    order: Int,
    imageOptions: ImageOptions,
    imageRequestBuilder: ImageRequest.Builder.() -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) =
    LandscapistImage(
        imageModel = sample::imageUri,
        requestBuilder = imageRequestBuilder,
        imageOptions = imageOptions,
        component = EmptyImageComponent,
        modifier = modifier
            .size(StampSize * 0.85f)
            .run {
                if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                    return@run this
                }

                with(sharedTransitionScope) {
                    sharedElement(
                        sharedContentState = rememberSharedContentState(sample.key),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 1f + order,
                    )
                }
            }
            .rotate(
                (possibleRotationAngles[sample.key.hashCode().absoluteValue % possibleRotationAngles.size])
            )
            .run {
                if (sample.imageUri !== Uri.EMPTY) {
                    return@run this
                }

                background(fallbackColor)
            }
            .dropShadow(
                shape = RectangleShape,
                shadow = Shadow(
                    radius = 4.dp,
                    color = Color(0x7447525E),
                )
            )
    )
