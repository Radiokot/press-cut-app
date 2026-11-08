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

package ua.com.radiokot.camerapp.cut.ui

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.stamps.ui.StampCutter
import ua.com.radiokot.camerapp.stamps.ui.UiStampShapeA
import ua.com.radiokot.camerapp.util.optionalSharedElement
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun StampCutFromCameraScreen(
    modifier: Modifier = Modifier,
    useCaseGroup: UseCaseGroup,
    surfaceRequest: SurfaceRequest?,
    cutImage: ImageBitmap?,
    onCutAction: (
        viewfinderSize: Size,
        cutFrameRect: Rect,
    ) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var camera by remember {
        mutableStateOf<Camera?>(null)
    }
    val processCameraProvider by produceState<ProcessCameraProvider?>(null) {
        value = ProcessCameraProvider.awaitInstance(context).also {
            camera = it.bindToLifecycle(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup = useCaseGroup,
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { processCameraProvider?.unbindAll() }
    }

    val focusIndicatorAlpha = remember {
        Animatable(0f)
    }

    StampCutScreenScaffold(
        modifier = modifier,
        cutImage = cutImage,
        onCutAction = onCutAction,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        imageSource = { imageSourceModifier, frameLayoutCoordinatesState ->
            if (surfaceRequest == null) {
                return@StampCutScreenScaffold
            }

            val coordinateTransformer = remember(::MutableCoordinateTransformer)
            val meteringFactory = remember(surfaceRequest) {
                SurfaceOrientedMeteringPointFactory(
                    surfaceRequest.resolution.width.toFloat(),
                    surfaceRequest.resolution.height.toFloat()
                )
            }

            val focusAtCenter = remember(coordinateTransformer) {
                fun() {
                    val camera = camera
                        ?: return
                    val frameLayoutCoordinates = frameLayoutCoordinatesState.value
                        ?: return

                    val centerPoint =
                        coordinateTransformer
                            .transformMatrix
                            .map(
                                frameLayoutCoordinates
                                    .boundsInParent()
                                    .center
                            )
                    val meteringPoint = meteringFactory.createPoint(
                        centerPoint.x,
                        centerPoint.y
                    )

                    camera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(meteringPoint)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                    )
                    coroutineScope.launch {
                        focusIndicatorAlpha.animateTo(
                            targetValue = 1f,
                        )
                        focusIndicatorAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(800),
                        )
                    }
                }
            }

            CameraXViewfinder(
                surfaceRequest = surfaceRequest,
                coordinateTransformer = coordinateTransformer,
                modifier = imageSourceModifier
                    // Tap to focus at center.
                    .pointerInput(
                        focusAtCenter,
                    ) {
                        detectTapGestures {
                            focusAtCenter()
                        }
                    }
                    .drawWithCache {
                        // Focus indicator.
                        val lineHalfLength = 32.dp.toPx()
                        val frameCenter =
                            frameLayoutCoordinatesState
                                .value
                                ?.boundsInParent()
                                ?.center
                                ?: size.center
                        val verticalLineWStart = frameCenter + Offset(0f, -lineHalfLength)
                        val verticalLineWEnd = frameCenter + Offset(0f, lineHalfLength)
                        val verticalLineBStart = frameCenter + Offset(1f, -lineHalfLength)
                        val verticalLineBEnd = frameCenter + Offset(1f, lineHalfLength)
                        val horizontalLineWStart = frameCenter + Offset(-lineHalfLength, 0f)
                        val horizontalLineWEnd = frameCenter + Offset(lineHalfLength, 0f)
                        val horizontalLineBStart = frameCenter + Offset(-lineHalfLength, 1f)
                        val horizontalLineBEnd = frameCenter + Offset(lineHalfLength, 1f)

                        onDrawWithContent {
                            drawContent()

                            val colorA = Color.White.copy(
                                alpha = focusIndicatorAlpha.value,
                            )
                            val colorB = Color.Black.copy(
                                alpha = focusIndicatorAlpha.value,
                            )

                            drawLine(
                                color = colorA,
                                strokeWidth = 1f,
                                start = verticalLineWStart,
                                end = verticalLineWEnd,
                            )
                            drawLine(
                                color = colorB,
                                strokeWidth = 1f,
                                start = verticalLineBStart,
                                end = verticalLineBEnd,
                            )
                            drawLine(
                                color = colorA,
                                strokeWidth = 1f,
                                start = horizontalLineWStart,
                                end = horizontalLineWEnd,
                            )
                            drawLine(
                                color = colorB,
                                strokeWidth = 1f,
                                start = horizontalLineBStart,
                                end = horizontalLineBEnd,
                            )
                        }
                    }
            )
        }
    )
}

