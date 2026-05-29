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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.stamps.ui.StampBoxView
import ua.com.radiokot.camerapp.stamps.ui.StampSampleItem
import ua.com.radiokot.camerapp.stamps.ui.UiStampShapeA
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.Vignette
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.StableHolder

@Composable
fun EnvelopePreviewScreen(
    modifier: Modifier = Modifier,
    errorMessage: String?,
    someStamps: ImmutableList<StampSampleItem>,
    stampCount: Int,
    message: String?,
    onSaveAction: () -> Unit,
    onErrorAcknowledged: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .safeContentPadding()
        .padding(24.dp)
) {
    BasicText(
        text = "You received stamps",
        style = TextStyle(
            fontFamily = PodkovaFamily,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .fillMaxWidth()
    )

    Vignette(
        modifier = Modifier
            .padding(
                vertical = 32.dp,
            )
    )

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
        if (errorMessage != null) {
            BasicText(
                text = errorMessage,
                style = TextStyle(
                    fontFamily = PodkovaFamily,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 72.dp,
                    )
            )
            return@Column
        }

        if (message != null) {
            BasicText(
                text = "«$message»",
                style = TextStyle(
                    fontFamily = PodkovaFamily,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 32.dp,
                    )
            )
        }

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

        Spacer(
            modifier = Modifier
                .height(72.dp)
        )
    }

    if (errorMessage == null) {
        LeTextButton(
            text = "Save",
            onClick = onSaveAction,
        )
    } else {
        LeTextButton(
            text = "Too Bad",
            onClick = onErrorAcknowledged,
        )
    }
}

@Preview
@Composable
private fun EnvelopePreviewScreenPreview() {
    EnvelopePreviewScreen(
        stampCount = 10,
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
        message = "From Oleg",
        errorMessage = null,
        onSaveAction = {},
        onErrorAcknowledged = {},
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
