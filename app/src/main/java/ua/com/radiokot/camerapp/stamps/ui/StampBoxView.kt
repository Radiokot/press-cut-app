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

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.collections.immutable.ImmutableList
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.util.EmptyImageComponent
import kotlin.math.absoluteValue

@Composable
fun StampBoxView(
    modifier: Modifier = Modifier,
    name: String,
    someStamps: ImmutableList<StampSampleItem>,
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = Box(
    modifier = modifier
        .requiredSize(CollectionViewSize)
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
                        sharedContentState = rememberSharedContentState("${key}-box-back"),
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

    when (someStamps.size) {
        1 -> {
            StampSampleView(
                sample = someStamps[0],
                order = 0,
                possibleRotationAngles = CenterSampleRotationAngles,
                fallbackColor = Color.Yellow,
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
                sample = someStamps[1],
                order = 0,
                possibleRotationAngles = RightSampleRotationAngles,
                fallbackColor = Color.Red,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = StampContainerBaseSize.width * 0.2f,
                        y = -(8.dp),
                    )
            )
            StampSampleView(
                sample = someStamps[0],
                order = 1,
                possibleRotationAngles = LeftSampleRotationAngles,
                fallbackColor = Color.Yellow,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -StampContainerBaseSize.width * 0.2f,
                        y = -(4.dp),
                    )
            )
        }

        3 -> {
            StampSampleView(
                sample = someStamps[2],
                order = 0,
                possibleRotationAngles = RightSampleRotationAngles,
                fallbackColor = Color.Yellow,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = StampContainerBaseSize.width * 0.25f,
                        y = -(2.dp),
                    )
            )
            StampSampleView(
                sample = someStamps[1],
                order = 1,
                possibleRotationAngles = CenterSampleRotationAngles,
                fallbackColor = Color.Red,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        y = -(4.dp),
                    )
            )
            StampSampleView(
                sample = someStamps[0],
                order = 2,
                possibleRotationAngles = LeftSampleRotationAngles,
                fallbackColor = Color.Magenta,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = -StampContainerBaseSize.width * 0.25f,
                        y = -(8.dp),
                    )
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(StampContainerBaseSize.height * 0.5f)
            .align(Alignment.BottomCenter)
            .run {
                if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                    return@run this
                }

                with(sharedTransitionScope) {
                    sharedBounds(
                        sharedContentState = rememberSharedContentState("${key}-box-front"),
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
            text = name,
            style = CollectionViewNameStyle,
            modifier = Modifier
                .fillMaxWidth()
                .run {
                    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                        return@run this
                    }

                    with(sharedTransitionScope) {
                        sharedElement(
                            sharedContentState = rememberSharedContentState("${key}-name"),
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
    width = StampContainerBaseSize.width * 1.55f,
    height = StampContainerBaseSize.height
)
val CollectionViewNameStyle = TextStyle(
    fontFamily = PodkovaFamily,
    fontSize = 20.sp,
    textAlign = TextAlign.Center,
)
val CollectionViewShape = RoundedCornerShape(10.dp)

@Composable
private fun StampSampleView(
    modifier: Modifier = Modifier,
    fallbackColor: Color,
    sample: StampSampleItem,
    possibleRotationAngles: FloatArray,
    order: Int,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val stampImageLoadingOptions =
        sample
            .shape
            .getListImageLoadingOptions(
                density = LocalDensity.current,
            )

    LandscapistImage(
        imageModel = sample.imageUri::value,
        requestBuilder = stampImageLoadingOptions.requestBuilder,
        imageOptions = stampImageLoadingOptions.imageOptions,
        component = EmptyImageComponent,
        modifier = modifier
            .size(sample.shape.size * 0.85f)
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
                if (sample.imageUri.value !== Uri.EMPTY) {
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
}
