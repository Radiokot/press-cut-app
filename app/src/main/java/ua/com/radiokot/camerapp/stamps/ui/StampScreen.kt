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
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastRoundToInt
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.EmptyImageComponent
import ua.com.radiokot.camerapp.util.StableHolder
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun StampScreen(
    modifier: Modifier = Modifier,
    stampId: String,
    captionState: TextFieldState,
    isCaptionInputEnabled: Boolean,
    imageUri: StableHolder<Uri>,
    shape: UiStampShape,
    takenAt: StableHolder<LocalDate>,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onMoveAction: () -> Unit,
    onSendAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val imePadding = WindowInsets.ime.asPaddingValues()
    val coroutineScope = rememberCoroutineScope()
    val detailsAlpha = remember {
        Animatable(0f)
    }
    val dragVerticalOffset = remember {
        Animatable(0f)
    }
    // 8 mm to drag to exit.
    val res = LocalResources.current
    val swipeToExitThreshold = remember(res) {
        8 * res.displayMetrics.ydpi / 25.4f
    }
    var areActionsVisible by retain {
        mutableStateOf(false)
    }
    val animatableBottomContentHeight = remember {
        Animatable(
            initialValue = 0,
            typeConverter = Int.VectorConverter,
            visibilityThreshold = Int.VisibilityThreshold,
        )
    }
    val captionInputFocusRequester = remember(::FocusRequester)

    LaunchedEffect(Unit) {
        delay(100)
        detailsAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400),
        )
    }

    // Oh my God...
    // Prevent window resize on soft keyboard appearance
    // on old Android versions.
    val window = LocalActivity.current?.window
    if (window != null) {
        val previousSoftInputMode = retain {
            window.attributes.softInputMode
        }
        DisposableEffect(Unit) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            onDispose {
                window.setSoftInputMode(previousSoftInputMode)
            }
        }
    }

    Layout(
        measurePolicy = { elements, layoutConstraints ->
            val width = layoutConstraints.maxWidth
            val height = layoutConstraints.maxHeight

            val wrapContentConstraints = Constraints(
                maxWidth = width,
                maxHeight = height,
            )

            val caption = elements[0].measure(wrapContentConstraints)

            val targetBottomContentHeight =
                (elements
                    .getOrNull(3)
                    ?.takeIf { areActionsVisible }
                    ?: elements[2])
                    .maxIntrinsicHeight(width)
            if (animatableBottomContentHeight.value == 0) {
                runBlocking {
                    animatableBottomContentHeight.snapTo(targetBottomContentHeight)
                }
            } else if (animatableBottomContentHeight.targetValue != targetBottomContentHeight) {
                coroutineScope.launch {
                    animatableBottomContentHeight.animateTo(
                        targetValue = targetBottomContentHeight,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                }
            }

            var stampHeight = shape.size.height.toPx().toInt() * 2
            var stampTop =
                ((height - stampHeight) / 2
                        - animatableBottomContentHeight.value * 0.35f
                        - imePadding.calculateBottomPadding().roundToPx() * 0.3f)
                    .toInt()
                    .fastCoerceAtLeast(caption.height)

            var bottomContentBottom = stampTop + stampHeight + animatableBottomContentHeight.value
            if (bottomContentBottom > height) {
                stampTop =
                    (stampTop - bottomContentBottom + layoutConstraints.maxHeight)
                        .fastCoerceAtLeast(caption.height)
                bottomContentBottom = stampTop + stampHeight + animatableBottomContentHeight.value

                if (bottomContentBottom > height) {
                    val stampSizeScale =
                        (1f - (bottomContentBottom - height).toFloat() / stampHeight)
                            .fastCoerceAtLeast(0.5f)
                    stampHeight = (stampHeight * stampSizeScale).toInt()
                }
            }

            val stamp = elements[1].measure(
                Constraints.fixed(
                    width = width,
                    height = stampHeight,
                )
            )

            val bottomContentConstraints = Constraints(
                maxWidth =
                    (StampContainerBaseSize.width.toPx() * 2.5f)
                        .fastRoundToInt(),
                maxHeight =
                    (height - stampTop - stampHeight)
                        .fastCoerceAtLeast(0),
            )

            val bottomDate = elements[2].measure(bottomContentConstraints)
            val bottomActions = elements.getOrNull(3)?.measure(bottomContentConstraints)

            layout(width, height) {
                caption.place(
                    x = 0,
                    y = stampTop - caption.height,
                )
                bottomDate.place(
                    x = (width - bottomDate.width) / 2,
                    y = stampTop + stamp.height,
                )
                bottomActions?.place(
                    x = (width - bottomActions.width) / 2,
                    y = stampTop + stamp.height,
                )
                stamp.place(
                    x = (width - stamp.width) / 2,
                    y = stampTop,
                )
            }
        },
        content = {
            CaptionInput(
                isEnabled = isCaptionInputEnabled,
                inputState = captionState,
                focusRequester = captionInputFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 24.dp,
                    )
                    .graphicsLayer {
                        alpha = detailsAlpha.value
                    }
            )

            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = dragVerticalOffset.value
                    }
                    .pointerInput(Unit) {
                        val dragAnimationSpec = spring<Float>(
                            stiffness = Spring.StiffnessHigh
                        )

                        detectDragGestures(
                            onDragStart = {
                                coroutineScope.launch {
                                    detailsAlpha.animateTo(0f)
                                }
                            },
                            onDrag = { _, offset ->
                                coroutineScope.launch {
                                    dragVerticalOffset.animateTo(
                                        targetValue = dragVerticalOffset.targetValue + offset.y,
                                        animationSpec = dragAnimationSpec,
                                    )
                                }
                            },
                            onDragEnd = onDragEnd@{
                                if (dragVerticalOffset.targetValue.absoluteValue >= swipeToExitThreshold) {
                                    onSwipedToExit()
                                    return@onDragEnd
                                }

                                coroutineScope.launch {
                                    dragVerticalOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                        )
                                    )
                                }
                                coroutineScope.launch {
                                    detailsAlpha.animateTo(1f)
                                }
                            },
                        )
                    }
            ) {
                val size = remember(maxHeight) {
                    DpSize(
                        width = shape.size.width * (maxHeight / shape.size.height),
                        height = maxHeight,
                    )
                }
                val density = LocalDensity.current
                val stampImageLoadingOptions = remember(shape, density) {
                    shape.getPreviewImageLoadingOptions(
                        density = density,
                    )
                }

                LandscapistImage(
                    imageModel = imageUri::value,
                    requestBuilder = stampImageLoadingOptions.requestBuilder,
                    imageOptions = stampImageLoadingOptions.imageOptions,
                    component = EmptyImageComponent,
                    modifier = Modifier
                        .size(size)
                        .run {
                            if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                                return@run this
                            }

                            with(sharedTransitionScope) {
                                sharedElement(
                                    sharedContentState = rememberSharedContentState(stampId),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                        }
                        .dropShadow(
                            shape = RectangleShape,
                            shadow = Shadow(
                                radius = 16.dp,
                                color = Color(0x7447525E),
                            )
                        )
                        .run {
                            if (imageUri.value !== Uri.EMPTY) {
                                return@run this
                            }

                            background(Color.Yellow)
                        }
                )
            }

            AnimatedVisibility(
                visible = !areActionsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(
                            min = 64.dp
                        )
                        .fillMaxWidth()
                ) {
                    Spacer(
                        modifier = Modifier
                            .width(32.dp)
                    )

                    BasicText(
                        text = takenAt.value.toString(),
                        style = TextStyle(
                            fontFamily = PodkovaFamily,
                            fontSize = 16.sp,
                            color = Color(0xFFB9AC8C),
                            textAlign = TextAlign.Center,
                        ),
                    )

                    Image(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "Edit",
                        colorFilter = ColorFilter.tint(Color(0xFFB9AC8C)),
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = null,
                                onClick = {
                                    areActionsVisible = !areActionsVisible
                                },
                            )
                            .size(
                                width = 32.dp,
                                height = 56.dp,
                            )
                            .padding(
                                vertical = 18.dp,
                            )
                    )
                }
            }

            AnimatedVisibility(
                visible = areActionsVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                Actions(
                    isCaptionSet = captionState.text.isNotEmpty(),
                    onAddCaption = {
                        areActionsVisible = false
                        onAddCaptionAction()
                        coroutineScope.launch {
                            delay(100)
                            captionInputFocusRequester.requestFocus()
                        }
                    },
                    onDelete = {
                        areActionsVisible = false
                        onDeleteAction()
                    },
                    onMove = {
                        areActionsVisible = false
                        onMoveAction()
                    },
                    onSend = {
                        areActionsVisible = false
                        onSendAction()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 32.dp,
                            bottom = 24.dp,
                        )
                )

                BackHandler {
                    areActionsVisible = false
                }
            }
        },
        modifier = modifier
            .safeGesturesPadding()
            .displayCutoutPadding()
            .padding(24.dp)
    )
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    isCaptionSet: Boolean,
    onAddCaption: () -> Unit,
    onMove: () -> Unit,
    onSend: () -> Unit,
    onDelete: () -> Unit,
) = Column(
    modifier = modifier
        .background(
            color = Color(0xFFfff9eb),
            shape = RoundedCornerShape(cornerRadius),
        )
        .border(
            width = 2.dp,
            color = Color(0xFF6B624B),
            shape = RoundedCornerShape(cornerRadius),
        )
        .verticalScroll(
            state = rememberScrollState(),
            overscrollEffect = null,
        )
) {
    val textStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            fontWeight = FontWeight.Bold,
        )
    }

    if (!isCaptionSet) {
        BasicText(
            text = "Add a caption",
            style = textStyle,
            modifier = Modifier
                .clickable(
                    onClick = onAddCaption,
                )
                .padding(
                    vertical = 20.dp,
                )
                .fillMaxWidth()
        )

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color(0xFFcbc4bb))
        )
    }

    BasicText(
        text = "Move",
        style = textStyle,
        modifier = Modifier
            .clickable(
                onClick = onMove,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )

    Spacer(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(Color(0xFFcbc4bb))
    )

    BasicText(
        text = "Send",
        style = textStyle,
        modifier = Modifier
            .clickable(
                onClick = onSend,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )

    Spacer(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(Color(0xFFcbc4bb))
    )

    BasicText(
        text = "Hold to delete",
        style = textStyle.copy(
            color = Color(0xFFD97D7D),
        ),
        modifier = Modifier
            .holdToDeleteAction(
                roundedCornerRadius = cornerRadius,
                areTopCornersRounded = false,
                onDelete = onDelete,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )
}

@Preview
@Composable
private fun StampScreenPreview(

) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paperBackground()
    ) {
        StampScreen(
            stampId = "",
            captionState = TextFieldState("My stamp"),
            isCaptionInputEnabled = false,
            imageUri = StableHolder(Uri.EMPTY),
            shape = UiStampShapeA,
            takenAt = StableHolder(LocalDate.now()),
            onAddCaptionAction = { },
            onDeleteAction = { },
            onMoveAction = { },
            onSendAction = { },
            onSwipedToExit = { },
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun ActionsPreview(

) {
    Actions(
        isCaptionSet = false,
        onAddCaption = {},
        onDelete = {},
        onMove = {},
        onSend = {},
        modifier = Modifier
            .width(350.dp)
    )
}
