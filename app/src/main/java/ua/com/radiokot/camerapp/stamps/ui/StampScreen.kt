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

@file:Suppress("ConvertLongToDuration")

package ua.com.radiokot.camerapp.stamps.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LocalColors
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
    isCaptionInputEnabled: State<Boolean>,
    imageUri: StableHolder<Uri>,
    shape: UiStampShape,
    takenAt: StableHolder<LocalDate>,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onMoveAction: () -> Unit,
    onSendAsImageAction: () -> Unit,
    onSendAsPosterAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val animatableDetailsAlpha = remember {
        Animatable(0f)
    }
    val animatableDragVerticalOffset = remember {
        Animatable(0f)
    }
    // 8 mm to drag to exit.
    val res = LocalResources.current
    val swipeToExitThreshold = remember(res) {
        8 * res.displayMetrics.ydpi / 25.4f
    }
    val areActionsVisible = retain {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(100)
        animatableDetailsAlpha.animateTo(
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

    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        StampScreenLandscape(
            modifier = modifier,
            stampId = stampId,
            captionState = captionState,
            isCaptionInputEnabled = isCaptionInputEnabled,
            imageUri = imageUri,
            shape = shape,
            takenAt = takenAt,
            onAddCaptionAction = onAddCaptionAction,
            areActionsVisible = areActionsVisible,
            onDeleteAction = onDeleteAction,
            onMoveAction = onMoveAction,
            onSendAsImageAction = onSendAsImageAction,
            onSendAsPosterAction = onSendAsPosterAction,
            onSwipedToExit = onSwipedToExit,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            animatableDetailsAlpha = animatableDetailsAlpha,
            animatableDragVerticalOffset = animatableDragVerticalOffset,
            swipeToExitThreshold = swipeToExitThreshold,
        )
    } else {
        StampScreenPortrait(
            modifier = modifier,
            stampId = stampId,
            captionState = captionState,
            isCaptionInputEnabled = isCaptionInputEnabled,
            imageUri = imageUri,
            shape = shape,
            takenAt = takenAt,
            onAddCaptionAction = onAddCaptionAction,
            areActionsVisible = areActionsVisible,
            onDeleteAction = onDeleteAction,
            onMoveAction = onMoveAction,
            onSendAsImageAction = onSendAsImageAction,
            onSendAsPosterAction = onSendAsPosterAction,
            onSwipedToExit = onSwipedToExit,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            animatableDetailsAlpha = animatableDetailsAlpha,
            animatableDragVerticalOffset = animatableDragVerticalOffset,
            swipeToExitThreshold = swipeToExitThreshold,
        )
    }
}

@Composable
private fun StampScreenPortrait(
    modifier: Modifier = Modifier,
    stampId: String,
    captionState: TextFieldState,
    isCaptionInputEnabled: State<Boolean>,
    imageUri: StableHolder<Uri>,
    shape: UiStampShape,
    takenAt: StableHolder<LocalDate>,
    areActionsVisible: MutableState<Boolean>,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onMoveAction: () -> Unit,
    onSendAsImageAction: () -> Unit,
    onSendAsPosterAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    animatableDetailsAlpha: Animatable<Float, *>,
    animatableDragVerticalOffset: Animatable<Float, *>,
    swipeToExitThreshold: Float,
) {
    val imePadding = WindowInsets.ime.asPaddingValues()
    val coroutineScope = rememberCoroutineScope()

    val animatableBottomContentHeight = remember {
        Animatable(
            initialValue = 0,
            typeConverter = Int.VectorConverter,
            visibilityThreshold = Int.VisibilityThreshold,
        )
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
                    ?.takeIf { areActionsVisible.value }
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
                maxWidth = width,
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
            StampScreenLayoutContent(
                stampId = stampId,
                captionState = captionState,
                isCaptionInputEnabled = isCaptionInputEnabled,
                imageUri = imageUri,
                shape = shape,
                takenAt = takenAt,
                areDateAndMoreVisible = remember { derivedStateOf { !areActionsVisible.value } },
                areActionsVisible = areActionsVisible,
                onAddCaptionAction = onAddCaptionAction,
                onDeleteAction = onDeleteAction,
                onMoveAction = onMoveAction,
                onSendAsImageAction = onSendAsImageAction,
                onSendAsPosterAction = onSendAsPosterAction,
                onSwipedToExit = onSwipedToExit,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                animatableDetailsAlpha = animatableDetailsAlpha,
                animatableDragVerticalOffset = animatableDragVerticalOffset,
                swipeToExitThreshold = swipeToExitThreshold,
                actionsEnterTransition = fadeIn() + slideInVertically(),
                actionsExitTransition = fadeOut() + slideOutVertically(),
                actionsModifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 32.dp,
                        bottom = 24.dp,
                    ),
            )
        },
        modifier = modifier
            .safeGesturesPadding()
            .displayCutoutPadding()
            .padding(24.dp)
    )
}

