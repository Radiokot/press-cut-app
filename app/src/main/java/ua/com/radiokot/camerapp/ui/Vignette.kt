package ua.com.radiokot.camerapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import ua.com.radiokot.camerapp.R

@Composable
fun Vignette(
    modifier: Modifier = Modifier
) = Image(
    painter = painterResource(R.drawable.element_by_lisa_krymova_from_noun_project),
    contentDescription = null,
    colorFilter = ColorFilter.tint(Color(0xFFB9AC8C)),
    modifier = modifier
        .fillMaxWidth()
)
