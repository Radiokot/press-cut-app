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

package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.core.ImageRequest
import ua.com.radiokot.camerapp.stamps.domain.StampShape
import ua.com.radiokot.camerapp.stamps.domain.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.StampShapeOneStampSquare
import ua.com.radiokot.camerapp.util.noProgressive
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

@Immutable
interface UiStampShape {

    val size: DpSize
    val fill: ImageVector
    val stroke: ImageVector

    fun getListImageLoadingOptions(
        density: Density,
    ): StampImageLoadingOptions =
        imageLoadingOptionsCache.computeIfAbsent(
            getImageLoadingOptionsCacheKey(
                size = size,
                density = density,
                isList = true,
            )
        ) {
            StampImageLoadingOptions(
                imageSize = IntSize(
                    width = (UiStampShapeA.size.width.value * density.density).toInt(),
                    height = (UiStampShapeA.size.height.value * density.density).toInt(),
                )
            )
        }

    fun getPreviewImageLoadingOptions(
        density: Density,
    ): StampImageLoadingOptions =
        imageLoadingOptionsCache.computeIfAbsent(
            getImageLoadingOptionsCacheKey(
                size = size,
                density = density,
                isList = false,
            )
        ) {
            StampImageLoadingOptions(
                imageSize = IntSize(
                    width = (UiStampShapeA.size.width.value * 2f * density.density).toInt(),
                    height = (UiStampShapeA.size.height.value * 2f * density.density).toInt(),
                )
            )
        }

    companion object {
        private val imageLoadingOptionsCache: MutableMap<Int, StampImageLoadingOptions> =
            ConcurrentHashMap(2)

        private fun getImageLoadingOptionsCacheKey(
            size: DpSize,
            density: Density,
            isList: Boolean,
        ): Int =
            Objects.hash(
                size,
                density.density,
                isList,
            )

        fun fromShape(
            shape: StampShape,
        ): UiStampShape = when (shape) {
            StampShapeA -> UiStampShapeA
            StampShapeOneStampSquare -> UiStampShapeOneStampSquare
        }
    }
}

@Immutable
class StampImageLoadingOptions(
    imageSize: IntSize,
) {
    val imageOptions: ImageOptions =
        ImageOptions(
            requestSize = imageSize,
        )

    val requestBuilder: ImageRequest.Builder.() -> Unit =
        noProgressive(
            size = imageSize,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StampImageLoadingOptions) return false

        if (imageOptions != other.imageOptions) return false

        return true
    }

    override fun hashCode(): Int {
        return imageOptions.hashCode()
    }
}

object UiStampShapeA : UiStampShape {

    override val size = DpSize(
        width = StampShapeA.size.width.dp,
        height = StampShapeA.size.height.dp,
    )

    override val fill: ImageVector by lazy {
        ImageVector.Builder(
            name = "StampA.Fill",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = StampShapeA.path,
                fill = SolidColor(Color.Black),
            )
            .build()
    }

    override val stroke: ImageVector by lazy {
        ImageVector.Builder(
            name = "StampA.Stroke",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = StampShapeA.path,
                stroke = SolidColor(Color.Red),
                strokeLineWidth = 0.4f,
            )
            .build()
    }
}

object UiStampShapeOneStampSquare : UiStampShape {

    override val size = DpSize(
        width = StampShapeOneStampSquare.size.width.dp,
        height = StampShapeOneStampSquare.size.height.dp,
    )

    override val fill: ImageVector by lazy {
        ImageVector.Builder(
            name = "OneStampSquare.Fill",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = StampShapeOneStampSquare.path,
                fill = SolidColor(Color.Black),
            )
            .build()
    }

    override val stroke: ImageVector by lazy {
        ImageVector.Builder(
            name = "OneStampSquare.Stroke",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = StampShapeOneStampSquare.path,
                stroke = SolidColor(Color.Red),
                strokeLineWidth = 0.4f,
            )
            .build()
    }
}

private class ShapeParameterProvider : PreviewParameterProvider<UiStampShape> {
    private val shapes = listOf(
        UiStampShapeA,
        UiStampShapeOneStampSquare,
    )

    override val values: Sequence<UiStampShape>
        get() = shapes.asSequence()

    override fun getDisplayName(index: Int): String? =
        shapes[index]::class.simpleName
}

@Preview
@Composable
private fun UiStampShapePreview(
    @PreviewParameter(
        provider = ShapeParameterProvider::class,
    )
    shape: UiStampShape,
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        BasicText("Fill")
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(StampContainerBaseSize)
                .background(Color.Yellow)
        ) {
            Image(
                imageVector = shape.fill,
                contentDescription = null,
            )
        }
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        BasicText("Stroke")
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(StampContainerBaseSize)
                .background(Color.Yellow)
        ) {
            Image(
                imageVector = shape.stroke,
                contentDescription = null,
            )
        }
    }
}