@Composable
fun StampScreenLandscape(
    modifier: Modifier = Modifier,
    stampId: String,
    captionState: TextFieldState,
    isCaptionInputEnabled: State<Boolean>,
    imageUri: StableHolder<Uri>,
    shape: UiStampShape,
    takenAt: StableHolder<LocalDate>,
    areActionsVisible: MutableState<Boolean>,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onMoveAction: () -> Unit,
    onSendAsImageAction: () -> Unit,
    onSendAsPosterAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    animatableDetailsAlpha: Animatable<Float, *>,
    animatableDragVerticalOffset: Animatable<Float, *>,
    swipeToExitThreshold: Float,
) {
    val coroutineScope = rememberCoroutineScope()
    val animatableColumnCenterXOffset = remember {
        Animatable(
            initialValue = 0,
            typeConverter = Int.VectorConverter,
            visibilityThreshold = Int.VisibilityThreshold,
        )
    }

    Layout(
        measurePolicy = { elements, layoutConstraints ->
            val width = layoutConstraints.maxWidth
            val height = layoutConstraints.maxHeight
            val actionsHorizontalSpacePx = 24.dp.roundToPx()

            val wrapContentConstraints = Constraints(
                maxWidth = width,
                maxHeight = height,
            )

            val caption = elements[0].measure(
                Constraints(
                    maxWidth = width + animatableColumnCenterXOffset.value * 2,
                    maxHeight = height,
                )
            )
            val date = elements[2].measure(wrapContentConstraints)

            var stampHeight = shape.size.height.toPx().toInt() * 2
            val columnHeight = caption.height + stampHeight + date.height
            if (columnHeight > height) {
                stampHeight =
                    (height - caption.height - date.height)
                        .fastCoerceAtLeast(stampHeight / 2)
            }

            val stamp = elements[1].measure(
                Constraints(
                    minWidth =
                        if (animatableColumnCenterXOffset.value != 0)
                            0
                        else
                            width,
                    maxWidth = width,
                    minHeight = stampHeight,
                    maxHeight = stampHeight,
                )
            )

            val actions = elements.getOrNull(3)?.measure(
                Constraints(
                    maxWidth = width / 2 - actionsHorizontalSpacePx,
                    maxHeight = height - caption.height,
                )
            )
            val targetColumnCenterXOffset =
                if (areActionsVisible.value)
                    -((actions?.width ?: 0) * 0.35f).fastRoundToInt()
                else
                    0
            if (animatableColumnCenterXOffset.targetValue != targetColumnCenterXOffset) {
                coroutineScope.launch {
                    animatableColumnCenterXOffset.animateTo(
                        targetValue = targetColumnCenterXOffset,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                }
            }

            layout(width, height) {
                val columnCenterX = width / 2 + animatableColumnCenterXOffset.value
                val stampY = (height - stampHeight) / 2
                caption.place(
                    x = columnCenterX - caption.width / 2,
                    y = stampY - caption.height,
                )
                date.place(
                    x = columnCenterX - date.width / 2,
                    y = stampY + stamp.height,
                )
                actions?.place(
                    x = columnCenterX + stamp.width / 2 + actionsHorizontalSpacePx,
                    y = (height - actions.height) / 2,
                )
                stamp.place(
                    x = columnCenterX - stamp.width / 2,
                    y = stampY,
                )
            }
        },
        content = {
            StampScreenLayoutContent(
                stampId = stampId,
                captionState = captionState,
                isCaptionInputEnabled = isCaptionInputEnabled,
                imageUri = imageUri,
                shape = shape,
                takenAt = takenAt,
                areDateAndMoreVisible = remember { mutableStateOf(true) },
                areActionsVisible = areActionsVisible,
                onAddCaptionAction = onAddCaptionAction,
                onDeleteAction = onDeleteAction,
                onMoveAction = onMoveAction,
                onSendAsImageAction = onSendAsImageAction,
                onSendAsPosterAction = onSendAsPosterAction,
                onSwipedToExit = onSwipedToExit,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                animatableDetailsAlpha = animatableDetailsAlpha,
                animatableDragVerticalOffset = animatableDragVerticalOffset,
                swipeToExitThreshold = swipeToExitThreshold,
                actionsEnterTransition = fadeIn() + slideInHorizontally(),
                actionsExitTransition = fadeOut() + slideOutHorizontally(),
                actionsModifier = Modifier
                    .fillMaxWidth(),
            )
        },
        modifier = modifier
            .safeGesturesPadding()
            .displayCutoutPadding()
            .padding(
                horizontal = 24.dp,
                vertical = 8.dp,
            )
    )
}


@Composable
private fun StampScreenLayoutContent(
    stampId: String,
    captionState: TextFieldState,
    isCaptionInputEnabled: State<Boolean>,
    imageUri: StableHolder<Uri>,
    shape: UiStampShape,
    takenAt: StableHolder<LocalDate>,
    areDateAndMoreVisible: State<Boolean>,
    areActionsVisible: MutableState<Boolean>,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onMoveAction: () -> Unit,
    onSendAsImageAction: () -> Unit,
    onSendAsPosterAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    animatableDetailsAlpha: Animatable<Float, *>,
    animatableDragVerticalOffset: Animatable<Float, *>,
    swipeToExitThreshold: Float,
    actionsEnterTransition: EnterTransition,
    actionsExitTransition: ExitTransition,
    @SuppressLint("ModifierParameter")
    actionsModifier: Modifier,
) {
    val captionInputFocusRequester = remember(::FocusRequester)
    val coroutineScope = rememberCoroutineScope()

    CaptionInput(
        isEnabled = isCaptionInputEnabled.value,
        inputState = captionState,
        focusRequester = captionInputFocusRequester,
        modifier = Modifier
            .padding(
                bottom = 24.dp,
            )
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatableDetailsAlpha.value
            }
    )

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                translationY = animatableDragVerticalOffset.value
            }
            .pointerInput(Unit) {
                val dragAnimationSpec = spring<Float>(
                    stiffness = Spring.StiffnessHigh
                )

                detectDragGestures(
                    onDragStart = {
                        coroutineScope.launch {
                            animatableDetailsAlpha.animateTo(0f)
                        }
                    },
                    onDrag = { _, offset ->
                        coroutineScope.launch {
                            animatableDragVerticalOffset.animateTo(
                                targetValue = animatableDragVerticalOffset.targetValue + offset.y,
                                animationSpec = dragAnimationSpec,
                            )
                        }
                    },
                    onDragEnd = onDragEnd@{
                        if (animatableDragVerticalOffset.targetValue.absoluteValue >= swipeToExitThreshold) {
                            onSwipedToExit()
                            return@onDragEnd
                        }

                        coroutineScope.launch {
                            animatableDragVerticalOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                )
                            )
                        }
                        coroutineScope.launch {
                            animatableDetailsAlpha.animateTo(1f)
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
                        color = LocalColors.current.stampShadow,
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
        visible = areDateAndMoreVisible.value,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .graphicsLayer {
                alpha = animatableDetailsAlpha.value
            }
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
                    color = LocalColors.current.textSecondary,
                    textAlign = TextAlign.Center,
                ),
            )

            Image(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = "More",
                colorFilter = ColorFilter.tint(LocalColors.current.textSecondary),
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = null,
                        onClick = {
                            areActionsVisible.value = !areActionsVisible.value
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
        visible = areActionsVisible.value,
        enter = actionsEnterTransition,
        exit = actionsExitTransition,
        modifier = Modifier
            .width(StampContainerBaseSize.width * 2.5f)
    ) {
        Actions(
            canAddCaption = !isCaptionInputEnabled.value,
            onAddCaption = {
                areActionsVisible.value = false
                onAddCaptionAction()
                coroutineScope.launch {
                    delay(100)
                    captionInputFocusRequester.requestFocus()
                }
            },
            onDelete = {
                areActionsVisible.value = false
                onDeleteAction()
            },
            onMove = {
                areActionsVisible.value = false
                onMoveAction()
            },
            onSendAsImage = {
                areActionsVisible.value = false
                onSendAsImageAction()
            },
            onSendAsPoster = {
                areActionsVisible.value = false
                onSendAsPosterAction()
            },
            modifier = actionsModifier
        )

        BackHandler {
            areActionsVisible.value = false
        }
    }
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    canAddCaption: Boolean,
    onAddCaption: () -> Unit,
    onMove: () -> Unit,
    onSendAsImage: () -> Unit,
    onSendAsPoster: () -> Unit,
    onDelete: () -> Unit,
) = Column(
    modifier = modifier
        .background(
            color = LocalColors.current.componentBackground,
            shape = RoundedCornerShape(cornerRadius),
        )
        .border(
            width = 2.dp,
            color = LocalColors.current.componentStroke,
            shape = RoundedCornerShape(cornerRadius),
        )
        .verticalScroll(
            state = rememberScrollState(),
            overscrollEffect = null,
        )
) {
    val colors = LocalColors.current
    val textStyle = remember(colors) {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }

    var submenu by remember {
        mutableIntStateOf(0)
    }

    AnimatedContent(
        targetState = submenu,
        modifier = Modifier
            .fillMaxWidth()
    ) { currentSubmenu ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (currentSubmenu == 1) {
                BasicText(
                    text = "Send stamp as",
                    style = textStyle.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 20.dp,
                        )
                )

                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(colors.componentDivider)
                )

                BasicText(
                    text = "An image",
                    style = textStyle,
                    modifier = Modifier
                        .clickable(
                            onClick = onSendAsImage,
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
                        .background(colors.componentDivider)
                )

                BasicText(
                    text = "A poster",
                    style = textStyle,
                    modifier = Modifier
                        .clickable(
                            onClick = onSendAsPoster,
                        )
                        .padding(
                            vertical = 20.dp,
                        )
                        .fillMaxWidth()
                )

                BackHandler { submenu = 0 }

                return@Column
            }

            if (canAddCaption) {
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
                        .background(colors.componentDivider)
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
                    .background(colors.componentDivider)
            )

            BasicText(
                text = "Send",
                style = textStyle,
                modifier = Modifier
                    .clickable(
                        onClick = {
                            submenu = 1
                        },
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
                    .background(colors.componentDivider)
            )

            BasicText(
                text = "Hold to delete",
                style = textStyle.copy(
                    color = colors.textDanger,
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
    }
}

@PreviewLightDark
@Composable
private fun StampScreenPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paperBackground(
                    drawBackgroundColor = true,
                )
        ) {
            StampScreen(
                stampId = "",
                captionState = TextFieldState("My stamp"),
                isCaptionInputEnabled = false.let(::mutableStateOf),
                imageUri = StableHolder(Uri.EMPTY),
                shape = UiStampShapeA,
                takenAt = StableHolder(LocalDate.now()),
                onAddCaptionAction = { },
                onDeleteAction = { },
                onMoveAction = { },
                onSendAsImageAction = { },
                onSendAsPosterAction = { },
                onSwipedToExit = { },
                sharedTransitionScope = null,
                animatedVisibilityScope = null,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ActionsPreview() {
    AppTheme {
        Actions(
            canAddCaption = false,
            onAddCaption = {},
            onDelete = {},
            onMove = {},
            onSendAsImage = {},
            onSendAsPoster = {},
            modifier = Modifier
                .width(350.dp)
                .paperBackground(
                    drawBackgroundColor = true,
                )
        )
    }
}
