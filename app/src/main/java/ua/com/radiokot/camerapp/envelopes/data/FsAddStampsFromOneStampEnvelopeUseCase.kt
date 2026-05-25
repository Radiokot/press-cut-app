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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import ua.com.radiokot.camerapp.envelopes.domain.AddStampsFromOneStampEnvelopeUseCase
import ua.com.radiokot.camerapp.envelopes.domain.OneStampEnvelopePreviewResult
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.util.entries
import ua.com.radiokot.camerapp.util.lazyLogger
import java.util.zip.ZipInputStream

class FsAddStampsFromOneStampEnvelopeUseCase(
    private val stampRepository: FsStampRepository,
    private val contentResolver: ContentResolver,
) : AddStampsFromOneStampEnvelopeUseCase {

    private val log by lazyLogger("FsAddStampsFromOneStampEnvelopeUC")

    override operator fun invoke(
        collectionId: String,
        envelopePreview: OneStampEnvelopePreviewResult.Preview,
    ): Flow<Pair<Int, Int>> = flow {

        log.debug {
            "invoke(): starting:" +
                    "\ncollectionId=$collectionId"
        }

        val stampByImagePath =
            envelopePreview
                .allStamps
                .associateBy(Stamp::imageUri)

        val totalStampCount = stampByImagePath.size
        var addedStampCount = 0

        log.debug {
            "invoke(): adding stamps:" +
                    "\ncollectionId=$collectionId" +
                    "\ntotalStampCount=$totalStampCount"
        }

        contentResolver
            .openInputStream(envelopePreview.envelopeContentUri)!!
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
                                    webpBytes = zipInputStream.readBytes(),
                                    caption = stamp.caption,
                                    takenAtLocal = stamp.takenAtLocal,
                                    shape = stamp.shape,
                                )
                            } else {
                                stampRepository.addStamp(
                                    collectionId = collectionId,
                                    imageBitmap = BitmapFactory.decodeStream(zipInputStream),
                                    caption = stamp.caption,
                                    takenAtLocal = stamp.takenAtLocal,
                                    shape = stamp.shape,
                                )
                            }
                            addedStampCount++
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                log.error(e) {
                                    "invoke(): failed adding a stamp:" +
                                            "\nstamp=$stamp"
                                }
                            }
                            throw e
                        } finally {
                            zipInputStream.closeEntry()
                        }

                        emit(addedStampCount to totalStampCount)
                    }
            }

        log.info {
            "Added $addedStampCount stamps to the collection $collectionId"
        }
    }.flowOn(Dispatchers.IO)
}
