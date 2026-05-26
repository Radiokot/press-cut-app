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

import com.ashampoo.kim.model.ImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import ua.com.radiokot.camerapp.BuildConfig
import ua.com.radiokot.camerapp.envelopes.data.OneStampEnvelopeManifest.Asset.Role
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSquare
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File
import java.io.OutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CreateOneStampEnvelopeUseCase(
    private val stampRepository: FsStampRepository,
) {

    private val log by lazyLogger("CreateOneStampEnvelopeUC")

    suspend operator fun invoke(
        message: String?,
        stampIds: Set<String>,
        outputStream: OutputStream,
    ): Unit = withContext(Dispatchers.IO) {

        with(ZipOutputStream(outputStream)) {

            log.debug {
                "invoke(): starting:" +
                        "\nstampIds=${stampIds.size}"
            }

            putNextEntry(
                ZipEntry("$OneStampEnvelopeRootDirectory/")
            )

            val stamps =
                stampRepository
                    .getStamps()
                    .filter { it.id in stampIds }
            val imageSizesByStamp = mutableMapOf<Stamp, ImageSize>()

            writeAssets(
                stamps = stamps,
                outImageSizesByStamp = imageSizesByStamp,
            )

            writeManifest(
                packageId = System.currentTimeMillis().toString(),
                message = message,
                stamps = stamps,
                imageSizesByStamp = imageSizesByStamp,
            )

            close()

            log.debug {
                "invoke(): done"
            }
        }
    }

    private suspend fun ZipOutputStream.writeManifest(
        packageId: String,
        message: String?,
        stamps: List<Stamp>,
        imageSizesByStamp: MutableMap<Stamp, ImageSize>,
    ) = withContext(Dispatchers.IO) {

        log.debug {
            "writeManifest(): writing"
        }

        putNextEntry(
            ZipEntry(OneStampEnvelopeManifestFile)
        )

        val manifest = OneStampEnvelopeManifest(
            packageID = packageId,
            schemaVersion = 1,
            createdAt = ZonedDateTime.now().toString(),
            envelopeColor = OneStampEnvelopeManifest.EnvelopeColor(
                alpha = 1.0,
                red = 1.0,
                green = 0.976,
                blue = 0.921,
            ),
            sourceApplication = OneStampEnvelopeManifest.SourceApplication(
                build = BuildConfig.VERSION_CODE.toString(),
                bundleIdentifier = BuildConfig.sharedEnvelopeContentProviderAuthority,
                name = "PressCut",
                version = BuildConfig.VERSION_NAME,
            ),
            message = message,
            assets = stamps.flatMap { stamp ->
                val stampFile = stampRepository.getStampFile(stamp)

                sequenceOf(
                    Role.Image,
                    Role.PreviewImage,
                ).map { role ->
                    OneStampEnvelopeManifest.Asset(
                        byteCount = stampFile.length(),
                        fileName =
                            getAssetFileName(
                                file = stampFile,
                                role = role,
                            ),
                        id =
                            getAssetId(
                                stampId = stamp.id,
                                role = role,
                            ),
                        role = role,
                        uniformTypeIdentifier = "public.${stampFile.extension}",
                    )
                }
            },
            stamps = stamps.map { stamp ->

                val imageSize = imageSizesByStamp[stamp]!!

                OneStampEnvelopeManifest.Stamp(
                    id = stamp.id,
                    createdAt = stamp
                        .takenAtLocal
                        .atZone(ZoneId.systemDefault())
                        .toString(),
                    previewImageAssetID =
                        getAssetId(
                            stampId = stamp.id,
                            role = Role.PreviewImage,
                        ),
                    imageAssetID =
                        getAssetId(
                            stampId = stamp.id,
                            role = Role.Image,
                        ),
                    title = stamp.caption ?: OneStampEnvelopeManifest.Stamp.UNTITLED_TITLE,
                    stampShape = when (stamp.shape) {
                        StampShapeA ->
                            OneStampEnvelopeManifest.Stamp.Shape(
                                kind = OneStampEnvelopeManifest.Stamp.Shape.Kind.PressCutA,
                                orientation = OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait,
                            )

                        StampShapeOneStamp ->
                            OneStampEnvelopeManifest.Stamp.Shape(
                                kind = OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner,
                                orientation = OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait,
                            )

                        StampShapeOneStampLandscape ->
                            OneStampEnvelopeManifest.Stamp.Shape(
                                kind = OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner,
                                orientation = OneStampEnvelopeManifest.Stamp.Shape.Orientation.Landscape,
                            )

                        StampShapeOneStampSquare ->
                            OneStampEnvelopeManifest.Stamp.Shape(
                                kind = OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner,
                                orientation = OneStampEnvelopeManifest.Stamp.Shape.Orientation.Landscape,
                            )
                    },
                    cropInfo = OneStampEnvelopeManifest.Stamp.CropInfo(
                        cropRectInImage = listOf(
                            listOf(
                                0.0,
                                0.0,
                            ),
                            listOf(
                                imageSize.width.toDouble(),
                                imageSize.height.toDouble(),
                            ),
                        ),
                        imageSize = listOf(
                            imageSize.width,
                            imageSize.height,
                        ),
                    ),
                )
            },
        )

        OneStampEnvelopeManifest.JSON.encodeToStream(manifest, this@writeManifest)

        closeEntry()
    }

    private suspend fun ZipOutputStream.writeAssets(
        stamps: List<Stamp>,
        outImageSizesByStamp: MutableMap<Stamp, ImageSize>,
    ): Unit = withContext(Dispatchers.IO) {

        log.debug {
            "writeAssets(): writing:" +
                    "\nstamps=${stamps.size}"
        }

        putNextEntry(
            ZipEntry("$OneStampEnvelopeAssetsDirectory/")
        )

        stamps.forEach { stamp ->
            val file = stampRepository.getStampFile(stamp)
            val (fileContent, imageSize) = stampRepository.getStampImageBytesAndSize(stamp)

            outImageSizesByStamp[stamp] = imageSize

            // Each file is written twice due to the current OneStamp requirement
            // to have different files for preview and image.
            sequenceOf(
                Role.PreviewImage,
                Role.Image,
            ).forEach { role ->
                putNextEntry(
                    ZipEntry(
                        "$OneStampEnvelopeAssetsDirectory/" +
                                getAssetFileName(
                                    file = file,
                                    role = role,
                                )
                    )
                )
                write(fileContent)
                closeEntry()
            }
        }
    }

    private fun getAssetId(
        stampId: String,
        role: Role,
    ): String =
        "$stampId-$role"

    private fun getAssetFileName(
        file: File,
        role: Role,
    ): String =
        "${file.nameWithoutExtension}-$role.${file.extension}"
}
