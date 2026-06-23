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

package ua.com.radiokot.camerapp.posters.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.adjustments.ui.AdjustmentsControllerItem
import ua.com.radiokot.camerapp.adjustments.ui.AdjustmentsController
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.LocalColors

@Composable
fun SendStampPosterScreen(
    modifier: Modifier = Modifier,
    imageState: State<ImageBitmap>,
    onSendAction: () -> Unit,
    adjustmentsControllerItems: ImmutableList<AdjustmentsControllerItem>,
) {
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .safeContentPadding()
                .padding(24.dp)
        ) {
            SendStampPosterScreenLayoutContent(
                row = this,
                column = null,
                imageState = imageState,
                onSendAction = onSendAction,
                adjustmentsControllerItems = adjustmentsControllerItems,
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .safeContentPadding()
                .padding(24.dp)
        ) {
            SendStampPosterScreenLayoutContent(
                row = null,
                column = this,
                imageState = imageState,
                onSendAction = onSendAction,
                adjustmentsControllerItems = adjustmentsControllerItems,
            )
        }
    }
}

@Composable
private fun SendStampPosterScreenLayoutContent(
    row: RowScope?,
    column: ColumnScope?,
    imageState: State<ImageBitmap>,
    onSendAction: () -> Unit,
    adjustmentsControllerItems: ImmutableList<AdjustmentsControllerItem>,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .run {
                if (column != null) {
                    with(column) {
                        weight(1f)
                    }
                } else {
                    this
                }
            }
            .padding(
                vertical =
                    if (column != null)
                        24.dp
                    else
                        0.dp,
                horizontal =
                    if (row != null)
                        24.dp
                    else
                        0.dp,
            )
    ) {
        val shape = RoundedCornerShape(10.dp)
        val height = maxHeight.coerceAtMost(500.dp)

        Image(
            bitmap = imageState.value,
            contentDescription = "Poster preview",
            modifier = Modifier
                .size(
                    height = height,
                    width = height * 0.5625f,
                )
                .border(
                    width = 2.dp,
                    color = LocalColors.current.componentStroke,
                    shape = shape,
                )
                .clip(shape)
        )
    }

    Column(
        modifier = Modifier
            .run {
                if (row != null) {
                    this
                        .fillMaxWidth(0.65f)
                        .padding(
                            horizontal = 24.dp,
                        )
                } else {
                    this
                }
            }
    ) {
        AdjustmentsController(
            items = adjustmentsControllerItems,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(
            modifier = Modifier
                .height(24.dp)
        )

        LeTextButton(
            text = "Send",
            onClick = onSendAction,
        )
    }
}

@PreviewLightDark
@Composable
private fun SendStampPosterScreenPreview() {
    val adjustmentsControllerItems = remember {
        val themeValueState = mutableIntStateOf(0)
        val captionValueState = mutableIntStateOf(0)

        persistentListOf(
            AdjustmentsControllerItem.Dial(
                title = "Theme",
                defaultValue = 0,
                minValue = 0,
                maxValue = 1,
                valueState = themeValueState,
                onValueChanged = themeValueState::intValue::set,
                key = "t",
            ),
            AdjustmentsControllerItem.Dial(
                title = "Caption",
                defaultValue = 0,
                minValue = 0,
                maxValue = 1,
                valueState = captionValueState,
                onValueChanged = captionValueState::intValue::set,
                key = "c",
            ),
        )
    }
    val context = LocalContext.current
    val image = remember {
        ContextCompat
            .getDrawable(context, R.drawable.panettone_stamp)!!
            .toBitmap(
                width = 400,
                height = 711,
                config = Bitmap.Config.RGB_565,
            )
            .asImageBitmap()
    }

    AppTheme {
        SendStampPosterScreen(
            imageState = image.let(::mutableStateOf),
            onSendAction = {},
            adjustmentsControllerItems = adjustmentsControllerItems,
            modifier = Modifier
                .fillMaxSize()
                .background(LocalColors.current.screenBackground)
        )
    }
}
