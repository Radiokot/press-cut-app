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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.EmptyImageComponent
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun StampScreen(
    modifier: Modifier = Modifier,
    stampId: String,
    captionState: TextFieldState,
    isCaptionInputEnabled: Boolean,
    imageUri: Uri,
    shape: UiStampShape,
    takenAt: LocalDate,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onMoveAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = BoxWithConstraints(
    modifier = modifier
        // IME is handled in the composition.
        .safeGesturesPadding()
        .displayCutoutPadding()
) {
    val isScreenVeryTall = remember(maxHeight) {
        maxHeight >= 640.dp
    }
    val isScreenQuiteTall = remember(maxHeight) {
        maxHeight >= 460.dp
    }
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
    val visibleActionsVerticalOffset by animateDpAsState(
        targetValue =
            if (areActionsVisible)
                (-48).dp
            else
                0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Dp.VisibilityThreshold,
        )
    )
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Center)
            .graphicsLayer {
                translationY =
                    if (isScreenVeryTall)
                        -0.3f * imePadding.calculateBottomPadding().toPx() +
                                visibleActionsVerticalOffset.toPx()
                    else
                        0f
            }
    ) {
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .run {
                    if (!isScreenQuiteTall) {
                        return@run this
                    }

                    weight(4f)
                }
                .graphicsLayer {
                    alpha = detailsAlpha.value
                }
        ) {
            CaptionInput(
                isEnabled = isCaptionInputEnabled,
                inputState = captionState,
                focusRequester = captionInputFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .graphicsLayer {
                        alpha = detailsAlpha.value
                    }
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
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
            val stampImageLoadingOptions =
                shape
                    .getPreviewImageLoadingOptions(
                        density = LocalDensity.current,
                    )

            LandscapistImage(
                imageModel = { imageUri },
                requestBuilder = stampImageLoadingOptions.requestBuilder,
                imageOptions = stampImageLoadingOptions.imageOptions,
                component = EmptyImageComponent,
                modifier = Modifier
                    .size(
                        if (isScreenQuiteTall)
                            shape.size * 2f
                        else
                            shape.size * 1.5f
                    )
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
                        if (imageUri !== Uri.EMPTY) {
                            return@run this
                        }

                        background(Color.Yellow)
                    }
            )
        }

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(5f)
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = null,
                )
                .graphicsLayer {
                    alpha = detailsAlpha.value
                }
        ) {
            AnimatedContent(
                targetState = areActionsVisible,
                transitionSpec = {
                    val toShowActions = targetState
                    ContentTransform(
                        targetContentEnter =
                            if (toShowActions)
                                fadeIn() + slideInVertically()
                            else
                                fadeIn(),
                        initialContentExit =
                            if (!toShowActions)
                                fadeOut() + slideOutVertically()
                            else
                                fadeOut(),
                        sizeTransform = null,
                    )
                },
                modifier = Modifier
                    .width(StampContainerBaseSize.width * 2.5f)
            ) { showActions ->

                if (!showActions) {
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
                                .width(40.dp)
                        )

                        BasicText(
                            text = takenAt.toString(),
                            style = TextStyle(
                                fontFamily = PodkovaFamily,
                                fontSize = 16.sp,
                                color = Color(0xFFB9AC8C),
                                textAlign = TextAlign.Center,
                            ),
                        )

                        Image(
                            painter = painterResource(R.drawable.ic_pencil),
                            contentDescription = "Edit",
                            colorFilter = ColorFilter.tint(Color(0xFFB9AC8C)),
                            modifier = Modifier
                                .clickable(
                                    onClick = { areActionsVisible = !areActionsVisible },
                                )
                                .padding(
                                    vertical = 16.dp,
                                    horizontal = 12.dp,
                                )
                                .size(16.dp)
                        )
                    }
                } else {
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
            }
        }
    }
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    isCaptionSet: Boolean,
    onAddCaption: () -> Unit,
    onMove: () -> Unit,
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
            imageUri = Uri.EMPTY,
            shape = UiStampShapeA,
            takenAt = LocalDate.now(),
            onAddCaptionAction = { },
            onDeleteAction = { },
            onMoveAction = { },
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
        modifier = Modifier
            .width(350.dp)
    )
}
