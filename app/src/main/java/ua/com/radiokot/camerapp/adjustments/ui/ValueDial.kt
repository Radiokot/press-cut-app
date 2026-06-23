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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import ua.com.radiokot.camerapp.ui.LocalColors

@Composable
fun ValueDial(
    modifier: Modifier = Modifier,
    minValue: Int,
    maxValue: Int,
    valueState: IntState,
    onValueChanged: (Int) -> Unit,
) = BoxWithConstraints(
    contentAlignment = Alignment.BottomCenter,
    modifier = modifier
) {
    val step = 5
    val spacingDp = 6.dp
    val spacingPx = with(LocalDensity.current) {
        spacingDp.toPx()
    }
    val rowState = rememberSaveable(
        minValue,
        saver = LazyListState.Saver,
    ) {
        val (initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset) =
            toIndexAndOffset(
                minValue = minValue,
                step = step,
                itemSpacingPx = spacingPx,
                value = valueState.intValue,
            )
        LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }
    val internalValueState = remember(rowState) {
        mutableIntStateOf(valueState.intValue)
    }
    val internalValueFlow = remember(rowState) {
        snapshotFlow {
            toValue(
                minValue = minValue,
                step = step,
                itemSpacingPx = spacingPx,
                firstVisibleItemIndex = rowState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = rowState.firstVisibleItemScrollOffset,
            )
        }.onEach(internalValueState::intValue::set)
    }
    val valueFlow = remember(valueState) {
        snapshotFlow { valueState.intValue }
    }
    val hapticFeedback = LocalHapticFeedback.current
    val colors = LocalColors.current

    LaunchedEffect(internalValueFlow) {
        internalValueFlow.collect { newInternalValue ->
            if (newInternalValue != valueState.intValue) {
                onValueChanged(newInternalValue)
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.GestureEnd
                )
            }
        }
    }

    LaunchedEffect(valueFlow) {
        valueFlow
            .filter { it != internalValueState.intValue }
            .collect { valueToSnapTo ->
                val (
                    itemIndex,
                    itemScrollOffset,
                ) = toIndexAndOffset(
                    minValue = minValue,
                    step = step,
                    itemSpacingPx = spacingPx,
                    value = valueToSnapTo,
                )
                rowState.scrollToItem(itemIndex, itemScrollOffset)
            }
    }

    LazyRow(
        state = rowState,
        overscrollEffect = null,
        contentPadding =
            PaddingValues(
                horizontal = (maxWidth - 2.dp) / 2,
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(spacingDp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val itemCount = (maxValue - minValue) / step + 1

        items(
            count = (maxValue - minValue) / step + 1,
        ) { i ->
            Spacer(
                modifier = Modifier
                    .size(
                        height =
                            if (i == itemCount / 2)
                                15.dp
                            else
                                12.dp,
                        width = 1.dp,
                    )
                    .background(
                        color =
                            if (i % step == 0)
                                colors.adjustmentsControllerDialMajor
                            else
                                colors.adjustmentsControllerDialMinor,
                    )
            )
        }
    }

    Spacer(
        modifier = Modifier
            .size(
                height = 16.dp,
                width = 2.dp,
            )
            .border(
                width = 2.dp,
                color = colors.componentStroke,
                shape = CircleShape,
            )
    )
}

private fun toValue(
    minValue: Int,
    step: Int,
    itemSpacingPx: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): Int {
    val stepIndex = firstVisibleItemIndex
    val stepOffsetPx = firstVisibleItemScrollOffset
    return minValue + ((stepIndex + stepOffsetPx / itemSpacingPx) * step).fastRoundToInt()
}

private fun toIndexAndOffset(
    minValue: Int,
    step: Int,
    itemSpacingPx: Float,
    value: Int,
): Pair<Int, Int> {
    val index = (value - minValue) / step
    val offsetPx = (itemSpacingPx * ((value - minValue) % step) / step).fastRoundToInt()
    return index to offsetPx
}
