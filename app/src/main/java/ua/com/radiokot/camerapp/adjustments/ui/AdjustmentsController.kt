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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.ui.rememberSnapFlingBehavior

@Composable
fun AdjustmentsController(
    modifier: Modifier = Modifier,
    items: ImmutableList<AdjustmentsControllerItem>,
) {
    val currentItemIndexState = retain {
        mutableIntStateOf(0)
    }

    Column(
        modifier = modifier,
    ) {
        BasicText(
            text = items[currentItemIndexState.intValue].title,
            style = TextStyle(
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontFamily = PodkovaFamily,
                color = LocalColors.current.textPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        ItemSelector2(
            items = items,
            currentItemIndexState = currentItemIndexState,
            onCurrentItemIndexChanged = currentItemIndexState::value::set,
            modifier = Modifier
                .padding(
                    top = 8.dp
                )
                .fillMaxWidth()
        )

        key("${items[currentItemIndexState.intValue]}-controller") {
            items[currentItemIndexState.intValue].ValueController()
        }
    }
}

@Composable
private fun ItemSelector2(
    modifier: Modifier = Modifier,
    items: List<AdjustmentsControllerItem>,
    currentItemIndexState: IntState,
    onCurrentItemIndexChanged: (Int) -> Unit,
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

    val initialFirstVisibleItemIndex = remember {
        currentItemIndexState.intValue
    }
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex)
    val itemIndexFlow = remember(rowState) {
        val offsetThreshold = (itemSizePx + spacingPx) / 2f
        snapshotFlow {
            var index = rowState.firstVisibleItemIndex
            if (rowState.firstVisibleItemScrollOffset > offsetThreshold) {
                index++
            }
            index
        }
    }
    LaunchedEffect(itemIndexFlow) {
        itemIndexFlow.collect { newItemIndex ->
            if (newItemIndex != currentItemIndexState.intValue) {
                onCurrentItemIndexChanged(newItemIndex)
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.GestureEnd
                )
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalColors.current

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
                        color = colors.standaloneIcon,
                        shape = CircleShape,
                    )
                    .clickable(
                        onClick = {
                            if (rowState.firstVisibleItemIndex == index) {
                                item.resetValue()
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
                        color = colors.standaloneIcon,
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
                color = colors.componentStroke,
                shape = CircleShape,
            )
    ) {
        Crossfade(
            targetState = currentItemIndexState.intValue,
            animationSpec = tween(150),
            modifier = Modifier
                .fillMaxSize()
        ) { itemIndex ->
            key("${items[itemIndex].key}-indicator") {
                items[itemIndex].ValueIndicator()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AdjustmentsControllerPreview() {
    AppTheme {
        val items = remember {
            val brightnessValueState = mutableIntStateOf(0)
            val contrastValueState = mutableIntStateOf(22)
            val vibranceValueState = mutableIntStateOf(33)

            persistentListOf(
                AdjustmentsControllerItem.Dial(
                    title = "Brightness",
                    defaultValue = 0,
                    minValue = -100,
                    maxValue = 100,
                    valueState = brightnessValueState,
                    onValueChanged = brightnessValueState::intValue::set,
                    key = "b",
                ),
                AdjustmentsControllerItem.Dial(
                    title = "Contrast",
                    defaultValue = 0,
                    minValue = -100,
                    maxValue = 100,
                    valueState = contrastValueState,
                    onValueChanged = contrastValueState::intValue::set,
                    key = "c",
                ),
                AdjustmentsControllerItem.Dial(
                    title = "Vibrance",
                    defaultValue = 0,
                    minValue = -100,
                    maxValue = 100,
                    valueState = vibranceValueState,
                    onValueChanged = vibranceValueState::intValue::set,
                    key = "v",
                )
            )
        }

        AdjustmentsController(
            items = items,
            modifier = Modifier
                .fillMaxWidth()
                .paperBackground(
                    drawBackgroundColor = true,
                )
                .padding(24.dp)
        )
    }
}
