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

import androidx.annotation.FloatRange
import androidx.compose.ui.util.fastCoerceIn

sealed interface ImageAdjustment {

    /**
     * From -1 to 1.
     */
    @setparam:FloatRange(from = -1.0, to = 1.0)
    @get:FloatRange(from = -1.0, to = 1.0)
    var value: Float

    fun apply(rgb: IntArray)
}

class BrightnessImageAdjustment : ImageAdjustment {

    override var value: Float = 0f

    override fun apply(rgb: IntArray) {
        if (value == 0f) {
            return
        }

        rgb[0] = (rgb[0] * (1 + value)).toInt().fastCoerceIn(0, 255)
        rgb[1] = (rgb[1] * (1 + value)).toInt().fastCoerceIn(0, 255)
        rgb[2] = (rgb[2] * (1 + value)).toInt().fastCoerceIn(0, 255)
    }
}

class ContrastImageAdjustment : ImageAdjustment {

    override var value: Float = 0f

    override fun apply(rgb: IntArray) {
        if (value == 0f) {
            return
        }

        val contrast = 1 + value
        rgb[0] = (((rgb[0] - 128) * contrast) + 128).toInt().fastCoerceIn(0, 255)
        rgb[1] = (((rgb[1] - 128) * contrast) + 128).toInt().fastCoerceIn(0, 255)
        rgb[2] = (((rgb[2] - 128) * contrast) + 128).toInt().fastCoerceIn(0, 255)
    }
}

class VibranceImageAdjustment : ImageAdjustment {

    override var value: Float = 0f

    override fun apply(rgb: IntArray) {
        if (value == 0f) {
            return
        }

        val (r, g, b) = rgb
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val chroma = (max - min) / 255f
        val luma = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()

        // Muted pixels get a strong boost, vivid pixels get little/none
        val multiplier = 1f + value * (1f - chroma)

        rgb[0] = (luma + (r - luma) * multiplier).toInt().fastCoerceIn(0, 255)
        rgb[1] = (luma + (g - luma) * multiplier).toInt().fastCoerceIn(0, 255)
        rgb[2] = (luma + (b - luma) * multiplier).toInt().fastCoerceIn(0, 255)
    }
}
