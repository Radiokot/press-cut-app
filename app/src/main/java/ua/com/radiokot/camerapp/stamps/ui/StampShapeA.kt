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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

object StampShapeA {

    // https://composables.com/svg-to-compose
    val path: List<PathNode> = PathBuilder().apply {
        moveTo(100f, 121.1f)
        verticalLineTo(115f)
        curveToRelative(-4f, 0.4f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4.4f, 0f, -3.7f, -6.7f, 0f, -6.2f)
        verticalLineTo(90f)
        curveToRelative(-4.1f, 0.4f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-3.8f, 0.4f, -4.4f, -6.2f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4f, 0.4f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4.4f, 0f, -3.7f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-3.8f, 0.4f, -4.4f, -6.2f, 0f, -6.2f)
        verticalLineToRelative(-6.4f)
        curveToRelative(-4f, 0.5f, -4f, -6.7f, 0f, -6.2f)
        verticalLineToRelative(-6.2f)
        curveToRelative(-4f, 0.5f, -4f, -6.7f, 0f, -6.2f)
        verticalLineTo(3.1f)
        arcTo(3f, 3f, 0f, false, true, 96.9f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineToRelative(-6.3f)
        curveToRelative(0f, 4.1f, -6.2f, 4.1f, -6.2f, 0f)
        horizontalLineTo(3.1f)
        arcTo(3f, 3f, 0f, false, true, 0f, 3.1f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        curveToRelative(4.1f, 0f, 4.1f, 6.2f, 0f, 6.2f)
        verticalLineToRelative(6.2f)
        arcToRelative(3f, 3f, 0f, false, true, 3.1f, 3.1f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        curveToRelative(0f, -4.1f, 6.2f, -4.1f, 6.2f, 0f)
        horizontalLineToRelative(6.3f)
        arcToRelative(3f, 3f, 0f, false, true, 3.1f, -2.9f)
    }.nodes

    val size = DpSize(
        width = 100.dp,
        height = 124.dp,
    )

    val fill: ImageVector by lazy {
        ImageVector.Builder(
            name = "StampA.Fill",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = path,
                fill = SolidColor(Color.Black),
            )
            .build()
    }

    val stroke: ImageVector by lazy {
        ImageVector.Builder(
            name = "StampA.Stroke",
            defaultWidth = size.width,
            defaultHeight = size.height,
            viewportWidth = size.width.value,
            viewportHeight = size.height.value,
        )
            .addPath(
                pathData = path,
                stroke = SolidColor(Color.Red),
                strokeLineWidth = 0.4f,
            )
            .build()
    }
}

@Preview(
    name = "Fill",
)
@Composable
private fun StampShapeAFillPreview() {
    Image(
        imageVector = StampShapeA.fill,
        contentDescription = null,
    )
}

@Preview(
    name = "Stroke",
)
@Composable
private fun StampShapeAStrokePreview() {
    Image(
        imageVector = StampShapeA.stroke,
        contentDescription = null,
    )
}
