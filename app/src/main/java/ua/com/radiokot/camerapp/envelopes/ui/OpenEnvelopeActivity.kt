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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.image.LocalLandscapist
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.compose.koinInject
import ua.com.radiokot.camerapp.envelopes.domain.GetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.stamps.ui.UiStampShape
import ua.com.radiokot.camerapp.util.EmptyImageComponent

class OpenEnvelopeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(0, 0),
        )
        super.onCreate(savedInstanceState)

        val envelopePreview = runBlocking {
            get<GetOneStampEnvelopePreviewUseCase>()
                .invoke(intent.data!!)
        }

        setContent {
            CompositionLocalProvider(
                LocalLandscapist provides koinInject(),
            ) {
                Column(
                    modifier = Modifier
                        .safeContentPadding()
                ) {
                    BasicText(
                        text = "Message: ${envelopePreview.message}",
                    )
                    BasicText(
                        text = "Stamps: ${envelopePreview.stampCount}"
                    )
                    envelopePreview.someStamps.forEach { stamp ->
                        BasicText(
                            text = "Stamp '${stamp.caption}' taken at ${stamp.takenAtLocal}"
                        )
                        val uiShape = UiStampShape.fromShape(stamp.shape)
                        val imageOptions = uiShape.getListImageLoadingOptions(LocalDensity.current)
                        LandscapistImage(
                            imageModel = stamp.imageUri::toUri,
                            component = EmptyImageComponent,
                            imageOptions = imageOptions.imageOptions,
                            requestBuilder = imageOptions.requestBuilder,
                            modifier = Modifier
                                .size(uiShape.size)
                        )
                    }
                }
            }
        }
    }
}
