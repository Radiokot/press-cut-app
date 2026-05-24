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

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.com.radiokot.camerapp.ui.PodkovaFamily

@Composable
fun CollectionActionsScreen(
    modifier: Modifier = Modifier,
    collection: CollectionListItem,
    canDelete: Boolean,
    onMoveStampsAction: () -> Unit,
    onDeleteAction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
        .safeContentPadding()
        .verticalScroll(
            state = rememberScrollState(),
            overscrollEffect = null,
        )
) {
    val detailsAlpha = remember {
        Animatable(0f)
    }

    LaunchedEffect(Unit) {
        delay(100)
        detailsAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400),
        )
    }

    StampBoxView(
        name = collection.name,
        someStamps = collection.someStamps,
        key = collection.key,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Actions(
        canDelete = canDelete,
        onMoveStamps = onMoveStampsAction,
        onDelete = onDeleteAction,
        modifier = Modifier
            .width(StampContainerBaseSize.width * 2.5f)
            .graphicsLayer {
                alpha = detailsAlpha.value
            }
    )

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    canDelete: Boolean,
    onMoveStamps: () -> Unit,
    onDelete: () -> Unit,
) = Column(
    modifier = modifier
        .background(
            color = Color(0xFFfff9eb),
            shape = RoundedCornerShape(cornerRadius),
        )
        .border(
            width = 2.dp,
            color = Color(0xFF6B624B),
            shape = RoundedCornerShape(cornerRadius),
        )
) {
    val textStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            fontWeight = FontWeight.Bold,
        )
    }

    BasicText(
        text = "Move stamps",
        style = textStyle,
        modifier = Modifier
            .clickable(
                onClick = onMoveStamps,
            )
            .padding(
                vertical = 20.dp,
            )
            .fillMaxWidth()
    )

    if (canDelete) {
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color(0xFFcbc4bb))
        )

        BasicText(
            text = "Hold to delete",
            style = textStyle.copy(
                color = Color(0xFFD97D7D),
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
}