@Composable
private fun StampCutScreenScaffold(
    modifier: Modifier = Modifier,
    cutImage: ImageBitmap?,
    onCutAction: (
        viewfinderSize: Size,
        cutFrameRect: Rect,
    ) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    imageSource: @Composable BoxWithConstraintsScope.(
        modifier: Modifier,
        frameLayoutCoordinatesState: State<LayoutCoordinates?>,
    ) -> Unit,
) = BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val soundPool: SoundPool? = remember {
        runCatching {
            SoundPool.Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
        }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { soundPool?.release() }
    }

    val cutterInteractionSource = remember(::MutableInteractionSource)
    var cutterSize = DpSize(
        width = UiStampShapeA.size.width * 2.5f,
        height = UiStampShapeA.size.height * 2.8f,
    )
    val cutterToScreenSizeRatio =
        if (maxHeight < 300.dp || maxWidth < 300.dp)
            0.9f
        else
            0.7f
    val cutterSizeScale = min(
        maxWidth * cutterToScreenSizeRatio / cutterSize.width,
        maxHeight * cutterToScreenSizeRatio / cutterSize.height,
    )
    cutterSize *= cutterSizeScale

    val frameSize = UiStampShapeA.size * 1.5f * cutterSizeScale
    val frameLayoutCoordinatesState = remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }

    val playCutSound: (() -> Unit)? = remember(soundPool) {
        if (soundPool == null) {
            return@remember null
        }

        val cutSoundId =
            soundPool.load(context, R.raw.cut, 1)

        fun() {
            soundPool.play(
                cutSoundId,
                1f,
                1f,
                1,
                0,
                1f,
            )
        }
    }

    val cut = remember(onCutAction) {
        fun() {
            val frameLayoutCoordinates = frameLayoutCoordinatesState.value
                ?: return

            val viewfinderSize =
                frameLayoutCoordinates
                    .parentLayoutCoordinates!!
                    .size
                    .toSize()
            val frameRect = frameLayoutCoordinates.boundsInParent()

            onCutAction(viewfinderSize, frameRect)
        }
    }

    imageSource(
        Modifier
            .testTag("imageSource")
            .fillMaxSize()
            // Cut on long press.
            .pointerInput(
                cut,
            ) {
                // Have to detect long press this way
                // because image source consumes down first.
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                    )

                    val didLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull false
                            if (change.changedToUpIgnoreConsumed()) {
                                return@withTimeoutOrNull false
                            }
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                return@withTimeoutOrNull false
                            }
                        }
                        @Suppress("UNREACHABLE_CODE") true
                    } == null

                    if (!didLongPress) {
                        return@awaitEachGesture
                    }

                    // Animate and do the cut.
                    coroutineScope.launch {
                        val pressInteraction = PressInteraction.Press(down.position)
                        cutterInteractionSource.emit(pressInteraction)
                        if (playCutSound != null) {
                            withContext(Dispatchers.IO) {
                                playCutSound()
                            }
                        }
                        delay(50.milliseconds)
                        cut()
                        cutterInteractionSource.emit(
                            PressInteraction.Cancel(
                                pressInteraction
                            )
                        )
                    }
                }
            },
        frameLayoutCoordinatesState,
    )

    // TODO this doesn't work if it is not centered.
    Spacer(
        modifier = Modifier
            .testTag("frame")
            .size(frameSize)
            .run {
                if (cutImage == null) {
                    return@run this
                }

                background(Color.Black)
            }
            .onPlaced(frameLayoutCoordinatesState::value::set)
    )

    StampCutter(
        frameSize = frameSize,
        interactionSource = cutterInteractionSource,
        modifier = Modifier
            .requiredSize(cutterSize)
    )

    if (cutImage != null) {
        val rotation = remember {
            Animatable(0f)
        }
        val offsetX = remember {
            Animatable(0f)
        }
        val offsetY = remember {
            Animatable(0f)
        }
        val scale = remember {
            Animatable(1f)
        }

        Image(
            bitmap = cutImage,
            contentDescription = null,
            modifier = Modifier
                .size(frameSize)
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .optionalSharedElement(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    contentKey = "image",
                )
                .graphicsLayer {
                    rotationZ = rotation.value
                }
        )

        val hapticFeedback = LocalHapticFeedback.current

        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(
                HapticFeedbackType.Confirm
            )

            coroutineScope {
                launch {
                    rotation.animateTo(
                        targetValue = Random
                            .nextInt(-10, 10)
                            .toFloat(),
                    )
                }

                launch {
                    offsetX.animateTo(
                        targetValue = Random
                            .nextInt(-200, 200)
                            .toFloat(),
                    )
                }

                launch {
                    offsetY.animateTo(
                        targetValue = Random
                            .nextInt(-200, 200)
                            .toFloat(),
                    )
                }

                launch {
                    scale.animateTo(
                        targetValue = 1.1f,
                        animationSpec = tween(
                            durationMillis = 120,
                            easing = EaseInQuad,
                        ),
                    )
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 120,
                            easing = EaseOutQuad,
                        ),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun StampCutScreenScaffoldPreview(

) {
    StampCutScreenScaffold(
        cutImage = null,
        onCutAction = { _, _ -> },
        sharedTransitionScope = null,
        animatedVisibilityScope = null,
        imageSource = { modifier, _ ->
            Spacer(modifier.background(Color.Yellow))
        },
        modifier = Modifier
            .fillMaxSize()
    )
}
