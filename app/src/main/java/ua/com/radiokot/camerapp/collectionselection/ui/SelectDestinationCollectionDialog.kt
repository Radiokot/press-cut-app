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

package ua.com.radiokot.camerapp.collectionselection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.collections.immutable.ImmutableList
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily

@Composable
fun SelectDestinationCollectionDialog(
    message: SelectDestinationCollectionDialogMessage,
    collections: ImmutableList<String>,
    onCollectionSelected: (Int) -> Unit,
    onNewCollectionAction: () -> Unit,
    onCancel: () -> Unit,
) = Dialog(
    onDismissRequest = onCancel,
) {
    val colors = LocalColors.current
    val actionTextStyle = remember(colors) {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
    }
    val collectionTextStyle = remember(colors) {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            color = colors.textPrimary,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.componentBackground,
                shape = RoundedCornerShape(10.dp),
            )
    ) {
        BasicText(
            text = when (message) {
                SelectDestinationCollectionDialogMessage.MoveStamps ->
                    "Which collection to move the stamp to?"

                SelectDestinationCollectionDialogMessage.MoveStamp ->
                    "Which collection to move the stamp to?"

                SelectDestinationCollectionDialogMessage.SaveStamps ->
                    "Which collection to save the stamps to?"
            },
            style = actionTextStyle.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 48.dp,
                    horizontal = 16.dp,
                )
        )

        LazyColumn(
            overscrollEffect = null,
            modifier = Modifier
                .weight(
                    weight = 1f,
                    fill = false,
                )
        ) {
            itemsIndexed(
                items = collections,
            ) { i, collection ->
                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(colors.componentDivider)
                )

                BasicText(
                    text = collection,
                    style = collectionTextStyle,
                    modifier = Modifier
                        .clickable(
                            onClick = {
                                onCollectionSelected(i)
                            },
                        )
                        .padding(
                            vertical = 20.dp,
                            horizontal = 16.dp,
                        )
                        .fillMaxWidth()
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(colors.componentDivider)
        )

        BasicText(
            text = "Create a new one",
            style = actionTextStyle,
            modifier = Modifier
                .clickable(
                    onClick = onNewCollectionAction,
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
            text = "Cancel",
            style = actionTextStyle,
            modifier = Modifier
                .clickable(
                    onClick = onCancel,
                )
                .padding(
                    vertical = 20.dp,
                )
                .fillMaxWidth()
        )
    }
}
