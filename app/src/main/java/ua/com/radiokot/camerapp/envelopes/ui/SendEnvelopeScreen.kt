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

package ua.com.radiokot.camerapp.envelopes.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.stamps.ui.CaptionInput
import ua.com.radiokot.camerapp.stamps.ui.StampBoxView
import ua.com.radiokot.camerapp.stamps.ui.StampSampleItem
import ua.com.radiokot.camerapp.stamps.ui.UiStampShapeA
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.StableHolder

@Composable
fun SendEnvelopeScreen(
    modifier: Modifier = Modifier,
    messageInputState: TextFieldState,
    someStamps: ImmutableList<StampSampleItem>,
    stampCount: Int,
    onSendAction: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .safeContentPadding()
        .imePadding()
        .padding(24.dp)
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(
                state = rememberScrollState(),
                overscrollEffect = null,
            )
    ) {
        val messageFocusRequester = remember(::FocusRequester)
        LaunchedEffect(messageFocusRequester) {
            messageFocusRequester.requestFocus()
        }

        CaptionInput(
            inputState = messageInputState,
            hint = "A memo",
            focusRequester = messageFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 32.dp
                )
        )

        StampBoxView(
            name = pluralStringResource(
                R.plurals.stamp_count,
                stampCount,
                stampCount,
            ),
            someStamps = someStamps,
            key = "envelope",
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
        )
    }

    BasicText(
        text = "On iOS, your stamps can be received in OneStamp",
        style = TextStyle(
            fontFamily = PodkovaFamily,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = LocalColors.current.textSecondary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 16.dp,
            )
    )

    LeTextButton(
        text = "Send",
        onClick = onSendAction,
    )
}

@Preview
@Composable
private fun SendEnvelopeScreenPreview() {
    SendEnvelopeScreen(
        messageInputState = TextFieldState("My message for the receiver"),
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
        stampCount = 1,
        onSendAction = {},
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
