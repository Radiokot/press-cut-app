package ua.com.radiokot.camerapp.about.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import ua.com.radiokot.camerapp.stamps.ui.StampSize
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.ui.podkovaFamily

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onProceedToUrl: (url: String) -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
        .verticalScroll(
            state = rememberScrollState()
        )
        .safeContentPadding()
        .padding(24.dp)
) {
    BasicText(
        text = "Camera app",
        style = TextStyle(
            fontFamily = podkovaFamily,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier
            .padding(
                top = 24.dp,
            )
            .fillMaxWidth()
    )

    Image(
        painter = painterResource(R.drawable.element_by_lisa_krymova_from_noun_project),
        contentDescription = null,
        colorFilter = ColorFilter.tint(Color(0xFFB9AC8C)),
        modifier = Modifier
            .fillMaxWidth()
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

    BasicText(
        text = aboutTextAnnotated,
        style = TextStyle(
            fontFamily = podkovaFamily,
            fontSize = 20.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
    )

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
                x = StampSize.width * 0.6f,
            )
            .size(StampSize)
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
            .size(StampSize)
            .offset(
                x = -StampSize.width * 0.6f,
                y = -StampSize.height * 0.6f,
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
