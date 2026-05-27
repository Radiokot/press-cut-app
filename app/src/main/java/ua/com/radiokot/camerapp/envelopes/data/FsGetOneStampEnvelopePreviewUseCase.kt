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

@file:OptIn(ExperimentalSerializationApi::class)

package ua.com.radiokot.camerapp.envelopes.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import ua.com.radiokot.camerapp.envelopes.domain.GetEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.envelopes.domain.EnvelopePreviewResult
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.util.entries
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File
import java.util.zip.ZipInputStream

class FsGetOneStampEnvelopePreviewUseCase(
    private val contentResolver: ContentResolver,
    private val tempStampImageDirectory: File,
) : GetEnvelopePreviewUseCase {

    private val log by lazyLogger("FsGetOneStampEnvelopePreviewUC")

    override suspend operator fun invoke(
        envelopeContentUri: Uri,
        maxPreviewStampCount: Int,
    ): EnvelopePreviewResult = withContext(Dispatchers.IO) {

        log.debug {
            "invoke(): starting:" +
                    "\noneStampEnvelopeContentUri=$envelopeContentUri"
        }

        val manifest: OneStampEnvelopeManifest =
            try {
                contentResolver
                    .openInputStream(envelopeContentUri)!!
                    .buffered()
                    .let(::ZipInputStream)
                    .use { zipInputStream ->
                        zipInputStream
                            .entries()
                            .find { it.name == OneStampEnvelopeManifestFile }
                            ?: error("Manifest not found")

                        OneStampEnvelopeManifest.JSON.decodeFromStream(zipInputStream)
                    }
            } catch (e: Exception) {
                ensureActive()
                log.error(e) {
                    "invoke(): failed reading the manifest"
                }
                return@withContext EnvelopePreviewResult.Error.Malformed(
                    reason = e.message ?: e.toString(),
                )
            }

        val assetFileNamesById =
            manifest
                .assets
                .associate { it.id to it.fileName }

        val supportedStamps =
            manifest
                .stamps
                .mapNotNull { oneStampStamp ->
                    try {
                        oneStampStamp.toStamp(
                            assetFileNamesById = assetFileNamesById,
                        )
                    } catch (e: Exception) {
                        ensureActive()
                        log.warn(e) {
                            "invoke(): failed mapping a stamp"
                        }
                        null
                    }
                }

        if (supportedStamps.isEmpty()) {
            return@withContext EnvelopePreviewResult.Error.NoSupportedStamps
        }

        val stampImagePaths =
            supportedStamps
                .mapTo(mutableSetOf(), Stamp::imageUri)
        val previewStampImagePaths = mutableSetOf<String>()

        val previewStamps =
            supportedStamps
                .take(maxPreviewStampCount)
                .map { stamp ->
                    previewStampImagePaths += stamp.imageUri
                    stamp.copy(
                        newImageUri =
                            "file://${tempStampImageDirectory.absolutePath}/${stamp.imageUri}",
                    )
                }

        if (tempStampImageDirectory.exists()) {
            log.debug {
                "invoke(): clearing the temp directory"
            }

            tempStampImageDirectory.deleteRecursively()
        }

        try {
            var extractedImageCount = 0
            var encounteredImageCount = 0

            contentResolver
                .openInputStream(envelopeContentUri)!!
                .buffered()
                .let(::ZipInputStream)
                .use { zipInputStream ->
                    zipInputStream
                        .entries()
                        .forEach { imageEntry ->
                            val imagePath = imageEntry.name

                            if (imagePath in stampImagePaths) {
                                encounteredImageCount++
                            }

                            if (imagePath !in previewStampImagePaths) {
                                return@forEach
                            }

                            try {
                                File(
                                    tempStampImageDirectory,
                                    imagePath,
                                )
                                    .also { it.parentFile?.mkdirs() }
                                    .outputStream()
                                    .use(zipInputStream::copyTo)

                                extractedImageCount++
                            } finally {
                                zipInputStream.closeEntry()
                            }
                        }
                }

            check(
                extractedImageCount == previewStampImagePaths.size
                        && encounteredImageCount == stampImagePaths.size
            ) {
                "Some images are missing"
            }
        } catch (e: Exception) {
            ensureActive()
            log.warn(e) {
                "invoke(): failed extracting preview stamp images"
            }
            return@withContext EnvelopePreviewResult.Error.Malformed(
                reason = e.message ?: e.toString(),
            )
        }

        return@withContext EnvelopePreviewResult.Preview(
            message = manifest.message,
            previewStamps = previewStamps,
            assetFileNamesById = assetFileNamesById,
            allStamps = supportedStamps,
            envelopeContentUri = envelopeContentUri,
        ).also {
            log.debug {
                "invoke(): got the preview:" +
                        "\nmessage=${manifest.message}" +
                        "\nstampCount=${supportedStamps.size}"
            }
        }
    }
}
