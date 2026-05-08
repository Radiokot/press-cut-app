@file:OptIn(ExperimentalCoroutinesApi::class)

package ua.com.radiokot.camerapp.cut.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import ua.com.radiokot.camerapp.util.lazyLogger

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
    )

    val currentItem: StateFlow<AdjustmentControllerItem>
        field = MutableStateFlow(items.first())
    val brightnessValue: StateFlow<Int>
        field = MutableStateFlow(0)
    val contrastValue: StateFlow<Int>
        field = MutableStateFlow(0)
    val vibranceValue: StateFlow<Int>
        field = MutableStateFlow(0)

    private val _currentValue: MutableStateFlow<Int>
        get() = when (currentItem.value.key) {
            BRIGHTNESS_KEY -> brightnessValue
            CONTRAST_KEY -> contrastValue
            VIBRANCE_KEY -> vibranceValue
            else -> error("Unknown key")
        }
    val currentValue: StateFlow<Int> =
        currentItem
            .flatMapLatest { _currentValue }
            .stateIn(viewModelScope, SharingStarted.Eagerly, _currentValue.value)

    fun onCurrentItemChanged(newItem: AdjustmentControllerItem) {
        log.debug {
            "onCurrentItemChanged(): setting new item:" +
                    "\nnewItem=$newItem"
        }

        currentItem.value = newItem
    }

    fun onValueChanged(newValue: Int) {
        log.debug {
            "onValueChanged(): setting new value:" +
                    "\nnewVale=$newValue" +
                    "\ncurrentItem=${currentItem.value}"
        }

        _currentValue.value = newValue
    }

    private companion object {
        private const val BRIGHTNESS_KEY = "brightness"
        private const val CONTRAST_KEY = "contrast"
        private const val VIBRANCE_KEY = "vibrance"
    }
}
