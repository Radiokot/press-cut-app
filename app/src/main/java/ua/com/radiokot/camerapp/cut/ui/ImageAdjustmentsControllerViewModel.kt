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

package ua.com.radiokot.camerapp.cut.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.camerapp.cut.domain.BrightnessImageAdjustment
import ua.com.radiokot.camerapp.cut.domain.GlitchImageAdjustment
import ua.com.radiokot.camerapp.cut.domain.ContrastImageAdjustment
import ua.com.radiokot.camerapp.cut.domain.ImageAdjustment
import ua.com.radiokot.camerapp.cut.domain.TemperatureImageAdjustment
import ua.com.radiokot.camerapp.cut.domain.VibranceImageAdjustment
import ua.com.radiokot.camerapp.util.lazyLogger
import kotlin.time.Duration.Companion.milliseconds

@Immutable
class ImageAdjustmentsControllerViewModel : ViewModel() {

    private val log by lazyLogger("ImageAdjControllerVM")

    val items = persistentListOf(
        AdjustmentControllerItem(
            title = "Brightness",
            minValue = -100,
            maxValue = 100,
            key = BRIGHTNESS_KEY,
        ),
        AdjustmentControllerItem(
            title = "Contrast",
            minValue = -100,
            maxValue = 100,
            key = CONTRAST_KEY,
        ),
        AdjustmentControllerItem(
            title = "Vibrance",
            minValue = -100,
            maxValue = 100,
            key = VIBRANCE_KEY,
        ),
        AdjustmentControllerItem(
            title = "Temperature",
            minValue = -100,
            maxValue = 100,
            key = TEMPERATURE_KEY,
        ),
        AdjustmentControllerItem(
            title = "Glitch",
            minValue = 0,
            maxValue = 100,
            key = GLITCH_KEY,
        ),
    )

    val currentItem: StateFlow<AdjustmentControllerItem>
        field = MutableStateFlow(items.first())
    private val brightnessValue: MutableStateFlow<Int> = MutableStateFlow(0)
    private val contrastValue: MutableStateFlow<Int> = MutableStateFlow(0)
    private val vibranceValue: MutableStateFlow<Int> = MutableStateFlow(0)
    private val temperatureValue: MutableStateFlow<Int> = MutableStateFlow(0)
    private val chaosValue: MutableStateFlow<Int> = MutableStateFlow(0)

    private val _currentValue: MutableStateFlow<Int>
        get() = when (currentItem.value.key) {
            BRIGHTNESS_KEY -> brightnessValue
            CONTRAST_KEY -> contrastValue
            VIBRANCE_KEY -> vibranceValue
            TEMPERATURE_KEY -> temperatureValue
            GLITCH_KEY -> chaosValue
            else -> error("Unknown key")
        }
    val currentValue: StateFlow<Int> =
        currentItem
            .flatMapLatest { _currentValue }
            .stateIn(viewModelScope, SharingStarted.Eagerly, _currentValue.value)

    val adjustments: StateFlow<Array<ImageAdjustment>> =
        combine(
            contrastValue
                .sample(10.milliseconds)
                .map { ContrastImageAdjustment(it / 100f) },
            brightnessValue
                .sample(10.milliseconds)
                .map { BrightnessImageAdjustment(it / 100f) },
            vibranceValue
                .sample(10.milliseconds)
                .map { VibranceImageAdjustment(it / 100f) },
            temperatureValue
                .sample(10.milliseconds)
                .map { TemperatureImageAdjustment(it / 100f) },
            chaosValue
                .sample(10.milliseconds)
                .map { GlitchImageAdjustment(it / 100f) },
            transform = ::arrayOf,
        )
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyArray())

    fun onCurrentItemChanged(newItem: AdjustmentControllerItem) {
        log.debug {
            "onCurrentItemChanged(): setting new item:" +
                    "\nnewItem=$newItem"
        }

        currentItem.value = newItem
    }

    fun onValueChanged(newValue: Int) {
        _currentValue.value = newValue
    }

    private companion object {
        private const val BRIGHTNESS_KEY = "brightness"
        private const val CONTRAST_KEY = "contrast"
        private const val VIBRANCE_KEY = "vibrance"
        private const val TEMPERATURE_KEY = "temperature"
        private const val GLITCH_KEY = "glitch"
    }
}
