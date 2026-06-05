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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.Vignette
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.EmptyImageComponent
import ua.com.radiokot.camerapp.util.StableHolder
import ua.com.radiokot.camerapp.util.plus
import kotlin.math.absoluteValue

@Composable
fun StampsScreen(
    modifier: Modifier = Modifier,
    collectionId: String,
    collectionNameInputState: TextFieldState,
    focusCollectionNameInput: Boolean,
    showGiftMessage: Boolean,
    stamps: State<ImmutableList<StampsScreenItem>>,
    onStampClicked: (StampsScreenItem) -> Unit,
    onStampLongClicked: (StampsScreenItem) -> Unit,
    selectedCountState: IntState,
    onMoveSelectedAction: () -> Unit,
    onSendSelectedAction: () -> Unit,
    onDeleteSelectedAction: () -> Unit,
    onNewStampAction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = Box(
    modifier
        .run {
            if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                return@run this
            }

            with(sharedTransitionScope) {
                sharedBounds(
                    sharedContentState = rememberSharedContentState("$collectionId-box-front"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    zIndexInOverlay = 10f,
                )
            }
        }
) {
    val shadowColor = LocalColors.current.stampShadow
    val rotationAngles = retain {
        floatArrayOf(4f, 3f, 2f, -2f, -3f, -4f)
    }
    val safeContentPadding =
        WindowInsets.safeContent.asPaddingValues()
    val contentPadding =
        safeContentPadding + PaddingValues(
            bottom = 120.dp,
        )

    val nameInputFocusRequester = remember(::FocusRequester)
    if (focusCollectionNameInput) {
        LaunchedEffect(Unit) {
            nameInputFocusRequester.requestFocus()
        }
    }
    val selectionAnimationSpec = retain {
        spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }
    val density = LocalDensity.current

    LazyVerticalGrid(
        columns = GridCells.FixedSize(StampContainerBaseSize.width * 1.15f),
        horizontalArrangement = Arrangement.SpaceAround,
        contentPadding = contentPadding,
        overscrollEffect = null,
        modifier = Modifier
            .fillMaxSize()
    ) {
        item(
            span = {
                GridItemSpan(maxCurrentLineSpan)
            },
            key = "name",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 24.dp,
                    )
            ) {
                CaptionInput(
                    hint = "A name",
                    focusRequester = nameInputFocusRequester,
                    inputState = collectionNameInputState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                                return@run this
                            }

                            with(sharedTransitionScope) {
                                sharedElement(
                                    sharedContentState = rememberSharedContentState("$collectionId-name"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    zIndexInOverlay = 20f,
                                )
                            }
                        }
                )

                Vignette(
                    modifier = Modifier
                        .padding(
                            top = 32.dp,
                        )
                )

                if (showGiftMessage) {
                    BasicText(
                        text = "Please take these stamps as a gift. " +
                                "I hope you'll enjoy collecting your own!",
                        style = TextStyle(
                            fontFamily = PodkovaFamily,
                            fontSize = 16.sp,
                            color = LocalColors.current.textSecondary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 32.dp,
                                start = 24.dp,
                                end = 24.dp,
                            )
                    )
                }
            }
        }

        items(
            items = stamps.value,
            key = StampsScreenItem::key,
        ) { stamp ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(StampContainerBaseSize.height * 1.2f)
                    .animateItem()
            ) {
                val selectionAnimationProgressState = animateFloatAsState(
                    targetValue =
                        if (stamp.isSelected)
                            1f
                        else
                            0f,
                    animationSpec = selectionAnimationSpec,
                )
                val stampImageLoadingOptions =
                    stamp
                        .shape
                        .getListImageLoadingOptions(
                            density = density,
                        )

                LandscapistImage(
                    imageModel = stamp.imageUri::value,
                    requestBuilder = stampImageLoadingOptions.requestBuilder,
                    imageOptions = stampImageLoadingOptions.imageOptions,
                    component = EmptyImageComponent,
                    modifier = Modifier
                        .size(stamp.shape.size * stamp.shape.fitContainerSizeScale)
                        .run {
                            if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                                return@run this
                            }

                            with(sharedTransitionScope) {
                                sharedElement(
                                    sharedContentState = rememberSharedContentState(stamp.key),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                        }
                        .graphicsLayer {
                            scaleX = 1f - 0.1f * selectionAnimationProgressState.value
                            scaleY = scaleX
                            rotationZ =
                                (rotationAngles[stamp.key.hashCode().absoluteValue % rotationAngles.size])
                        }
                        .dropShadow(
                            shape = RectangleShape,
                            shadow = Shadow(
                                radius = 4.dp,
                                color = shadowColor,
                            )
                        )
                        .run {
                            if (stamp.imageUri.value !== Uri.EMPTY) {
                                return@run this
                            }

                            background(Color.Yellow)
                        }
                        .selectionEnvelope(
                            animationProgressState = selectionAnimationProgressState,
                            heightFraction = when (stamp.shape) {
                                UiStampShapeOneStampSquare -> 0.7f
                                UiStampShapeOneStampLandscape -> 0.8f
                                else -> 0.6f
                            },
                        )
                        .combinedClickable(
                            indication = null,
                            interactionSource = null,
                            onClick = {
                                onStampClicked(stamp)
                            },
                            onLongClick = {
                                onStampLongClicked(stamp)
                            },
                        )
                )
            }
        }
    }

    var visibleSelectedCount by rememberSaveable {
        mutableIntStateOf(0)
    }
    var areSelectionActionsVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val isSelectionVisible by remember {
        derivedStateOf {
            if (selectedCountState.intValue > 0) {
                visibleSelectedCount = selectedCountState.intValue
                true
            } else {
                areSelectionActionsVisible = false
                false
            }
        }
    }

    AnimatedVisibility(
        visible = areSelectionActionsVisible,
        enter =
            fadeIn() + slideInVertically(
                initialOffsetY = { height ->
                    height / 2
                },
            ),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(contentPadding)
            .padding(
                horizontal = 24.dp,
            )
            .width(StampContainerBaseSize.width * 2.5f)
    ) {
        SelectionActions(
            onMove = {
                areSelectionActionsVisible = false
                onMoveSelectedAction()
            },
            onSend = {
                areSelectionActionsVisible = false
                onSendSelectedAction()
            },
            onDelete = {
                areSelectionActionsVisible = false
                onDeleteSelectedAction()
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        BackHandler {
            areSelectionActionsVisible = false
        }
    }

    AnimatedContent(
        targetState = isSelectionVisible,
        modifier = Modifier
            .padding(safeContentPadding)
            .padding(24.dp)
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) { isSelectionVisible ->
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (isSelectionVisible) {
                SelectionController(
                    selectedCount = visibleSelectedCount,
                    onActionsClicked = {
                        areSelectionActionsVisible = !areSelectionActionsVisible
                    },
                    modifier = Modifier
                        .padding(
                            bottom = 10.dp,
                        )
                )
            } else {
                LeTextButton(
                    text = "New Stamp",
                    onClick = onNewStampAction,
                    modifier = Modifier
                        .fillMaxWidth()
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
        }
    }
}

@Composable
private fun SelectionController(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    onActionsClicked: () -> Unit,
) {
    val colors = LocalColors.current
    val cornerRadius = 10.dp
    val textStyle = remember(colors) {
        TextStyle(
            fontFamily = PodkovaFamily,
            fontSize = 20.sp,
            color = colors.textPrimary,
        )
    }
    val density = LocalDensity.current
    val spDp = remember(density) {
        with(density) {
            1.sp.toDp()
        }
    }

    Row(
        modifier = modifier
            .background(
                color = colors.componentBackground,
                shape = RoundedCornerShape(cornerRadius),
            )
            .border(
                width = 2.dp,
                color = colors.componentStroke,
                shape = RoundedCornerShape(cornerRadius),
            )
            .height(IntrinsicSize.Max)
    ) {
        Spacer(
            modifier = Modifier
                .width(24.dp)
        )

        BasicText(
            text = "Picked ",
            style = textStyle,
            modifier = Modifier
                .padding(
                    vertical = 16.dp,
                )
        )

        // All the width shenanigans are to prevent slight width changes
        // due to variable digit width.
        val selectedCountString = selectedCount.toString()
        BasicText(
            text = selectedCountString,
            style = textStyle,
            modifier = Modifier
                .padding(
                    vertical = 16.dp,
                )
                .width(24.dp + (spDp * 10f * selectedCountString.length))
        )

        Spacer(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.componentDivider)
        )

        Image(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = "Actions",
            colorFilter = ColorFilter.tint(colors.textPrimary),
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    onClick = onActionsClicked,
                )
                .padding(
                    start = 14.dp,
                    end = 16.dp,
                )
        )
    }
}

