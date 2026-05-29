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

package ua.com.radiokot.camerapp.intro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.stamps.ui.UiStampShapeA
import ua.com.radiokot.camerapp.ui.LeTextButton
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.Vignette
import ua.com.radiokot.camerapp.ui.paperBackground

@Composable
fun IntroScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) = Column(
    modifier = modifier
        .safeContentPadding()
        .padding(24.dp)
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(
                state = rememberScrollState(),
                overscrollEffect = null,
            )
    ) {
        BasicText(
            text = "Mind a quick intro?",
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

        val textStyle = TextStyle(
            fontFamily = PodkovaFamily,
            fontSize = 20.sp,
        )

        BasicText(
            text = "With this app you can cut digital postage stamps from what your camera sees. " +
                    "For free and without limits.",
            style = textStyle,
            modifier = Modifier
                .fillMaxWidth()
        )

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(UiStampShapeA.size.height * 1.7f)
        ) {
            Image(
                painter = painterResource(R.drawable.flowers_stamp),
                contentDescription = "Flowers",
                modifier = Modifier
                    .padding(
                        top = 24.dp,
                    )
                    .offset(
                        x = UiStampShapeA.size.width * 0.6f,
                    )
                    .size(UiStampShapeA.size)
                    .rotate(4f)
                    .dropShadow(
                        shape = RectangleShape,
                        shadow = Shadow(
                            radius = 4.dp,
                            color = Color(0x7447525E),
                        )
                    )
                    .zIndex(10f)
            )

            Image(
                painter = painterResource(R.drawable.panettone_stamp),
                contentDescription = "Panettone",
                modifier = Modifier
                    .size(UiStampShapeA.size)
                    .offset(
                        x = -UiStampShapeA.size.width * 0.6f,
                        y = UiStampShapeA.size.height * 0.4f,
                    )
                    .rotate(-3f)
                    .dropShadow(
                        shape = RectangleShape,
                        shadow = Shadow(
                            radius = 4.dp,
                            color = Color(0x7447525E),
                        )
                    )
            )
        }

        BasicText(
            text = "The stamps are stored as images in your phone's \"Pictures\" folder. " +
                    "They remain there even if you uninstall the app.",
            style = textStyle,
            modifier = Modifier
                .fillMaxWidth()
        )

        BasicText(
            text = "If you ever need to back up your collection or transfer it to a new phone, " +
                    "just copy the \"PressCutStamps\" contents from the \"Pictures\" folder.",
            style = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 24.dp,
                )
        )

        Image(
            painter = painterResource(R.drawable.onestamp_presscut),
            contentDescription = "Receive stamps from OneStamp",
            modifier = Modifier
                .padding(
                    top = 24.dp,
                )
        )

        BasicText(
            text = "Have friends using OneStamp on iOS? You can receive stamps from them – " +
                    "PressCut opens envelope files.",
            style = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 24.dp,
                )
        )
    }

    Spacer(
        modifier = Modifier
            .height(24.dp)
    )

    LeTextButton(
        text = "All Right",
        onClick = onDone,
    )
}

@Preview
@Composable
private fun IntroScreenPreview() {
    IntroScreen(
        onDone = {},
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
