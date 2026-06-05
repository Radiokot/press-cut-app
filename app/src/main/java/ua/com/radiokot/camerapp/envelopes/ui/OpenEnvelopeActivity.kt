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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.skydoves.landscapist.image.LocalLandscapist
import org.koin.compose.koinInject
import ua.com.radiokot.camerapp.collectionselection.ui.SelectDestinationCollectionContract
import ua.com.radiokot.camerapp.collectionselection.ui.selectDestinationCollectionDestination
import ua.com.radiokot.camerapp.envelopes.domain.EnvelopePreviewResult
import ua.com.radiokot.camerapp.ui.AppTheme
import ua.com.radiokot.camerapp.ui.paperBackground
import ua.com.radiokot.camerapp.util.StableHolder

@Immutable
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

        val oneStampEnvelopeContentUri = StableHolder(intentData)

        setContent {
            AppTheme(
                LocalLandscapist provides koinInject(),
            ) {
                OpenEnvelopeNavHost(
                    oneStampEnvelopeContentUri = oneStampEnvelopeContentUri,
                    onDone = {
                        Toast.makeText(
                            this@OpenEnvelopeActivity,
                            "Stamps saved",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
                    onErrorAcknowledged = {
                        Toast.makeText(
                            this@OpenEnvelopeActivity,
                            "Sorry",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
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
    oneStampEnvelopeContentUri: StableHolder<Uri>,
    onDone: () -> Unit,
    onErrorAcknowledged: () -> Unit,
) {
    val navController = rememberNavController()
    val selectDestinationCollectionContract =
        SelectDestinationCollectionContract(
            navController = navController,
        )
    val envelopePreviewToSaveState = remember {
        mutableStateOf<EnvelopePreviewResult.Preview?>(null)
    }

    NavHost(
        navController = navController,
        startDestination =
            EnvelopePreviewRoute(
                oneStampEnvelopeContentUri = oneStampEnvelopeContentUri.value,
            ),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        modifier = modifier
    ) {
        envelopePreviewDestination(
            selectDestinationCollectionContract = selectDestinationCollectionContract,
            onProceedToSaveStamps = {
                    collectionId,
                    envelopePreview,
                ->
                envelopePreviewToSaveState.value = envelopePreview
                navController
                    .navigate(
                        route = SaveEnvelopeStampsRoute(
                            destinationCollectionId = collectionId,
                        )
                    ) {
                        launchSingleTop = true
                    }
            },
            onErrorAcknowledged = onErrorAcknowledged,
        )

        selectDestinationCollectionDestination(
            contract = selectDestinationCollectionContract,
        )

        saveEnvelopeStampsDestination(
            envelopePreviewState = envelopePreviewToSaveState,
            onDone = onDone,
        )
    }
}
