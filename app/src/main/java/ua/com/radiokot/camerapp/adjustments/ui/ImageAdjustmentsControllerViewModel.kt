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

@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package ua.com.radiokot.camerapp.adjustments.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.camerapp.adjustments.domain.BrightnessImageAdjustment
import ua.com.radiokot.camerapp.adjustments.domain.ContrastImageAdjustment
import ua.com.radiokot.camerapp.adjustments.domain.GlitchImageAdjustment
import ua.com.radiokot.camerapp.adjustments.domain.ImageAdjustment
import ua.com.radiokot.camerapp.adjustments.domain.TemperatureImageAdjustment
import ua.com.radiokot.camerapp.adjustments.domain.VibranceImageAdjustment
import kotlin.time.Duration.Companion.milliseconds

@Immutable
class ImageAdjustmentsControllerViewModel : ViewModel() {

    private val brightnessValueState = mutableIntStateOf(0)
    private val contrastValueState = mutableIntStateOf(0)
    private val vibranceValueState = mutableIntStateOf(0)
    private val temperatureValueState = mutableIntStateOf(0)
    private val glitchValueState = mutableIntStateOf(0)

    val items = persistentListOf(
        AdjustmentsControllerItem.Dial(
            title = "Brightness",
            defaultValue = brightnessValueState.intValue,
            minValue = -100,
            maxValue = 100,
            valueState = brightnessValueState,
            onValueChanged = brightnessValueState::value::set,
            key = "b",
        ),
        AdjustmentsControllerItem.Dial(
            title = "Contrast",
            defaultValue = contrastValueState.intValue,
            minValue = -100,
            maxValue = 100,
            valueState = contrastValueState,
            onValueChanged = contrastValueState::value::set,
            key = "c",
        ),
        AdjustmentsControllerItem.Dial(
            title = "Vibrance",
            defaultValue = vibranceValueState.intValue,
            minValue = -100,
            maxValue = 100,
            valueState = vibranceValueState,
            onValueChanged = vibranceValueState::value::set,
            key = "v",
        ),
        AdjustmentsControllerItem.Dial(
            title = "Temperature",
            defaultValue = temperatureValueState.intValue,
            minValue = -100,
            maxValue = 100,
            valueState = temperatureValueState,
            onValueChanged = temperatureValueState::value::set,
            key = "t",
        ),
        AdjustmentsControllerItem.Dial(
            title = "Glitch",
            defaultValue = glitchValueState.intValue,
            minValue = 0,
            maxValue = 100,
            valueState = glitchValueState,
            onValueChanged = glitchValueState::value::set,
            key = "g",
        )
    )

    val adjustments: StateFlow<Array<ImageAdjustment>> =
        snapshotFlow {
            arrayOf(
                BrightnessImageAdjustment(brightnessValueState.intValue / 100f),
                ContrastImageAdjustment(contrastValueState.intValue / 100f),
                VibranceImageAdjustment(vibranceValueState.intValue / 100f),
                TemperatureImageAdjustment(temperatureValueState.intValue / 100f),
                GlitchImageAdjustment(glitchValueState.intValue / 100f),
            )
        }
            .sample(10.milliseconds)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyArray())
}
