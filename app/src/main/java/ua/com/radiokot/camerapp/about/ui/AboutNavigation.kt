package ua.com.radiokot.camerapp.about.ui

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable


fun NavGraphBuilder.aboutDestination(

) = composable(
    route = AboutRoute,
    enterTransition = {
        fadeIn() + slideInVertically(
            initialOffsetY = { height -> height / 2 },
        )
    },
    exitTransition = { fadeOut() },
) {
    val context = LocalContext.current
    AboutScreen(
        onProceedToUrl = { url ->
            if (url.startsWith("/android_asset/")) {
                context.startActivity(
                    Intent(context, HtmlActivity::class.java).putExtras(
                        HtmlActivity.getBundle(
                            androidAssetPath = url,
                        )
                    )
                )
            } else {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                val chooser = Intent.createChooser(intent, "Open with")
                context.startActivity(chooser)
            }
        },
        modifier = Modifier
            .fillMaxSize()
    )
}

const val AboutRoute = "about"
