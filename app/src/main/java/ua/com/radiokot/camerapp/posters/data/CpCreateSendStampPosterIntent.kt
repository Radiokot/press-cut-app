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

package ua.com.radiokot.camerapp.posters.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.BuildConfig
import ua.com.radiokot.camerapp.posters.domain.CreateSendStampPosterIntent
import ua.com.radiokot.camerapp.posters.domain.StampPosterDensity
import ua.com.radiokot.camerapp.posters.domain.StampPosterLayer
import ua.com.radiokot.camerapp.posters.domain.StampPosterSize
import ua.com.radiokot.camerapp.posters.domain.drawStampPoster
import ua.com.radiokot.camerapp.util.drawToBitmap
import java.io.File
import java.io.OutputStream
import kotlin.io.outputStream
import kotlin.use

class CpCreateSendStampPosterIntent(
    private val context: Context,
    private val stampPosterProviderDirectory: File,
) : CreateSendStampPosterIntent {

    private val posterBitmap = createBitmap(
        width = StampPosterSize.width.toInt(),
        height = StampPosterSize.height.toInt(),
        config = Bitmap.Config.RGB_565,
    )

    override suspend fun invoke(
        layers: Collection<StampPosterLayer>,
        isDark: Boolean,
    ): Intent {

        if (!stampPosterProviderDirectory.exists()) {
            stampPosterProviderDirectory.mkdirs()
        }

        val posterFile = File(stampPosterProviderDirectory, "poster.png")

        posterFile.outputStream().use { outputStream ->
            renderPosterAsPng(
                layers = layers,
                isDark = isDark,
                outputStream = outputStream,
            )
        }

        val uri = FileProvider.getUriForFile(
            context,
            BuildConfig.stampPosterContentProviderAuthority,
            posterFile,
            posterFile.name,
        )

        return Intent(Intent.ACTION_SEND)
            .setDataAndType(uri, "image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private suspend fun renderPosterAsPng(
        layers: Collection<StampPosterLayer>,
        isDark: Boolean,
        outputStream: OutputStream,
    ) = withContext(Dispatchers.IO) {

        drawToBitmap(
            outBitmap = posterBitmap,
            density = StampPosterDensity,
            block = {
                drawStampPoster(
                    layers = layers,
                    isDark = isDark,
                )
            },
        )

        posterBitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            outputStream,
        )
    }
}