@Composable
@PreviewLightDark
private fun SelectionControllerPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .paperBackground(
                    drawBackgroundColor = true,
                )
                .padding(24.dp)
        ) {
            SelectionController(
                selectedCount = 24,
                onActionsClicked = {}
            )
        }
    }
}

@Composable
private fun SelectionActions(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    onMove: () -> Unit,
    onSend: () -> Unit,
    onDelete: () -> Unit,
) = Column(
    modifier = modifier
        .background(
            color = LocalColors.current.componentBackground,
            shape = RoundedCornerShape(cornerRadius),
        )
        .border(
            width = 2.dp,
            color = LocalColors.current.componentStroke,
            shape = RoundedCornerShape(cornerRadius),
        )
) {
    val colors = LocalColors.current
    val textStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
    }

    BasicText(
        text = "Move",
        style = textStyle,
        modifier = Modifier
            .clickable(
                onClick = onMove,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )

    Spacer(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(colors.componentDivider)
    )

    BasicText(
        text = "Send",
        style = textStyle,
        modifier = Modifier
            .clickable(
                onClick = onSend,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )

    Spacer(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(colors.componentDivider)
    )

    BasicText(
        text = "Hold to delete",
        style = textStyle.copy(
            color = colors.textDanger,
        ),
        modifier = Modifier
            .holdToDeleteAction(
                roundedCornerRadius = cornerRadius,
                areTopCornersRounded = false,
                onDelete = onDelete,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )
}

@Composable
fun StampsScreenDummy(
    modifier: Modifier = Modifier,
) {
    val stamps = remember {
        (1..6)
            .map { i ->
                StampsScreenItem(
                    imageUri = StableHolder(Uri.EMPTY),
                    shape = UiStampShapeA,
                    isSelected = false,
                    key = i.toString(),
                )
            }
            .toPersistentList()
    }

    StampsScreen(
        collectionId = "",
        collectionNameInputState = TextFieldState("My stamps"),
        focusCollectionNameInput = false,
        showGiftMessage = true,
        stamps = stamps.let(::mutableStateOf),
        onStampClicked = { },
        onStampLongClicked = { },
        selectedCountState = 0.let(::mutableIntStateOf),
        onMoveSelectedAction = { },
        onSendSelectedAction = { },
        onDeleteSelectedAction = { },
        onNewStampAction = { },
        sharedTransitionScope = null,
        animatedVisibilityScope = null,
        modifier = modifier
    )
}

@PreviewLightDark
@Composable
private fun StampsScreenPreview() {
    AppTheme {
        StampsScreenDummy(
            modifier = Modifier
                .fillMaxSize()
                .paperBackground(
                    drawBackgroundColor = true,
                )
        )
    }
}
