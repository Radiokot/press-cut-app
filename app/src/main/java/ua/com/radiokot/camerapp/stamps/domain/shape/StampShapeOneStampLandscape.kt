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

package ua.com.radiokot.camerapp.stamps.domain.shape

import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.IntSize

object StampShapeOneStampLandscape: StampShape {

    override val name = "onestamp_landscape"

    override val path: List<PathNode> by lazy {
        PathBuilder().apply {
            moveTo(8f, 0f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(5f)
            curveToRelative(0f, 5.2f, 8f, 5.2f, 8f, 0f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(7f)
            curveToRelative(-5.2f, 0f, -5.2f, 8f, 0f, 8f)
            verticalLineToRelative(7.4f)
            curveToRelative(-5.2f, 0f, -5.2f, 8f, 0f, 8f)
            verticalLineToRelative(7.4f)
            curveToRelative(-5.2f, 0f, -5.2f, 8f, 0f, 8f)
            verticalLineToRelative(7.4f)
            curveToRelative(-5.2f, 0f, -5.2f, 8f, 0f, 8f)
            verticalLineToRelative(7.4f)
            curveToRelative(-5.2f, 0f, -5.2f, 8f, 0f, 8f)
            verticalLineTo(84f)
            curveToRelative(-5.2f, 0f, -5.2f, 8f, 0f, 8f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(-8f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineToRelative(-5f)
            curveToRelative(0f, -5.2f, -8f, -5.2f, -8f, 0f)
            horizontalLineTo(0f)
            verticalLineToRelative(-8f)
            curveToRelative(5.2f, 0f, 5.2f, -8f, 0f, -8f)
            verticalLineToRelative(-7.4f)
            arcToRelative(4f, 4f, 0f, false, false, 4f, -4f)
            arcToRelative(4f, 4f, 0f, false, false, -4f, -4f)
            verticalLineToRelative(-7.4f)
            curveToRelative(5.2f, 0f, 5.2f, -8f, 0f, -8f)
            verticalLineToRelative(-7.4f)
            arcToRelative(4f, 4f, 0f, false, false, 4f, -4f)
            arcToRelative(4f, 4f, 0f, false, false, -4f, -4f)
            verticalLineToRelative(-7.4f)
            curveToRelative(5.2f, 0f, 5.2f, -8f, 0f, -8f)
            verticalLineTo(15f)
            curveToRelative(5.2f, 0f, 5.2f, -8f, 0f, -8f)
            verticalLineTo(0f)
            close()
        }.nodes
    }

    override val size =
        IntSize(
            width = 128,
            height = 100,
        )
}
