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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalColors = compositionLocalOf<AppColors> { LightAppColors }

@Composable
fun AppTheme(
    vararg extras: ProvidedValue<*>,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (isDark)
            DarkAppColors
        else
            LightAppColors

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.textInputCursor,
            backgroundColor = colors.textInputCursor.copy(
                alpha = 0.6f,
            )
        ),
        *extras,
        content = content,
    )
}

interface AppColors {
    val screenBackground: Color
    val paperBackgroundLine: Color
    val textPrimary: Color
    val textSecondary: Color
    val textClickable: Color
    val textDanger: Color
    val textInputHint: Color
    val textInputCursor: Color
    val stampBoxBack: Color
    val componentBackground: Color
    val componentStroke: Color
    val leButtonFront: Color
    val leButtonDepth: Color
    val standaloneIcon: Color
    val componentDivider: Color
    val adjustmentsControllerAdjustedBackground: Color
    val adjustmentsControllerDialMajor: Color
    val adjustmentsControllerDialMinor: Color
    val stampShadow: Color
    val progressBarProgress: Color
    val holdToDeleteBackground: Color
    val selectionEnvelopeLeft: Color
    val selectionEnvelopeRight: Color
}

object LightAppColors : AppColors {
    override val screenBackground = Color(0xfffef6eb)
    override val paperBackgroundLine = Color(0xFFEEEDE6)
    override val textPrimary = Color(0xFF3D3B36)
    override val textSecondary = Color(0xff7e7a74)
    override val textClickable = Color(0xFF85794B)
    override val textDanger = Color(0xFFD97D7D)
    override val textInputCursor = Color(0xff3f3f46)
    override val textInputHint = Color(0xFFB9AC8C)
    override val stampBoxBack = Color(0xFFCBC4BB)
    override val componentBackground = Color(0xFFFFF9EB)
    override val componentStroke = Color(0xFF6B624B)
    override val leButtonFront = Color(0xFFFFF9EB)
    override val leButtonDepth = Color(0xFFcbc4bb)
    override val standaloneIcon = Color(0xFFB9AC8C)
    override val componentDivider = Color(0xFFcbc4bb)
    override val adjustmentsControllerAdjustedBackground = Color(0xFFEFE7CD)
    override val adjustmentsControllerDialMajor = Color(0xFF9A8E72)
    override val adjustmentsControllerDialMinor = Color(0x99B9AC8C)
    override val stampShadow = Color(0x7447525E)
    override val progressBarProgress = Color(0xFFD7C3AA)
    override val holdToDeleteBackground = Color(0xFFEAB8B8)
    override val selectionEnvelopeLeft = Color(0xFFfff9eb)
    override val selectionEnvelopeRight = Color(0xFFF1EBDB)
}

object DarkAppColors : AppColors {
    override val screenBackground = Color(0xff111A10)
    override val paperBackgroundLine = Color(0xFF2D2A1C)
    override val textPrimary = Color(0xFFECC78F)
    override val textSecondary = Color(0xFFA28A65)
    override val textClickable = Color(0xFFBE9851)
    override val textDanger = Color(0xFFBD6B6B)
    override val textInputHint = Color(0xFF645640)
    override val textInputCursor = Color(0xFFECC78F)
    override val stampBoxBack = Color(0xFF252616)
    override val componentBackground = Color(0xff111A10)
    override val componentStroke = Color(0xFF937C59)
    override val leButtonFront = Color(0xff111A10)
    override val leButtonDepth = Color(0xFF252615)
    override val standaloneIcon = Color(0xFF937C59)
    override val componentDivider = Color(0xFF504D2F)
    override val adjustmentsControllerAdjustedBackground = Color(0xFF252615)
    override val adjustmentsControllerDialMajor = Color(0xFFA28A65)
    override val adjustmentsControllerDialMinor = Color(0xFF645640)
    override val stampShadow = Color(0xFF2D2A1C)
    override val progressBarProgress = Color(0xFF524628)
    override val holdToDeleteBackground = Color(0xFF563333)
    override val selectionEnvelopeLeft = Color(0xFF202112)
    override val selectionEnvelopeRight = Color(0xFF252615)
}
