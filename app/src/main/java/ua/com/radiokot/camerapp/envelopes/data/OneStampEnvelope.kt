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

@file:Suppress("unused")

package ua.com.radiokot.camerapp.envelopes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmall
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmallLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSquare
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCorners
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCornersLandscape
import java.time.ZonedDateTime

@Serializable
class OneStampEnvelopeManifest(
    val assets: List<Asset>,
    val createdAt: String,
    val envelopeColor: EnvelopeColor? = null,
    val message: String? = null,
    val packageID: String,
    val schemaVersion: Int,
    val sourceApplication: SourceApplication,
    val stamps: List<Stamp>,
) {
    @Serializable
    class EnvelopeColor(
        val alpha: Double,
        val blue: Double,
        val green: Double,
        val red: Double,
    )

    @Serializable
    class SourceApplication(
        val build: String,
        val bundleIdentifier: String,
        val name: String?,
        val version: String?,
    )

    @Serializable
    class Asset(
        val byteCount: Long,
        val fileName: String,
        val id: String,
        val role: Role? = null,
        val uniformTypeIdentifier: String,
    ) {
        @Serializable
        enum class Role {
            @SerialName("image")
            Image,

            @SerialName("previewImage")
            PreviewImage,
        }

    }

    @Serializable
    class Stamp(
        val id: String,
        val createdAt: String,
        val cropInfo: CropInfo,
        val imageAssetID: String,
        val previewImageAssetID: String,
        val stampShape: Shape,
        val title: String,
    ) {
        @Serializable
        class CropInfo(
            /**
             * Top left XY coordinates, then width and height.
             */
            val cropRectInImage: List<List<Double>>,
            /**
             * Width and height.
             */
            val imageSize: List<Int>,
        )

        @Serializable
        data class Shape(
            val kind: Kind? = null,
            val orientation: Orientation,
        ) {

            @Serializable
            enum class Kind {
                @SerialName("pressCutA")
                PressCutA,

                @SerialName("rectangleWithCorner")
                RectangleWithCorner,

                @SerialName("rectangleSmall")
                RectangleSmall,

                @SerialName("rectangle")
                Rectangle,

                @SerialName("square")
                Square,
            }

            @Serializable
            enum class Orientation {
                @SerialName("portrait")
                Portrait,

                @SerialName("landscape")
                Landscape,
            }
        }

        companion object {
            const val UNTITLED_TITLE = "Untitled"
        }
    }

    companion object {
        val JSON by lazy {
            Json {
                isLenient = true
                coerceInputValues = true
                ignoreUnknownKeys = true
            }
        }
    }
}

fun OneStampEnvelopeManifest.Stamp.toStamp(
    assetFileNamesById: Map<String, String>,
): Stamp {
    val previewAssetId = previewImageAssetID
    val fileName =
        assetFileNamesById[previewAssetId]
            ?: error("Stamp preview asset not found")
    val (shapeKind, shapeOrientation) = stampShape

    return Stamp(
        id = id,
        collectionId = "OneStampPackage",
        imageUri = "$OneStampEnvelopeAssetsDirectory/$fileName",
        caption = title.takeIf { it != OneStampEnvelopeManifest.Stamp.UNTITLED_TITLE },
        takenAtLocal =
            ZonedDateTime
                .parse(createdAt)
                .toLocalDateTime(),
        shape =
            when (shapeKind) {
                OneStampEnvelopeManifest.Stamp.Shape.Kind.PressCutA ->
                    StampShapeA

                OneStampEnvelopeManifest.Stamp.Shape.Kind.Square ->
                    StampShapeOneStampSquare

                OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner
                    if shapeOrientation == OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait ->
                    StampShapeOneStamp

                OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner ->
                    StampShapeOneStampLandscape

                OneStampEnvelopeManifest.Stamp.Shape.Kind.Rectangle
                    if shapeOrientation == OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait ->
                    StampShapeOneStampWithoutCorners

                OneStampEnvelopeManifest.Stamp.Shape.Kind.Rectangle ->
                    StampShapeOneStampWithoutCornersLandscape

                OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleSmall
                    if shapeOrientation == OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait ->
                    StampShapeOneStampSmall

                OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleSmall ->
                    StampShapeOneStampSmallLandscape

                null -> error("Unsupported stamp shape $shapeKind $shapeOrientation")
            }
    )
}

const val OneStampEnvelopeRootDirectory = "OneStamp-PackageStaging"
const val OneStampEnvelopeAssetsDirectory = "$OneStampEnvelopeRootDirectory/assets"
const val OneStampEnvelopeManifestFile = "$OneStampEnvelopeRootDirectory/manifest.json"
