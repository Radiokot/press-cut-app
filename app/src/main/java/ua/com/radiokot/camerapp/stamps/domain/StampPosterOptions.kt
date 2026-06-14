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

package ua.com.radiokot.camerapp.stamps.domain

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

class StampPosterOptions(
    @FloatRange(from = 0.2, to = 3.0)
    val scale: Float,
    val withCaption: Boolean,
    val colors: Colors,
) {
    init {
        require(scale >= 0.2) {
            "A poster that small's fookin' useless, innit?"
        }
        require(scale <= 3) {
            "You off yer nut, mate?"
        }
    }

    class Colors(
        @ColorInt
        val paperBackground: Int,
        @ColorInt
        val paperBackgroundLine: Int,
        @ColorInt
        val stampShadow: Int,
        @ColorInt
        val caption: Int,
    )
}
