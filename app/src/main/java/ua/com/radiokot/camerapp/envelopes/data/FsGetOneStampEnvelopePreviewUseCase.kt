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
import ua.com.radiokot.camerapp.envelopes.domain.GetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.envelopes.domain.StampEnvelopePreview
import ua.com.radiokot.camerapp.util.entries
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File
import java.util.zip.ZipInputStream

class FsGetOneStampEnvelopePreviewUseCase(
    private val contentResolver: ContentResolver,
    private val tempStampImageDirectory: File,
) : GetOneStampEnvelopePreviewUseCase {

    private val log by lazyLogger("FsGetOneStampEnvelopePreviewUC")

    override suspend operator fun invoke(
        oneStampEnvelopeContentUri: Uri,
    ): StampEnvelopePreview = withContext(Dispatchers.IO) {

        val manifest: OneStampPackageManifest =
            contentResolver
                .openInputStream(oneStampEnvelopeContentUri)!!
                .buffered()
                .let(::ZipInputStream)
                .use { zipInputStream ->
                    zipInputStream
                        .entries()
                        .find { it.name == OneStampPackageManifestFile }
                        ?: error("Manifest not found")

                    OneStampPackageManifest.JSON.decodeFromStream(zipInputStream)
                }

        val assetFileNamesById =
            manifest
                .assets
                .associate { it.id to it.fileName }

        val imagePathsToExtract = mutableSetOf<String>()

        val someStamps =
            manifest
                .stamps
                .take(3)
                .mapNotNull { oneStampPackageManifestStamp ->
                    try {
                        val stamp =
                            oneStampPackageManifestStamp
                                .toStamp(
                                    assetFileNamesById = assetFileNamesById,
                                )
                        imagePathsToExtract += stamp.imageUri
                        stamp.copy(
                            newImageUri =
                                "file://${tempStampImageDirectory.absolutePath}/${stamp.imageUri}",
                        )
                    } catch (e: Exception) {
                        ensureActive()
                        log.error(e) {
                            "invoke(): failed mapping a stamp"
                        }
                        null
                    }
                }

        if (tempStampImageDirectory.exists()) {
            log.debug {
                "invoke(): clearing the temp directory"
            }

            tempStampImageDirectory.deleteRecursively()
        }

        contentResolver
            .openInputStream(oneStampEnvelopeContentUri)!!
            .buffered()
            .let(::ZipInputStream)
            .use { zipInputStream ->
                zipInputStream
                    .entries()
                    .filter { it.name in imagePathsToExtract }
                    .forEach { imageEntry ->
                        try {
                            File(
                                tempStampImageDirectory,
                                imageEntry.name,
                            )
                                .also { it.parentFile?.mkdirs() }
                                .outputStream()
                                .use(zipInputStream::copyTo)
                        } catch (e: Exception) {
                            ensureActive()
                            log.error(e) {
                                "invoke(): failed extracting an image:" +
                                        "\nname=${imageEntry.name}"
                            }
                        } finally {
                            zipInputStream.closeEntry()
                        }
                    }
            }

        StampEnvelopePreview(
            message = manifest.message,
            someStamps = someStamps,
            stampCount = manifest.stamps.size,
        )
    }
}
