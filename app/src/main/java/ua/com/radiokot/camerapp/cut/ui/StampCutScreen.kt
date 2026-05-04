package ua.com.radiokot.camerapp.cut.ui

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.stamps.ui.StampCutter
import ua.com.radiokot.camerapp.stamps.ui.StampSize
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Composable
fun StampCutScreen(
    useCases: Array<UseCase?>,
    surfaceRequest: SurfaceRequest?,
    cutImage: ImageBitmap?,
    onCutAction: (
        viewfinderSize: Size,
        cutFrameRect: Rect,
    ) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember {
        mutableStateOf<Camera?>(null)
    }
    val processCameraProvider by produceState<ProcessCameraProvider?>(null) {
        value = ProcessCameraProvider.awaitInstance(context).also {
            camera = it.bindToLifecycle(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                useCases = useCases,
            )
        }
    }
    val soundPool = remember {
        SoundPool.Builder()
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    DisposableEffect(Unit) {
        onDispose {
            processCameraProvider?.unbindAll()
            soundPool.release()
        }
    }

    var frameLayoutCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    val cutterInteractionSource = remember(::MutableInteractionSource)
    val focusIndicatorAlpha = remember {
        Animatable(0f)
    }

    if (surfaceRequest != null) {
        val coordinateTransformer = remember(::MutableCoordinateTransformer)
        val meteringFactory = remember(surfaceRequest) {
            SurfaceOrientedMeteringPointFactory(
                surfaceRequest.resolution.width.toFloat(),
                surfaceRequest.resolution.height.toFloat()
            )
        }
        val coroutineScope = rememberCoroutineScope()

        val focusAtCenter = remember(coordinateTransformer) {
            fun() {
                val camera = camera
                    ?: return
                val frameLayoutCoordinates = frameLayoutCoordinates
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

        val playCutSound = remember {
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
                val frameLayoutCoordinates = frameLayoutCoordinates
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

        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            coordinateTransformer = coordinateTransformer,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    focusAtCenter,
                    cut,
                ) {
                    detectTapGestures(
                        // Long press – cut, tap – focus.
                        // The cutter doesn't move until the long press is registered.
                        onPress = { pressPosition ->
                            val pressInteraction = PressInteraction.Press(pressPosition)
                            val longPress = coroutineScope.launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                cutterInteractionSource.emit(pressInteraction)
                                withContext(Dispatchers.IO) {
                                    playCutSound()
                                }
                                delay(50)
                                cut()
                            }
                            val releasedInMs = measureTimeMillis {
                                tryAwaitRelease()
                            }
                            if (releasedInMs < viewConfiguration.longPressTimeoutMillis) {
                                focusAtCenter()
                            }
                            longPress.cancel()
                            cutterInteractionSource.emit(PressInteraction.Cancel(pressInteraction))
                        },
                    )
                }
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            BasicText(
                text = "Opening the camera…",
                style = TextStyle(
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            )
        }
    }

    val frameSize = StampSize * 1.5f

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
            .onPlaced { layoutCoordinates ->
                frameLayoutCoordinates = layoutCoordinates
            }
            .graphicsLayer {
                alpha = focusIndicatorAlpha.value
            }
            .drawWithCache {
                // Focus indicator.

                val lineHalfLength = 32.dp.toPx()
                val verticalLineWStart = size.center + Offset(0f, -lineHalfLength)
                val verticalLineWEnd = size.center + Offset(0f, lineHalfLength)
                val verticalLineBStart = size.center + Offset(1f, -lineHalfLength)
                val verticalLineBEnd = size.center + Offset(1f, lineHalfLength)
                val horizontalLineWStart = size.center + Offset(-lineHalfLength, 0f)
                val horizontalLineWEnd = size.center + Offset(lineHalfLength, 0f)
                val horizontalLineBStart = size.center + Offset(-lineHalfLength, 1f)
                val horizontalLineBEnd = size.center + Offset(lineHalfLength, 1f)

                onDrawBehind {
                    drawLine(
                        color = Color.White,
                        strokeWidth = 1f,
                        start = verticalLineWStart,
                        end = verticalLineWEnd,
                    )
                    drawLine(
                        color = Color.Black,
                        strokeWidth = 1f,
                        start = verticalLineBStart,
                        end = verticalLineBEnd,
                    )
                    drawLine(
                        color = Color.White,
                        strokeWidth = 1f,
                        start = horizontalLineWStart,
                        end = horizontalLineWEnd,
                    )
                    drawLine(
                        color = Color.Black,
                        strokeWidth = 1f,
                        start = horizontalLineBStart,
                        end = horizontalLineBEnd,
                    )
                }
            }
    )

    StampCutter(
        frameSize = frameSize,
        interactionSource = cutterInteractionSource,
        modifier = Modifier
            .requiredWidth(StampSize.width * 2.5f)
            .requiredHeight(StampSize.height * 2.8f)
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
                .run {
                    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                        return@run this
                    }

                    with(sharedTransitionScope) {
                        sharedElement(
                            sharedContentState = rememberSharedContentState("image"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                }
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun StampCutScreenPreview(

) {
    StampCutScreen(
        useCases = emptyArray(),
        surfaceRequest = null,
        cutImage = null,
        onCutAction = { _, _ -> },
        sharedTransitionScope = null,
        animatedVisibilityScope = null,
        modifier = Modifier
            .fillMaxSize()
    )
}
