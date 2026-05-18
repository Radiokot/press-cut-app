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

package ua.com.radiokot.camerapp.about.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ua.com.radiokot.camerapp.R
import ua.com.radiokot.camerapp.stamps.ui.UiStampShapeA
import ua.com.radiokot.camerapp.ui.PodkovaFamily
import ua.com.radiokot.camerapp.ui.Vignette
import ua.com.radiokot.camerapp.ui.paperBackground

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onProceedToUrl: (url: String) -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .verticalScroll(
            state = rememberScrollState(),
            overscrollEffect = null,
        )
        .safeContentPadding()
        .padding(24.dp)
) {
    BasicText(
        text = "About",
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

    val aboutTextHtml = stringResource(R.string.about_text)
    val aboutTextAnnotated = retain(
        aboutTextHtml,
        onProceedToUrl,
    ) {
        AnnotatedString.fromHtml(
            htmlString = aboutTextHtml,
            linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = Color(0xFF85794B),
                    textDecoration = TextDecoration.Underline,
                )
            ),
            linkInteractionListener = {
                if (it is LinkAnnotation.Url) {
                    onProceedToUrl(it.url)
                }
            },
        )
    }

    val textStyle = TextStyle(
        fontFamily = PodkovaFamily,
        fontSize = 20.sp,
    )

    BasicText(
        text = aboutTextAnnotated,
        style = textStyle,
        modifier = Modifier
            .fillMaxWidth()
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(UiStampShapeA.size.height * 1.8f)
    ) {
        val negativeColorFilter = retain {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                    )
                )
            )
        }
        var isNegativeColorFilterApplied by remember {
            mutableStateOf(false)
        }

        Image(
            painter = painterResource(R.drawable.mrcat_stamp),
            contentDescription = "Oleg K",
            colorFilter =
                if (isNegativeColorFilterApplied)
                    negativeColorFilter
                else
                    null,
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
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = { isNegativeColorFilterApplied = !isNegativeColorFilterApplied },
                )
                .zIndex(10f)
        )

        Image(
            painter = painterResource(R.drawable.flowers_stamp),
            contentDescription = "Flowers",
            modifier = Modifier
                .size(UiStampShapeA.size)
                .offset(
                    x = -UiStampShapeA.size.width * 0.6f,
                    y = UiStampShapeA.size.height * 0.6f,
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
        text = "Reminder: The stamps are stored as images in your phone's \"Pictures\" folder.",
        style = textStyle,
        modifier = Modifier
            .fillMaxWidth()
    )
}

@Preview
@Composable
private fun AboutScreenPreview() {
    AboutScreen(
        onProceedToUrl = {},
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(
                drawBackgroundColor = true,
            )
    )
}
