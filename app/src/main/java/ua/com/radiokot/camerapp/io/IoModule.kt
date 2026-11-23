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

package ua.com.radiokot.camerapp.io

import android.os.Build
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.LandscapistConfig
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.util.BitmapFactoryImageFileDecoder
import ua.com.radiokot.camerapp.util.FileUriDecodingImageFetcher
import ua.com.radiokot.camerapp.util.ImageDecoderImageFileDecoder
import ua.com.radiokot.camerapp.util.ImageFileDecoder
import ua.com.radiokot.camerapp.util.NoOpImageDecoder

val ioModule = module {

    single<ImageFileDecoder> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoderImageFileDecoder()
        } else {
            BitmapFactoryImageFileDecoder()
        }
    } bind ImageFileDecoder::class

    single<Landscapist> {
        val config = LandscapistConfig(
            diskCacheSize = 0L,
            memoryCacheSize = 1073741824, // 1 GB (Hardware bitmaps)
        )

        Landscapist.Builder()
            .config(config)
            .dispatcher(Dispatchers.Default)
            .fetcher(
                FileUriDecodingImageFetcher(
                    decoder = get(),
                )
            )
            .decoder(NoOpImageDecoder())
            .build()
    } bind Landscapist::class
}
