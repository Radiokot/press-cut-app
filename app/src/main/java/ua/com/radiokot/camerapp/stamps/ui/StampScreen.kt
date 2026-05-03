package ua.com.radiokot.camerapp.stamps.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.noProgressive
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.ui.podkovaFamily
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun StampScreen(
    modifier: Modifier = Modifier,
    stampId: String,
    isEditable: Boolean,
    captionState: TextFieldState,
    isCaptionInputEnabled: Boolean,
    imageUri: String,
    takenAt: LocalDate,
    onAddCaptionAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onSwipedToExit: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = Box(
    modifier = modifier
        .safeContentPadding()
) {
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
    val allCenterVerticalOffset = animateDpAsState(
        targetValue =
            if (areActionsVisible)
                -(StampSize.height / 2)
            else 0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
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

    BackHandler(
        enabled = areActionsVisible,
        onBack = { areActionsVisible = false },
    )

    AnimatedVisibility(
        visible = areActionsVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut(),
        modifier = Modifier
            .width(StampSize.width * 2.5f)
            .offset(
                y = StampSize.height,
            )
            .align(Alignment.Center)
            .graphicsLayer {
                alpha = detailsAlpha.value
            }
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
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Center)
            .graphicsLayer {
                translationY = allCenterVerticalOffset.value.toPx()
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
            LandscapistImage(
                imageModel = { imageUri.toUri() },
                requestBuilder = ImageRequest.Builder::noProgressive,
                modifier = Modifier
                    .size(StampSize * 2f)
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
                        if (imageUri.isNotEmpty()) {
                            return@run this
                        }

                        background(Color.Yellow)
                    }
            )
        }

        val dateRowAlpha by animateFloatAsState(
            targetValue = if (areActionsVisible) 0f else 1f,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(
                    min = 64.dp
                )
                .graphicsLayer {
                    alpha = detailsAlpha.value * dateRowAlpha
                }
        ) {
            Spacer(
                modifier = Modifier
                    .width(40.dp)
            )

            BasicText(
                text = takenAt.toString(),
                style = TextStyle(
                    fontFamily = podkovaFamily,
                    fontSize = 16.sp,
                    color = Color(0xFFB9AC8C),
                    textAlign = TextAlign.Center,
                ),
            )

            if (isEditable) {
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
            } else {
                Spacer(
                    modifier = Modifier
                        .width(40.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxHeight(0.2f)
        )
    }
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    isCaptionSet: Boolean,
    onAddCaption: () -> Unit,
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
            fontFamily = podkovaFamily,
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
        text = "Hold to delete",
        style = textStyle.copy(
            color = Color(0xFFD97D7D),
        ),
        modifier = Modifier
            .holdToDeleteAction(
                roundedCornerRadius = cornerRadius,
                areTopCornersRounded = isCaptionSet,
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
            isEditable = true,
            captionState = TextFieldState("My stamp"),
            isCaptionInputEnabled = false,
            imageUri = "",
            takenAt = LocalDate.now(),
            onAddCaptionAction = { },
            onDeleteAction = { },
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
        modifier = Modifier
            .width(350.dp)
    )
}
