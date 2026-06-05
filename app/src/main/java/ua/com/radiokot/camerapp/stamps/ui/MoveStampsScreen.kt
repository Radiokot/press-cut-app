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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.com.radiokot.camerapp.ui.LocalColors
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.ProgressBar
import ua.com.radiokot.camerapp.ui.paperBackground

/**
 * @param progressState value in [0..1] range
 */
@Composable
fun MoveStampsScreen(
    modifier: Modifier = Modifier,
    progressState: FloatState,
) = Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .safeContentPadding()
        .padding(24.dp),
) {
    BasicText(
        text = "Moving the stamps…",
        style = TextStyle(
            fontFamily = PodkovaFamily,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = LocalColors.current.textPrimary,
        )
    )

    Spacer(
        modifier = Modifier
            .height(8.dp)
    )

    ProgressBar(
        progressState = progressState,
        modifier = Modifier
            .fillMaxWidth()
    )

    Spacer(
        modifier = Modifier
            .height(56.dp)
    )
}

@Composable
@Preview
private fun MoveStampsScreenPreview() {
    MoveStampsScreen(
        progressState = 0.5f.let(::mutableFloatStateOf),
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
