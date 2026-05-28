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

@file:Suppress("unused", "FunctionName")
@file:OptIn(ExperimentalSerializationApi::class)

package ua.com.radiokot.camerapp.envelopes.data

import com.ashampoo.kim.model.ImageSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmall
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmallLandscape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSquare
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCorners
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCornersLandscape
import ua.com.radiokot.camerapp.util.Iso8601OffsetDateTimeSerializer
import java.time.OffsetDateTime
import kotlin.math.abs

@Serializable
class OneStampEnvelopeManifest(
    val assets: List<Asset>,
    @Serializable(with = Iso8601OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime,
    val envelopeColor: ArgbColor? = null,
    val message: String? = null,
    val packageID: String,
    val schemaVersion: Int,
    val sourceApplication: SourceApplication,
    val stamps: List<Stamp>,
) {
    @Serializable
    class ArgbColor(
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
        @Serializable(with = Iso8601OffsetDateTimeSerializer::class)
        val createdAt: OffsetDateTime,
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
            val pressCutKind: Kind? = null,
            val orientation: Orientation,
            val paddingBackgroundColor: ArgbColor? = null,
        ) {

            @Serializable
            enum class Kind {
                @SerialName("pressCutA")
                PressCutA,

                @SerialName("rectangleWithCorner")
                @JsonNames(
                    "rectangleWithCornerAndPadding",
                )
                RectangleWithCorner,

                @SerialName("rectangleSmall")
                @JsonNames(
                    "rectangleSmallAndPadding",
                )
                RectangleSmall,

                @SerialName("rectangle")
                Rectangle,

                @SerialName("rectangleAndPadding")
                RectangleAndPadding,


                @SerialName("square")
                @JsonNames(
                    "squareAndPadding",
                )
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
            const val UNTITLED_STAMP_TITLE = "Untitled"
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
    val (oneStampShapeKind, pressCutShapeKind, shapeOrientation) = stampShape
    val shapeKind = pressCutShapeKind ?: oneStampShapeKind

    return Stamp(
        id = id,
        collectionId = "OneStampPackage",
        imageUri = "$OneStampEnvelopeAssetsDirectory/$fileName",
        caption = title.takeIf { it != OneStampEnvelopeManifest.Stamp.UNTITLED_STAMP_TITLE },
        takenAtLocal = createdAt.toLocalDateTime(),
        shape = when (shapeKind) {
            OneStampEnvelopeManifest.Stamp.Shape.Kind.PressCutA ->
                StampShapeA

            OneStampEnvelopeManifest.Stamp.Shape.Kind.Square ->
                StampShapeOneStampSquare

            OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner
                if shapeOrientation == OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait ->
                StampShapeOneStamp

            OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleWithCorner ->
                StampShapeOneStampLandscape

            OneStampEnvelopeManifest.Stamp.Shape.Kind.Rectangle,
            OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleAndPadding,
                ->
                when (shapeOrientation) {
                    OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait ->
                        StampShapeOneStampWithoutCorners

                    OneStampEnvelopeManifest.Stamp.Shape.Orientation.Landscape ->
                        StampShapeOneStampWithoutCornersLandscape
                }

            OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleSmall
                if shapeOrientation == OneStampEnvelopeManifest.Stamp.Shape.Orientation.Portrait ->
                StampShapeOneStampSmall

            OneStampEnvelopeManifest.Stamp.Shape.Kind.RectangleSmall ->
                StampShapeOneStampSmallLandscape

            null -> error("Unsupported stamp shape $shapeKind $shapeOrientation")
        }
    )
}

fun OneStampEnvelopeCropInfo(
    stampSize: ImageSize,
    paddingPercent: Double = 0.0,
): OneStampEnvelopeManifest.Stamp.CropInfo {

    val widthD = stampSize.width.toDouble()
    val heightD = stampSize.height.toDouble()

    return OneStampEnvelopeManifest.Stamp.CropInfo(
        cropRectInImage = listOf(
            listOf(
                widthD * paddingPercent,
                heightD * paddingPercent,
            ),
            listOf(
                widthD * (1 - paddingPercent * 2),
                heightD * (1 - paddingPercent * 2),
            )
        ),
        imageSize = listOf(
            stampSize.width,
            stampSize.height,
        )
    )
}

const val OneStampEnvelopeRootDirectory = "OneStamp-PackageStaging"
const val OneStampEnvelopeAssetsDirectory = "$OneStampEnvelopeRootDirectory/assets"
const val OneStampEnvelopeManifestFile = "$OneStampEnvelopeRootDirectory/manifest.json"
