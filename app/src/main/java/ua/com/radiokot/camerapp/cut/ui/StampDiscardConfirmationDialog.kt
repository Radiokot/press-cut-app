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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ua.com.radiokot.camerapp.ui.PodkovaFamily

@Composable
fun StampDiscardConfirmationDialog(
    onConfirmDiscard: () -> Unit,
    onCancel: () -> Unit,
) = Dialog(
    onDismissRequest = onCancel,
) {
    val actionTextStyle = remember {
        TextStyle(
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontFamily = PodkovaFamily,
            fontWeight = FontWeight.Bold,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFfff9eb),
                shape = RoundedCornerShape(10.dp),
            )
    ) {
        BasicText(
            text = "Discard this stamp?",
            style = actionTextStyle.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 48.dp,
                )
        )

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color(0xFFcbc4bb))
        )

        BasicText(
            text = "No, keep editing",
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

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color(0xFFcbc4bb))
        )

        BasicText(
            text = "Yes, discard",
            style = actionTextStyle,
            modifier = Modifier
                .clickable(onClick = onConfirmDiscard)
                .padding(
                    vertical = 20.dp,
                )
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun StampConfirmationDiscardDialogPreview(

) {
    StampDiscardConfirmationDialog(
        onConfirmDiscard = {},
        onCancel = {},
    )
}
