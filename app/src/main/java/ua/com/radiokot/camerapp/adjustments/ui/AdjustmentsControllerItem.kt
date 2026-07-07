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

package ua.com.radiokot.camerapp.adjustments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.com.radiokot.camerapp.ui.DarkAppColors
import ua.com.radiokot.camerapp.ui.LightAppColors
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground
import java.text.DecimalFormat
import java.text.NumberFormat

@Stable
interface AdjustmentsControllerItem {
    val title: String
    val key: Any

    fun resetValue()

    @Composable
    fun ValueIndicator()

    @Composable
    fun ValueController()

    @Stable
    class Dial(
        override val title: String,
        val defaultValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val valueState: IntState,
        val onValueChanged: (Int) -> Unit,
        override val key: Any,
    ) : AdjustmentsControllerItem {

        @Composable
        override fun ValueIndicator() {
            val isValueVisible by remember {
                derivedStateOf {
                    valueState.intValue != defaultValue
                }
            }
            val colors = LocalColors.current

            if (isValueVisible) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = colors.adjustmentsControllerAdjustedBackground,
                            shape = CircleShape,
                        )
                ) {
                    BasicText(
                        text = valueNumberFormat.format(valueState.intValue),
                        style = TextStyle(
                            fontFamily = PodkovaFamily,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    )
                }
            }
        }

        @Composable
        override fun ValueController() {
            ValueDial(
                minValue = minValue,
                maxValue = maxValue,
                valueState = valueState,
                onValueChanged = onValueChanged,
                modifier = Modifier
                    .padding(
                        top = 8.dp
                    )
                    .fillMaxWidth()
            )
        }

        override fun resetValue() {
            onValueChanged(defaultValue)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Dial) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }

        private companion object {
            private val valueNumberFormat =
                (NumberFormat.getNumberInstance() as DecimalFormat).apply {
                    positivePrefix = "+"
                    negativePrefix = "-"
                }
        }
    }

    @Stable
    class Theme(
        val isDarkByDefault: Boolean,
        val isDarkState: State<Boolean>,
        val onIsDarkChanged: (Boolean) -> Unit,
    ) : AdjustmentsControllerItem {
        override val title: String =
            "Theme"

        override val key: Any =
            "theme"

        override fun resetValue() {
            onIsDarkChanged(isDarkByDefault)
        }

        @Composable
        override fun ValueIndicator() {
        }

        @Composable
        override fun ValueController() {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.CenterHorizontally,
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                    )
            ) {
                val cornerRadius = 8.dp

                (0..1).forEach { i ->
                    val isDark = i == 1

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                shape = RoundedCornerShape(cornerRadius),
                            )
                            .border(
                                width = 2.dp,
                                color = LocalColors.current.componentStroke,
                                shape = RoundedCornerShape(cornerRadius),
                            )
                            .clickable(
                                onClick = {
                                    onIsDarkChanged(isDark)
                                }
                            )
                    ) {
                        CompositionLocalProvider(
                            LocalColors provides if (isDark) DarkAppColors else LightAppColors,
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .paperBackground(
                                        drawBackgroundColor = true,
                                        gridSize = 10.dp,
                                    )
                            )

                            BasicText(
                                text = if (isDark) "D" else "L",
                                style = TextStyle(
                                    fontFamily = PodkovaFamily,
                                    color = LocalColors.current.textPrimary,
                                    fontSize = 22.sp,
                                    textDecoration =
                                        if (isDark == isDarkState.value)
                                            TextDecoration.Underline
                                        else
                                            null,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
