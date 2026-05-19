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

object StampShapeOneStamp : StampShape {

    override val name = "onestamp"

    override val path: List<PathNode> by lazy {
        PathBuilder().apply {
            moveTo(100f, 8f)
            curveTo(94.8412f, 8f, 94.8412f, 16f, 100f, 16f)
            verticalLineTo(21f)
            curveTo(94.8412f, 21f, 94.8412f, 29f, 100f, 29f)
            verticalLineTo(34f)
            curveTo(94.8412f, 34f, 94.8412f, 42f, 100f, 42f)
            verticalLineTo(47f)
            curveTo(94.8412f, 47f, 94.8412f, 55f, 100f, 55f)
            verticalLineTo(60f)
            curveTo(94.8412f, 60f, 94.8412f, 68f, 100f, 68f)
            verticalLineTo(73f)
            curveTo(94.8412f, 73f, 94.8412f, 81f, 100f, 81f)
            verticalLineTo(86f)
            curveTo(94.8412f, 86f, 94.8412f, 94f, 100f, 94f)
            verticalLineTo(99f)
            curveTo(94.8412f, 99f, 94.8412f, 107f, 100f, 107f)
            verticalLineTo(112f)
            curveTo(94.8412f, 112f, 94.8412f, 120f, 100f, 120f)
            verticalLineTo(128f)
            horizontalLineTo(93f)
            curveTo(93f, 122.841f, 85f, 122.841f, 85f, 128f)
            horizontalLineTo(77.5996f)
            curveTo(77.5996f, 122.841f, 69.5996f, 122.841f, 69.5996f, 128f)
            horizontalLineTo(62.2002f)
            curveTo(62.2002f, 122.841f, 54.2002f, 122.841f, 54.2002f, 128f)
            horizontalLineTo(46.7998f)
            curveTo(46.7998f, 122.841f, 38.7998f, 122.841f, 38.7998f, 128f)
            horizontalLineTo(31.4004f)
            curveTo(31.4004f, 122.841f, 23.4004f, 122.841f, 23.4004f, 128f)
            horizontalLineTo(16f)
            curveTo(16f, 122.841f, 8f, 122.841f, 8f, 128f)
            horizontalLineTo(0f)
            verticalLineTo(120f)
            curveTo(5.15877f, 120f, 5.15877f, 112f, 0f, 112f)
            verticalLineTo(107f)
            curveTo(5.15877f, 107f, 5.15877f, 99f, 0f, 99f)
            verticalLineTo(94f)
            curveTo(5.15877f, 94f, 5.15877f, 86f, 0f, 86f)
            verticalLineTo(81f)
            curveTo(5.15877f, 81f, 5.15877f, 73f, 0f, 73f)
            verticalLineTo(68f)
            curveTo(5.15877f, 68f, 5.15877f, 60f, 0f, 60f)
            verticalLineTo(55f)
            curveTo(5.15877f, 55f, 5.15877f, 47f, 0f, 47f)
            verticalLineTo(42f)
            curveTo(5.15877f, 42f, 5.15877f, 34f, 0f, 34f)
            verticalLineTo(29f)
            curveTo(5.15877f, 29f, 5.15877f, 21f, 0f, 21f)
            verticalLineTo(16f)
            curveTo(5.15877f, 16f, 5.15877f, 8f, 0f, 8f)
            verticalLineTo(0f)
            horizontalLineTo(8f)
            curveTo(8f, 5.15877f, 16f, 5.15877f, 16f, 0f)
            horizontalLineTo(23.4004f)
            curveTo(23.4004f, 2.20914f, 25.1913f, 4f, 27.4004f, 4f)
            curveTo(29.6263f, 3.99979f, 31.3385f, 2.1758f, 31.4004f, 0f)
            horizontalLineTo(38.7998f)
            curveTo(38.7998f, 5.15869f, 46.7998f, 5.15879f, 46.7998f, 0f)
            horizontalLineTo(54.2002f)
            curveTo(54.2002f, 2.20914f, 55.9911f, 4f, 58.2002f, 4f)
            curveTo(60.4262f, 3.99989f, 62.1383f, 2.17582f, 62.2002f, 0f)
            horizontalLineTo(69.5996f)
            curveTo(69.5996f, 5.1586f, 77.5996f, 5.15881f, 77.5996f, 0f)
            horizontalLineTo(85f)
            curveTo(85f, 5.15877f, 93f, 5.15877f, 93f, 0f)
            horizontalLineTo(100f)
            verticalLineTo(8f)
            close()
        }.nodes
    }

    override val size =
        IntSize(
            width = 100,
            height = 128,
        )
}
