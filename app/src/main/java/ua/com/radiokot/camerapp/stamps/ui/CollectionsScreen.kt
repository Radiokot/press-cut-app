package ua.com.radiokot.camerapp.stamps.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.paperBackground
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
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(
            items = itemsState.value,
            key = CollectionListItem::key,
        ) { item ->
            CollectionView(
                item = item,
                onClicked = onItemClicked,
                onLongClicked = onItemLongClicked,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
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
                val frontBackgroundColor = Color(0xFFFFF9EB)
                val strokeColor = Color(0xFF6B624B)
                val strokeInterval = StampSize.width.toPx() * 0.08f
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
                        height = StampSize.height.toPx() * 0.5f,
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
                        color = strokeColor,
                        style = dashStrokeStyle,
                    )
                    translate(
                        left = -1f,
                        top = backOutline.bounds.height - frontOutline.bounds.height + 1f,
                    ) {
                        drawOutline(
                            outline = frontOutline,
                            color = frontBackgroundColor,
                            style = Fill,
                        )
                        drawOutline(
                            outline = frontOutline,
                            color = frontBackgroundColor,
                            style = plainStrokeStyle,
                        )
                        drawOutline(
                            outline = frontOutline,
                            color = strokeColor,
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
            .height(StampSize.height * 0.5f)
            .align(Alignment.BottomCenter)
            .padding(8.dp)
    ) {
        BasicText(
            text = "New Collection",
            style = CollectionViewNameStyle,
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
                CollectionListItem.StampSampleItem(
                    imageUri = Uri.EMPTY,
                    key = "",
                ),
            ),
        ),
        CollectionListItem(
            key = "2",
            name = "RED",
            someStamps = persistentListOf(
                CollectionListItem.StampSampleItem(
                    imageUri = Uri.EMPTY,
                    key = "1",
                ),
                CollectionListItem.StampSampleItem(
                    imageUri = Uri.EMPTY,
                    key = "2",
                ),
            ),
        ),
        CollectionListItem(
            key = "3",
            name = "Food",
            someStamps = persistentListOf(
                CollectionListItem.StampSampleItem(
                    imageUri = Uri.EMPTY,
                    key = "1",
                ),
                CollectionListItem.StampSampleItem(
                    imageUri = Uri.EMPTY,
                    key = "2",
                ),
                CollectionListItem.StampSampleItem(
                    imageUri = Uri.EMPTY,
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
