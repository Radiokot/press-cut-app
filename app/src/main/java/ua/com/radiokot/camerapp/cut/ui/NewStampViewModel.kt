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

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import ua.com.radiokot.camerapp.cut.render.StampCutBitmapImageRenderer
import ua.com.radiokot.camerapp.cut.render.StampCutCameraImageRenderer
import ua.com.radiokot.camerapp.cut.render.StampCutImageRenderer
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.util.ImageFileDecoder
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File

class NewStampViewModel(
    private val contentResolver: ContentResolver,
    private val receivedImageCacheDirectory: File,
    private val imageFileDecoder: ImageFileDecoder,
) : ViewModel() {

    private val log by lazyLogger("NewStampVM")

    private var isInitialized = false
    private val cachedImageFile = File(receivedImageCacheDirectory, "image")

    lateinit var collectionId: String
        private set

    var showToastOnSave: Boolean = true
        private set

    lateinit var imageRenderer: StampCutImageRenderer
        private set

    var stampImageBitmap: Bitmap? = null

    val isCameraRequired: Boolean
        get() = imageRenderer is StampCutCameraImageRenderer

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    fun initOnce(
        collectionId: String?,
        showToastOnSave: Boolean,
        receivedImageUri: Uri?,
    ) {
        if (isInitialized) {
            return
        }

        this.collectionId = collectionId ?: StampCollection.PRIMARY_ID
        this.showToastOnSave = showToastOnSave

        if (receivedImageUri != null) {
            val (bitmapRenderer, bitmapRendererCreationError) =
                createBitmapRenderer(
                    imageUri = receivedImageUri,
                )

            this.imageRenderer = bitmapRenderer ?: createCameraRenderer()

            when (bitmapRendererCreationError) {
                null -> {}

                BitmapRendererCreationError.FileTooBig -> {
                    events.tryEmit(Event.ShowImageFileTooBigMessage)
                }

                BitmapRendererCreationError.ImageNotDecoding,
                BitmapRendererCreationError.IoError,
                    -> {
                    events.tryEmit(Event.ShowImageReadErrorMessage)
                }
            }
        } else {
            this.imageRenderer = createCameraRenderer()
        }

        log.debug {
            "initOnce(): initialized:" +
                    "\ncollectionId=$collectionId" +
                    "\nshowToastOnSave=$showToastOnSave" +
                    "\nimageUri=$receivedImageUri" +
                    "\nimageRenderer=${this.imageRenderer}"
        }

        isInitialized = true
    }

    private fun createCameraRenderer(

    ) = StampCutCameraImageRenderer(
        coroutineScope = viewModelScope,
    )

    private fun createBitmapRenderer(
        imageUri: Uri,
    ): Pair<StampCutBitmapImageRenderer?, BitmapRendererCreationError?> = runBlocking {

        if (!receivedImageCacheDirectory.exists()) {
            if (!receivedImageCacheDirectory.mkdirs()) {
                log.error {
                    "createBitmapRendererOrNull(): failed to create cache directory:" +
                            "\nreceivedImageCacheDirectory=$receivedImageCacheDirectory"
                }
            }
            return@runBlocking null to BitmapRendererCreationError.IoError
        }

        val imageInputStream = try {
            contentResolver.openInputStream(imageUri)!!
        } catch (e: Exception) {
            log.error(e) {
                "createBitmapRendererOrNull(): failed to open image input stream:" +
                        "\nimageUri=$imageUri"
            }
            return@runBlocking null to BitmapRendererCreationError.IoError
        }

        var imageSizeBytes = 0L
        try {
            cachedImageFile
                .sink()
                .buffer()
                .use { cachedImageFileSink ->
                    imageSizeBytes =
                        imageInputStream
                            .source()
                            .buffer()
                            .use(cachedImageFileSink::writeAll)
                }
        } catch (e: Exception) {
            log.error(e) {
                "createBitmapRendererOrNull(): failed to copy image to cache:" +
                        "\nimageUri=$imageUri" +
                        "\nreceivedImageCacheDirectory=$receivedImageCacheDirectory"
            }
            cachedImageFile.delete()
            return@runBlocking null to BitmapRendererCreationError.IoError
        } finally {
            imageInputStream.close()
        }

        if (imageSizeBytes > MAX_IMAGE_FILE_SIZE_BYTES) {
            log.debug {
                "createBitmapRendererOrNull(): image file is too big:" +
                        "\nimageSizeBytes=$imageSizeBytes"
            }
            cachedImageFile.delete()
            return@runBlocking null to BitmapRendererCreationError.FileTooBig
        }

        val image = try {
            imageFileDecoder
                .invoke(
                    filePath = cachedImageFile.absolutePath,
                    maxWidth = 2 * StampCutBitmapImageRenderer.HQ_SCENE_SIZE.toInt(),
                    maxHeight = 2 * StampCutBitmapImageRenderer.HQ_SCENE_SIZE.toInt(),
                )
                .asImageBitmap()
        } catch (e: Exception) {
            log.error(e) {
                "createBitmapRendererOrNull(): failed to decode image:" +
                        "\nimageUri=$imageUri" +
                        "\ncachedImageFile=$cachedImageFile"
            }
            cachedImageFile.delete()
            return@runBlocking null to BitmapRendererCreationError.ImageNotDecoding
        }

        return@runBlocking StampCutBitmapImageRenderer(
            image = image,
            coroutineScope = viewModelScope,
        ) to null
    }

    private enum class BitmapRendererCreationError {
        FileTooBig,
        ImageNotDecoding,
        IoError,
    }

    override fun onCleared() {
        cachedImageFile.delete()
    }

    sealed interface Event {
        object ShowImageFileTooBigMessage : Event
        object ShowImageReadErrorMessage : Event
    }

    private companion object {
        private const val MAX_IMAGE_FILE_SIZE_BYTES = 16 * 1024 * 1024
    }
}
