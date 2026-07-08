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

import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import ua.com.radiokot.camerapp.BuildConfig
import ua.com.radiokot.camerapp.envelopes.data.OneStampEnvelopeManifest.Asset.Role
import ua.com.radiokot.camerapp.envelopes.data.OneStampEnvelopeManifest.Stamp.Shape
import ua.com.radiokot.camerapp.envelopes.domain.CreateEnvelopeUseCase
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmall
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmallLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSquare
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCorners
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCornersLandscape
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FsCreateOneStampEnvelopeUseCase(
    private val applicationId: String,
    private val stampRepository: FsStampRepository,
) : CreateEnvelopeUseCase {

    private val log by lazyLogger("FsCreateOneStampEnvelopeUC")

    override suspend operator fun invoke(
        id: String,
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
            val imageSizesByStamp = mutableMapOf<Stamp, Size>()

            writeAssets(
                stamps = stamps,
                outImageSizesByStamp = imageSizesByStamp,
            )

            writeManifest(
                packageId = id,
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
        imageSizesByStamp: MutableMap<Stamp, Size>,
    ) = withContext(Dispatchers.IO) {

        log.debug {
            "writeManifest(): writing"
        }

        putNextEntry(
            ZipEntry(OneStampEnvelopeManifestFile)
        )

        val pressCutColor = OneStampEnvelopeManifest.ArgbColor(
            alpha = 1.0,
            red = 1.0,
            green = 0.9764705882,
            blue = 0.9215686275,
        )

        val manifest = OneStampEnvelopeManifest(
            packageID = packageId,
            schemaVersion = 1,
            createdAt = Instant
                .now()
                .atOffset(ZoneOffset.UTC),
            envelopeColor = pressCutColor,
            sourceApplication = OneStampEnvelopeManifest.SourceApplication(
                build = BuildConfig.VERSION_CODE.toString(),
                bundleIdentifier = applicationId,
                name = "Press-Cut",
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
                        .atOffset(ZoneOffset.UTC),
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
                    title = stamp.caption ?: OneStampEnvelopeManifest.Stamp.UNTITLED_STAMP_TITLE,
                    stampShape = when (stamp.shape) {
                        // Make the A shape appear in a rectangle & padding shape in OneStamp.
                        StampShapeA ->
                            Shape(
                                pressCutKind = Shape.Kind.PressCutA,
                                kind = Shape.Kind.RectangleAndPadding,
                                orientation = Shape.Orientation.Portrait,
                                paddingBackgroundColor = pressCutColor,
                            )

                        StampShapeOneStamp ->
                            Shape(
                                kind = Shape.Kind.RectangleWithCorner,
                                orientation = Shape.Orientation.Portrait,
                            )

                        StampShapeOneStampLandscape ->
                            Shape(
                                kind = Shape.Kind.RectangleWithCorner,
                                orientation = Shape.Orientation.Landscape,
                            )

                        StampShapeOneStampSquare ->
                            Shape(
                                kind = Shape.Kind.Square,
                                orientation = Shape.Orientation.Landscape,
                            )

                        StampShapeOneStampSmall ->
                            Shape(
                                kind = Shape.Kind.RectangleSmall,
                                orientation = Shape.Orientation.Portrait,
                            )

                        StampShapeOneStampSmallLandscape ->
                            Shape(
                                kind = Shape.Kind.RectangleSmall,
                                orientation = Shape.Orientation.Landscape,
                            )

                        StampShapeOneStampWithoutCorners ->
                            Shape(
                                kind = Shape.Kind.Rectangle,
                                orientation = Shape.Orientation.Portrait,
                            )

                        StampShapeOneStampWithoutCornersLandscape ->
                            Shape(
                                kind = Shape.Kind.Rectangle,
                                orientation = Shape.Orientation.Landscape,
                            )
                    },
                    cropInfo = OneStampEnvelopeCropInfo(
                        stampSize = imageSize,
                        paddingPercent =
                            // Add padding to make A shape look pretty in OneStamp
                            // when combined with rectangle & padding shape.
                            if (stamp.shape == StampShapeA)
                                0.01
                            else
                                0.0,
                    ),
                )
            },
        )

        OneStampEnvelopeManifest.JSON.encodeToStream(manifest, this@writeManifest)

        closeEntry()
    }

    private suspend fun ZipOutputStream.writeAssets(
        stamps: List<Stamp>,
        outImageSizesByStamp: MutableMap<Stamp, Size>,
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
