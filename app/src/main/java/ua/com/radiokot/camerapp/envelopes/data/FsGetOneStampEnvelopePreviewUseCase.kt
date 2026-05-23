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
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ua.com.radiokot.camerapp.envelopes.domain.GetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.envelopes.domain.StampEnvelopePreview
import ua.com.radiokot.camerapp.util.entries
import java.io.File
import java.util.zip.ZipInputStream

class FsGetOneStampEnvelopePreviewUseCase(
    private val contentResolver: ContentResolver,
    private val tempStampImageDirectory: File,
) : GetOneStampEnvelopePreviewUseCase {

    private val manifestJsonSerializer =
        Json {
            isLenient = true
            coerceInputValues = true
            ignoreUnknownKeys = true
        }

    override suspend operator fun invoke(
        oneStampPackageContentUri: Uri,
    ): StampEnvelopePreview = withContext(Dispatchers.IO) {

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

                    manifestJsonSerializer.decodeFromStream(zipInputStream)
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
                    runCatching {
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
                    }.getOrNull()
                }

        if (tempStampImageDirectory.exists()) {
            tempStampImageDirectory.deleteRecursively()
        }

        contentResolver
            .openInputStream(oneStampPackageContentUri)!!
            .buffered()
            .let(::ZipInputStream)
            .use { zipInputStream ->
                zipInputStream
                    .entries()
                    .filter { it.name in imagePathsToExtract }
                    .forEach { imageEntry ->
                        File(
                            tempStampImageDirectory,
                            imageEntry.name,
                        )
                            .also { it.parentFile?.mkdirs() }
                            .outputStream()
                            .use(zipInputStream::copyTo)
                        zipInputStream.closeEntry()
                    }
            }

        StampEnvelopePreview(
            message = manifest.message,
            someStamps = someStamps,
            stampCount = manifest.stamps.size,
        )
    }
}
