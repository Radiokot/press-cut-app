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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.R
import kotlin.time.Duration.Companion.milliseconds

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("InflateParams")
@Suppress("DEPRECATION")
fun showToast(
    context: Context,
    text: CharSequence,
    colors: AppColors,
    @IntRange(from = 300, to = 3500)
    durationMs: Int = 3500,
    gravity: Int = Gravity.BOTTOM,
    offset: IntOffset = IntOffset(
        x = 0,
        y = 200,
    ),
) {
    val toastView =
        LayoutInflater
            .from(context)
            .inflate(R.layout.view_toast, null)

    toastView.backgroundTintList = ColorStateList.valueOf(colors.componentBackground.toArgb())

    with(toastView.findViewById<TextView>(R.id.textView)) {
        setText(text)
        setTextColor(colors.textPrimary.toArgb())
        setTypeface(ResourcesCompat.getFont(context, R.font.podkova_regular))
        backgroundTintList = ColorStateList.valueOf(colors.componentStroke.toArgb())
    }

    with(Toast(context)) {
        duration = Toast.LENGTH_LONG
        setGravity(gravity, offset.x, offset.y)
        view = toastView
        show()

        GlobalScope.launch {
            delay(durationMs.milliseconds)
            cancel()
        }
    }
}
