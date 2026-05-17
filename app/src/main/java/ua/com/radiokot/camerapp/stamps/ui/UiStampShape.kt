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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ua.com.radiokot.camerapp.stamps.domain.StampShape
import ua.com.radiokot.camerapp.stamps.domain.StampShapeA

sealed interface UiStampShape {

    val size: DpSize
    val fill: ImageVector
    val stroke: ImageVector

    companion object {

        fun fromShape(
            shape: StampShape,
        ): UiStampShape = when (shape) {

            StampShapeA -> UiStampShapeA
        }
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

private class ShapeParameterProvider : CollectionPreviewParameterProvider<UiStampShape>(
    listOf(
        UiStampShapeA,
    )
)

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
        BasicText(shape::class.simpleName!!)
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        BasicText("Fill")
        Image(
            imageVector = shape.fill,
            contentDescription = null,
        )
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        BasicText("Stroke")
        Image(
            imageVector = shape.stroke,
            contentDescription = null,
        )
    }
}
