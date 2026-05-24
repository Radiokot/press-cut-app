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
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import ua.com.radiokot.camerapp.envelopes.domain.AddStampsFromOneStampPackageUseCase
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.util.entries
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.InputStream
import java.util.zip.ZipInputStream

class FsAddStampsFromOneStampPackageUseCase(
    private val stampRepository: FsStampRepository,
    private val contentResolver: ContentResolver,
) : AddStampsFromOneStampPackageUseCase {

    private val log by lazyLogger("FsAddStampsFromOneStampPackageUC")

    override suspend operator fun invoke(
        collectionId: String,
        oneStampPackageContentUri: Uri,
    ): Unit = withContext(Dispatchers.IO) {

        val manifest: OneStampPackageManifest =
            contentResolver
                .openInputStream(oneStampPackageContentUri)!!
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

        val stampByImagePath =
            manifest
                .stamps
                .mapNotNull { oneStampPackageManifestStamp ->
                    try {
                        oneStampPackageManifestStamp.toStamp(
                            assetFileNamesById = assetFileNamesById,
                        )
                    } catch (e: Exception) {
                        ensureActive()
                        log.error(e) {
                            "invoke(): failed mapping a stamp"
                        }
                        null
                    }
                }
                .associateBy(Stamp::imageUri)

        var addedStamps = 0

        contentResolver
            .openInputStream(oneStampPackageContentUri)!!
            .buffered()
            .let(::ZipInputStream)
            .use { zipInputStream ->
                zipInputStream
                    .entries()
                    .filter { it.name in stampByImagePath }
                    .forEach { imageEntry ->
                        val stamp = stampByImagePath[imageEntry.name]!!

                        val isImageWebp = imageEntry.name.endsWith(
                            suffix = ".${FsStampRepository.WEBP_EXTENSION}",
                            ignoreCase = true,
                        )

                        try {
                            if (isImageWebp) {
                                stampRepository.addStamp(
                                    collectionId = collectionId,
                                    webpBytes = zipInputStream.use(InputStream::readBytes),
                                    caption = stamp.caption,
                                    takenAtLocal = stamp.takenAtLocal,
                                    shape = stamp.shape,
                                )
                            } else {
                                stampRepository.addStamp(
                                    collectionId = collectionId,
                                    imageBitmap = zipInputStream.use(BitmapFactory::decodeStream),
                                    caption = stamp.caption,
                                    takenAtLocal = stamp.takenAtLocal,
                                    shape = stamp.shape,
                                )
                            }
                            addedStamps++
                        } catch (e: Exception) {
                            ensureActive()
                            log.error(e) {
                                "invoke(): failed adding a stamp:" +
                                        "\nstamp=$stamp"
                            }
                        } finally {
                            zipInputStream.closeEntry()
                        }
                    }
            }

        log.info {
            "Added $addedStamps stamps to the collection $collectionId"
        }
    }
}
