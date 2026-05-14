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
