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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.skydoves.landscapist.image.LocalLandscapist
import org.koin.compose.koinInject
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionContract
import ua.com.radiokot.camerapp.collectionselection.ui.selectDestinationCollectionDestination
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.StableHolder

class OpenEnvelopeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        super.onCreate(savedInstanceState)

        val intentData = intent.data
        if (intentData == null) {
            finish()
            return
        }

        val oneStampPackageContentUri = StableHolder(intentData)

        setContent {
            CompositionLocalProvider(
                LocalLandscapist provides koinInject(),
            ) {
                OpenEnvelopeNavHost(
                    oneStampPackageContentUri = oneStampPackageContentUri,
                    modifier = Modifier
                        .fillMaxSize()
                        .paperBackground()
                )
            }
        }
    }
}

@Composable
private fun OpenEnvelopeNavHost(
    modifier: Modifier = Modifier,
    oneStampPackageContentUri: StableHolder<Uri>,
) {
    val navController = rememberNavController()
    val selectDestinationCollectionContract =
        SelectDestinationCollectionContract(
            navController = navController,
        )

    NavHost(
        navController = navController,
        startDestination =
            EnvelopePreviewRoute(
                oneStampPackageContentUri = oneStampPackageContentUri.value,
            ),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        modifier = modifier
    ) {
        envelopePreviewDestination(
            selectDestinationCollectionContract = selectDestinationCollectionContract,
            onProceedToSaveStamps = { collectionId ->
                // TODO
            }
        )

        selectDestinationCollectionDestination(
            contract = selectDestinationCollectionContract,
        )
    }
}
