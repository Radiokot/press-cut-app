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

package ua.com.radiokot.camerapp.cut.domain

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.takePicture
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StampCutCameraImageSource(
    coroutineScope: CoroutineScope,
) : StampCutImageSource {

    private val log by lazyLogger("StampCutCameraImageSource")
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

    val useCaseGroup =
        UseCaseGroup.Builder()
            .addUseCase(previewUseCase)
            .addUseCase(captureUseCase)
            .build()

    val surfaceRequest: StateFlow<SurfaceRequest?> =
        callbackFlow {
            previewUseCase.setSurfaceProvider(::trySend)
            awaitClose { previewUseCase.surfaceProvider = null }
        }
            .stateIn(coroutineScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalAtomicApi::class)
    @SuppressLint("RestrictedApi")
    override fun capture(

    ): Flow<Bitmap> = channelFlow {

        coroutineScope {
            var hqSentAt = Long.MAX_VALUE

            // Get low quality capture from the preview first,
            // for immediate visual effect.
            launch(Dispatchers.Default) {
                val previewCapture = getPreviewCaptureOrNull()
                    ?: return@launch
                if (System.currentTimeMillis() < hqSentAt) {
                    channel.send(previewCapture)
                }
            }

            // Then make the high quality capture,
            // that takes some time.
            launch(Dispatchers.Default) {
                val hqCapture = getHighQualityCapture()
                hqSentAt = System.currentTimeMillis()
                channel.send(hqCapture)
            }
        }
    }

    @Suppress("SuspendCoroutineLacksCancellationGuarantees")
    @SuppressLint("RestrictedApi")
    private suspend fun getPreviewCaptureOrNull(

    ): Bitmap? = suspendCoroutine { continuation ->

        val previewResolutionInfo = previewUseCase.resolutionInfo!!
        val resultBitmap =
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

        PixelCopy.request(
            surfaceRequest.value!!.deferrableSurface.surface.get()!!,
            resultBitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    continuation.resume(resultBitmap)
                } else {
                    log.warn {
                        "getPreviewCapture(): PixelCopy failed:" +
                                "\ncopyResult=$copyResult"
                    }
                    continuation.resume(null)
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private suspend fun getHighQualityCapture(

    ) = withContext(Dispatchers.Default) {

        val captureImageProxy = captureUseCase.takePicture()
        val captureImageBitmap = captureImageProxy.toBitmap()
        val resultBitmap = Bitmap.createBitmap(
            captureImageBitmap,
            0, 0,
            captureImageProxy.width,
            captureImageProxy.height,
            Matrix().apply {
                setRotate(captureImageProxy.imageInfo.rotationDegrees.toFloat())
            },
            true
        )
        captureImageBitmap.recycle()
        captureImageProxy.close()

        return@withContext resultBitmap
    }
}
