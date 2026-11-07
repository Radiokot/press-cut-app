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

package ua.com.radiokot.camerapp.cut.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

sealed interface StampCutImageSource {

    /**
     * Captures a photo from this source.
     *
     * @return a flow of captured frames:
     * could be a low-quality one and then a high-quality one,
     * or maybe just a high-quality one.
     */
    fun capture(): Flow<Bitmap>
}
