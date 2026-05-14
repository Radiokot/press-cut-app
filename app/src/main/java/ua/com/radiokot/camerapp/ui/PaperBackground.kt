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

package ua.com.radiokot.camerapp.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.paperBackground(
    verticalOffset: (() -> Int)? = null,
    drawBackgroundColor: Boolean = false,
) = drawWithCache {

    val backgroundColor = Color(0xfffef6eb)
    val lineColor = Color(0xFFEEEDE6)
    val gridSize = 20.dp.roundToPx()
    val gridThickness = 1.dp.toPx()

    onDrawWithContent {
        if (drawBackgroundColor) {
            drawRect(backgroundColor)
        }

        var startY = (0 - gridSize / 2)
        if (verticalOffset != null) {
            startY += verticalOffset() % gridSize
        }

        for (y in (startY..size.height.toInt() step gridSize)) {
            drawLine(
                color = lineColor,
                start = Offset(
                    x = 0f,
                    y = y.toFloat(),
                ),
                end = Offset(
                    x = size.width,
                    y = y.toFloat(),
                ),
                strokeWidth = gridThickness,
            )
        }
        for (x in (0..size.width.toInt() step gridSize)) {
            drawLine(
                color = lineColor,
                start = Offset(
                    x = x.toFloat(),
                    y = 0f,
                ),
                end = Offset(
                    x = x.toFloat(),
                    y = size.height,
                ),
                strokeWidth = gridThickness,
            )
        }

        drawContent()
    }
}
