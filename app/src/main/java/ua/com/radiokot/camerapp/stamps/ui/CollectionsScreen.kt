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

package ua.com.radiokot.camerapp.stamps.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.StableHolder
import ua.com.radiokot.camerapp.util.plus

@Composable
fun CollectionsScreen(
    modifier: Modifier = Modifier,
    itemsState: State<ImmutableList<CollectionListItem>>,
    onItemClicked: (CollectionListItem) -> Unit,
    onItemLongClicked: (CollectionListItem) -> Unit,
    onNewStampAction: () -> Unit,
    onNewCollectionAction: () -> Unit,
    onMoreClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = Box(
    modifier = modifier
) {
    val safeContentPadding =
        WindowInsets.safeContent.asPaddingValues()
    val contentPadding =
        safeContentPadding +
                PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    // The More dots.
                    top = 32.dp,
                    // Button height and spacing.
                    bottom = 120.dp,
                )

    LazyVerticalGrid(
        columns = GridCells.FixedSize(CollectionViewSize.width * 1.05f),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterVertically,
        ),
        contentPadding = contentPadding,
        state = rememberLazyGridState(),
        overscrollEffect = null,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(
            items = itemsState.value,
            key = CollectionListItem::key,
        ) { item ->
            StampBoxView(
                name = item.name,
                someStamps = item.someStamps,
                key = item.key,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            onItemClicked(item)
                        },
                        onLongClick = {
                            onItemLongClicked(item)
                        },
                    )
            )
        }

        item(
            key = "new-collection-button"
        ) {
            NewCollectionView(
                onClicked = onNewCollectionAction,
            )
        }
    }

    Image(
        painter = painterResource(R.drawable.ic_more_hor_stroked),
        contentDescription = "More",
        modifier = Modifier
            .padding(safeContentPadding)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onMoreClicked,
            )
            .padding(16.dp)
            .align(Alignment.TopEnd)
    )

    LeTextButton(
        text = "New Stamp",
        onClick = onNewStampAction,
        modifier = Modifier
            .padding(safeContentPadding)
            .padding(24.dp)
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .run {
                if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                    return@run this
                }

                with(sharedTransitionScope) {
                    sharedElement(
                        sharedContentState = rememberSharedContentState("new-stamp-button"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        zIndexInOverlay = 30f,
                    )
                }
            }
    )
}

@Composable
private fun NewCollectionView(
    modifier: Modifier = Modifier,
    onClicked: () -> Unit,
) = Box(
    modifier = modifier
        .requiredSize(CollectionViewSize)
        .clickable(
            onClick = {
                onClicked()
            },
        ),
    contentAlignment = Alignment.Center,
) {
    val layoutDirection = LocalLayoutDirection.current
    val colors = LocalColors.current

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(CollectionViewSize.height * 0.7f)
            .drawWithCache {
                val backOutline = CollectionViewShape.createOutline(
                    size = this.size,
                    layoutDirection = layoutDirection,
                    density = this,
                )
                val strokeInterval = StampContainerBaseSize.width.toPx() * 0.08f
                val dashStrokeStyle = Stroke(
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(strokeInterval, strokeInterval)
                    )
                )
                val frontOutline = CollectionViewShape.createOutline(
                    size = Size(
                        width = this.size.width + 1f,
                        height = StampContainerBaseSize.height.toPx() * 0.5f,
                    ),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                val plainStrokeStyle = Stroke(
                    width = dashStrokeStyle.width,
                )

                onDrawBehind {
                    drawOutline(
                        outline = backOutline,
                        color = colors.componentStroke,
                        style = dashStrokeStyle,
                    )
                    translate(
                        left = -1f,
                        top = backOutline.bounds.height - frontOutline.bounds.height + 1f,
                    ) {
                        drawOutline(
                            outline = frontOutline,
                            color = colors.componentBackground,
                            style = Fill,
                        )
                        drawOutline(
                            outline = frontOutline,
                            color = colors.componentBackground,
                            style = plainStrokeStyle,
                        )
                        drawOutline(
                            outline = frontOutline,
                            color = colors.componentStroke,
                            style = dashStrokeStyle,
                        )
                    }
                }
            }
            .align(Alignment.BottomCenter)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(StampContainerBaseSize.height * 0.5f)
            .align(Alignment.BottomCenter)
            .padding(8.dp)
    ) {
        BasicText(
            text = "New Collection",
            style = TextStyle(
                fontFamily = PodkovaFamily,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = colors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun CollectionsScreenPreview() {
    val items = listOf(
        CollectionListItem(
            key = "1",
            name = "My stamps",
            someStamps = persistentListOf(
                StampSampleItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    key = "",
                ),
            ),
        ),
        CollectionListItem(
            key = "2",
            name = "RED",
            someStamps = persistentListOf(
                StampSampleItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    key = "1",
                ),
                StampSampleItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    key = "2",
                ),
            ),
        ),
        CollectionListItem(
            key = "3",
            name = "Food",
            someStamps = persistentListOf(
                StampSampleItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    key = "1",
                ),
                StampSampleItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    key = "2",
                ),
                StampSampleItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    key = "3",
                ),
            ),
        ),
    ).toImmutableList()

    CollectionsScreen(
        itemsState = items.let(::mutableStateOf),
        onItemClicked = {},
        onNewStampAction = {},
        onItemLongClicked = {},
        onNewCollectionAction = {},
        onMoreClicked = {},
        sharedTransitionScope = null,
        animatedVisibilityScope = null,
        modifier = Modifier
            .fillMaxSize()
            .paperBackground()
    )
}
