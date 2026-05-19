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
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSquare
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
            StampShapeOneStamp -> UiStampShapeOneStamp
            StampShapeOneStampSquare -> UiStampShapeOneStampSquare
        }
    }
}

private class UiStampShapeImpl(
    shape: StampShape,
) : UiStampShape {
    override val size =
        DpSize(
            width = shape.size.width.dp,
            height = shape.size.height.dp,
        )

    override val fill: ImageVector by lazy {
        ImageVector.Builder(
            name = "${shape.name}.fill",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = shape.path,
                fill = SolidColor(Color.Black),
            )
            .build()
    }

    override val stroke: ImageVector by lazy {
        ImageVector.Builder(
            name = "${shape.name}.stroke",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = shape.path,
                stroke = SolidColor(Color.Red),
                strokeLineWidth = 0.4f,
            )
            .build()
    }
}

object UiStampShapeA : UiStampShape by UiStampShapeImpl(StampShapeA)
object UiStampShapeOneStamp : UiStampShape by UiStampShapeImpl(StampShapeOneStamp)
object UiStampShapeOneStampSquare : UiStampShape by UiStampShapeImpl(StampShapeOneStampSquare)

private class ShapeParameterProvider : PreviewParameterProvider<UiStampShape> {
    private val shapes = listOf(
        UiStampShapeA,
        UiStampShapeOneStamp,
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
