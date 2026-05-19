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

import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.unit.IntSize

sealed interface StampShape {

    val name: String
    val path: List<PathNode>
    val size: IntSize

    companion object {

        fun fromName(
            name: String,
        ): StampShape = when (name) {

            StampShapeA.name -> StampShapeA
            StampShapeOneStamp.name -> StampShapeOneStamp
            StampShapeOneStampLandscape.name -> StampShapeOneStampLandscape
            StampShapeOneStampSquare.name -> StampShapeOneStampSquare

            else -> throw IllegalArgumentException("Unknown shape name $name")
        }
    }
}
