package ua.com.radiokot.camerapp.cut.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.takePicture
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.toPath
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.times
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.stamps.ui.StampShapeA
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import ua.com.radiokot.camerapp.util.map
import kotlin.math.min

@Immutable
class StampCutScreenViewModel : ViewModel() {

    private val log by lazyLogger("StampCutScreenVM")

    private val previewUseCase =
        Preview.Builder().build()

    @SuppressLint("UnsafeOptInUsageError")
    private val captureUseCase =
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
            .setTargetRotation(Surface.ROTATION_0)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(1920, 1920),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .build()

    val useCases: Array<UseCase?> =
        arrayOf(
            previewUseCase,
            captureUseCase,
        )

    val surfaceRequest: StateFlow<SurfaceRequest?> =
        callbackFlow {
            previewUseCase.setSurfaceProvider(::trySend)
            awaitClose { previewUseCase.surfaceProvider = null }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _cutBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val cutImage: StateFlow<ImageBitmap?> =
        _cutBitmap
            .map(viewModelScope) { bitmap ->
                bitmap?.asImageBitmap()
            }

    private val _events: MutableSharedFlow<Event> = eventSharedFlow()
    val events: SharedFlow<Event> = _events

    private var cutJob: Job? = null

    fun onCutAction(
        visibleViewfinderSize: Size,
        visibleFrameRect: Rect,
    ) {
        if (cutJob?.isActive == true) {
            return
        }

        log.debug {
            "onCutAction(): starting the cut sequence:" +
                    "\nvisibleViewfinderSize: $visibleViewfinderSize" +
                    "\nvisibleFrameRect: $visibleFrameRect"
        }

        cutJob = viewModelScope.launch {
            doCut(
                visibleViewfinderRect =
                    RectF(0f, 0f, visibleViewfinderSize.width, visibleViewfinderSize.height),
                visibleFrameRect =
                    RectF(
                        visibleFrameRect.left,
                        visibleFrameRect.top,
                        visibleFrameRect.right,
                        visibleFrameRect.bottom
                    ),
            )
        }
    }


    @SuppressLint("RestrictedApi")
    private suspend fun doCut(
        visibleViewfinderRect: RectF,
        visibleFrameRect: RectF,
    ) = withContext(Dispatchers.Default) {

        // Get low quality cut from the preview first,
        // for immediate visual effect.

        val previewResolutionInfo = previewUseCase.resolutionInfo!!
        val previewImageBitmap =
            createBitmap(
                width =
                    if (previewResolutionInfo.rotationDegrees == 0
                        || previewResolutionInfo.rotationDegrees == 180
                    )
                        previewResolutionInfo.resolution.width
                    else
                        previewResolutionInfo.resolution.height,
                height =
                    if (previewResolutionInfo.rotationDegrees == 0
                        || previewResolutionInfo.rotationDegrees == 180
                    )
                        previewResolutionInfo.resolution.height
                    else
                        previewResolutionInfo.resolution.width,
            )
        withContext(Dispatchers.IO) {
            PixelCopy.request(
                surfaceRequest.value!!.deferrableSurface.surface.get()!!,
                previewImageBitmap,
                {
                    _cutBitmap.value = frameImage(
                        image = previewImageBitmap,
                        visibleViewfinderRect = visibleViewfinderRect,
                        visibleFrameRect = visibleFrameRect,
                    )
                    previewImageBitmap.recycle()
                },
                Handler(Looper.getMainLooper()),
            )
        }

        // Then make the actual cut from a high-res capture,
        // that takes some time.

        val captureImageProxy = captureUseCase.takePicture()
        val captureImageBitmap = captureImageProxy.toBitmap()
        val rotatedCaptureImageBitmap = Bitmap.createBitmap(
            captureImageBitmap,
            0, 0,
            captureImageProxy.width,
            captureImageProxy.height,
            Matrix().apply {
                setRotate(captureImageProxy.imageInfo.rotationDegrees.toFloat())
            },
            true
        )

        _events.emit(
            Event.DidCut(
                stampImageBitmap = frameImage(
                    image = rotatedCaptureImageBitmap,
                    visibleViewfinderRect = visibleViewfinderRect,
                    visibleFrameRect = visibleFrameRect,
                ),
            )
        )

        captureImageProxy.close()
        captureImageBitmap.recycle()
        rotatedCaptureImageBitmap.recycle()
    }

    fun onScreenDisposed() {
        log.debug {
            "onScreenDisposed(): clearing the cut"
        }

        cutJob?.cancel()
        _cutBitmap.value?.recycle()
        _cutBitmap.value = null
    }

    private fun frameImage(
        image: Bitmap,
        visibleViewfinderRect: RectF,
        visibleFrameRect: RectF,
    ): Bitmap {
        val frameScale = min(
            image.width / visibleViewfinderRect.width(),
            image.height / visibleViewfinderRect.height(),
        )
        val scaledViewfinderRect = visibleViewfinderRect * frameScale
        val scaledFrameRect = visibleFrameRect * frameScale

        val resultBitmap = createBitmap(
            width = scaledFrameRect.width().toInt(),
            height = scaledFrameRect.height().toInt(),
        )

        val imageShaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                image,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP,
            ).apply {
                setLocalMatrix(Matrix().apply {
                    setTranslate(
                        -(image.width - scaledViewfinderRect.width()) / 2f
                                - scaledFrameRect.left,
                        -(image.height - scaledViewfinderRect.height()) / 2f
                                - scaledFrameRect.top,
                    )
                })
            }
            isAntiAlias = true
        }

        val stampPath =
            StampShapeA
                .path
                .toPath()
                .asAndroidPath()
                .apply {
                    transform(Matrix().apply {
                        val scale = resultBitmap.width / StampShapeA.size.width.value
                        setScale(scale, scale)
                    })
                }

        resultBitmap.applyCanvas {
            drawPath(stampPath, imageShaderPaint)
        }

        return resultBitmap
    }

    sealed interface Event {
        class DidCut(
            val stampImageBitmap: Bitmap,
        ) : Event
    }
}
