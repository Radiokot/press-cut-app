package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.noProgressive
import ua.com.radiokot.camerapp.util.plus
import kotlin.math.absoluteValue

@Composable
fun StampsScreen(
    modifier: Modifier = Modifier,
    collectionId: String,
    collectionNameInputState: TextFieldState,
    focusCollectionNameInput: Boolean,
    stamps: State<ImmutableList<StampListItem>>,
    onStampClicked: (StampListItem) -> Unit,
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
    val spacedBy = Arrangement.spacedBy(24.dp)
    val shadowColor = Color(0x7447525E)
    val rotationAngles = remember {
        intArrayOf(4, 3, 2, -2, -3, -4)
    }
    val safeContentPadding =
        WindowInsets.safeContent.asPaddingValues()
    val contentPadding =
        safeContentPadding +
                PaddingValues(
                    // Button height and spacing.
                    bottom = 120.dp,
                )
    val nameInputFocusRequester = remember(::FocusRequester)
    if (focusCollectionNameInput) {
        LaunchedEffect(Unit) {
            nameInputFocusRequester.requestFocus()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(
            minSize = StampSize.width * 1.05f,
        ),
        verticalArrangement = spacedBy,
        contentPadding = contentPadding,
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

                Image(
                    painter = painterResource(R.drawable.element_by_lisa_krymova_from_noun_project),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFFB9AC8C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 32.dp,
                        )
                )
            }
        }
        items(
            items = stamps.value,
            key = StampListItem::key,
        ) { stamp ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(StampSize.height * 1.05f)
            ) {
                LandscapistImage(
                    imageModel = { stamp.thumbnailUrl.toUri() },
                    requestBuilder = ImageRequest.Builder::noProgressive,
                    modifier = Modifier
                        .size(StampSize)
                        .run {
                            if (stamp.thumbnailUrl.isNotEmpty()) {
                                return@run this
                            }

                            background(Color.Yellow)
                        }
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
                        .rotate(
                            (rotationAngles[stamp.key.hashCode().absoluteValue % rotationAngles.size])
                                .toFloat()
                        )
                        .dropShadow(
                            shape = RectangleShape,
                            shadow = Shadow(
                                radius = 4.dp,
                                color = shadowColor,
                            )
                        )
                        .clickable(
                            indication = null,
                            interactionSource = null,
                            onClick = {
                                onStampClicked(stamp)
                            },
                        )
                )
            }
        }
    }

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

@Preview
@Composable
private fun StampsScreenPreview(

) {
    val stamps = (1..5)
        .map { i ->
            StampListItem(
                thumbnailUrl = "",
                key = i.toString(),
            )
        }
        .toPersistentList()

    StampsScreen(
        modifier = Modifier
            .fillMaxSize(),
        collectionId = "",
        collectionNameInputState = TextFieldState("My stamps"),
        focusCollectionNameInput = false,
        stamps = stamps.let(::mutableStateOf),
        onStampClicked = { },
        onNewStampAction = { },
        sharedTransitionScope = null,
        animatedVisibilityScope = null
    )
}
