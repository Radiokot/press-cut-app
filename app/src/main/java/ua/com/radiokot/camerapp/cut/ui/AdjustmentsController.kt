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

package ua.com.radiokot.camerapp.cut.ui

import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.rememberSnapFlingBehavior

@Composable
fun AdjustmentsController(
    modifier: Modifier = Modifier,
    items: ImmutableList<AdjustmentControllerItem>,
    currentItemState: State<AdjustmentControllerItem>,
    onCurrentItemChanged: (AdjustmentControllerItem) -> Unit,
    valueState: IntState,
    onValueChanged: (Int) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        BasicText(
            text = currentItemState.value.title,
            style = TextStyle(
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontFamily = PodkovaFamily,
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        ItemSelector(
            items = items,
            currentItemState = currentItemState,
            onCurrentItemChanged = onCurrentItemChanged,
            valueState = valueState,
            onValueResetClicked = remember {
                fun() {
                    onValueChanged(0)
                }
            },
            modifier = Modifier
                .padding(
                    top = 8.dp
                )
                .fillMaxWidth()
        )

        ValueDial(
            minValue = currentItemState.value.minValue,
            maxValue = currentItemState.value.maxValue,
            valueState = valueState,
            onValueChanged = onValueChanged,
            modifier = Modifier
                .padding(
                    top = 8.dp
                )
                .fillMaxWidth()
        )
    }
}

@Composable
private fun ItemSelector(
    modifier: Modifier = Modifier,
    items: List<AdjustmentControllerItem>,
    currentItemState: State<AdjustmentControllerItem>,
    onCurrentItemChanged: (AdjustmentControllerItem) -> Unit,
    valueState: IntState,
    onValueResetClicked: () -> Unit,
) = BoxWithConstraints(
    modifier = modifier
) {
    val itemSize = 42.dp
    val itemSizePx = with(LocalDensity.current) {
        itemSize.toPx()
    }
    val spacingDp = 16.dp
    val spacingPx = with(LocalDensity.current) {
        spacingDp.toPx()
    }
    val hapticFeedback = LocalHapticFeedback.current

    val initialFirstVisibleItemIndex = remember(currentItemState) {
        items.indexOf(currentItemState.value)
    }
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex)
    val itemFlow = remember(rowState) {
        val offsetThreshold = (itemSizePx + spacingPx) / 2f
        snapshotFlow {
            var index = rowState.firstVisibleItemIndex
            if (rowState.firstVisibleItemScrollOffset > offsetThreshold) {
                index++
            }
            items[index]
        }
    }
    LaunchedEffect(itemFlow) {
        itemFlow.collect { newItem ->
            if (newItem != currentItemState.value) {
                onCurrentItemChanged(newItem)
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.GestureEnd
                )
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()

    LazyRow(
        state = rowState,
        flingBehavior = rememberSnapFlingBehavior(
            lazyListState = rowState,
            frictionMultiplier = 6f,
        ),
        overscrollEffect = null,
        contentPadding =
            PaddingValues(
                start = (maxWidth - itemSize) / 2,
                end = (maxWidth - itemSize) / 2,
            ),
        horizontalArrangement = Arrangement.spacedBy(spacingDp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.key },
        ) { index, item ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(itemSize)
                    .align(Alignment.Center)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFB9AC8C),
                        shape = CircleShape,
                    )
                    .clickable(
                        onClick = {
                            if (rowState.firstVisibleItemIndex == index
                                && valueState.intValue != 0
                            ) {
                                onValueResetClicked()
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.Confirm
                                )
                            } else {
                                coroutineScope.launch {
                                    rowState.animateScrollToItem(index)
                                }
                            }
                        }
                    )
            ) {
                BasicText(
                    text = item.title.first().toString(),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = PodkovaFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB9AC8C),
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .size(itemSize)
            .align(Alignment.Center)
            .border(
                width = 2.dp,
                color = Color(0xFF6B624B),
                shape = CircleShape,
            )
    ) {
        val valueNumberFormat = remember {
            (NumberFormat.getNumberInstance() as DecimalFormat).apply {
                positivePrefix = "+"
                negativePrefix = "-"
            }
        }
        val isValueVisible by remember {
            derivedStateOf {
                valueState.intValue != 0
            }
        }

        AnimatedVisibility(
            enter = fadeIn(
                animationSpec = tween(150),
            ),
            exit = fadeOut(
                animationSpec = tween(150),
            ),
            visible = isValueVisible,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFEFE7CD),
                        shape = CircleShape,
                    )
            ) {
                BasicText(
                    text =
                        valueNumberFormat
                            .format(valueState.intValue),
                    style = TextStyle(
                        fontFamily = PodkovaFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun ValueDial(
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

    LaunchedEffect(valueFlow, minValue) {
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
                                Color(0xFF9A8E72)
                            else
                                Color(0x99B9AC8C),
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
                color = Color(0xFF6B624B),
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

@Preview
@Composable
private fun AdjustmentsControllerPreview(

) = Box(
    modifier = Modifier
        .fillMaxSize()
        .paperBackground()
) {
    val items = persistentListOf(
        AdjustmentControllerItem(
            title = "Brightness",
            minValue = -100,
            maxValue = 100,
            key = "B",
        ),
        AdjustmentControllerItem(
            title = "Contrast",
            minValue = -100,
            maxValue = 100,
            key = "C",
        ),
        AdjustmentControllerItem(
            title = "Vibrance",
            minValue = -100,
            maxValue = 100,
            key = "V",
        )
    )
    val currentItem = remember {
        mutableStateOf(items[1])
    }
    val currentValue = remember {
        mutableIntStateOf(10)
    }

    AdjustmentsController(
        items = items,
        currentItemState = currentItem,
        onCurrentItemChanged = currentItem::value::set,
        valueState = currentValue,
        onValueChanged = currentValue::intValue::set,
        modifier = Modifier
            .fillMaxWidth()
    )
}
