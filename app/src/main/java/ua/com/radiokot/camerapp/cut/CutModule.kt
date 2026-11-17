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

package ua.com.radiokot.camerapp.cut

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ua.com.radiokot.camerapp.cut.ui.NewStampViewModel
import ua.com.radiokot.camerapp.cut.ui.StampCutScreenViewModel
import ua.com.radiokot.camerapp.cut.ui.StampSaveScreenViewModel
import java.io.File

val cutModule = module {

    viewModel {
        NewStampViewModel(
            contentResolver = androidApplication().contentResolver,
            receivedImageCacheDirectory =
                File(androidApplication().cacheDir, "received"),
            imageFileDecoder = get(),
        )
    }

    viewModel {
        StampCutScreenViewModel(
            sharedViewModel = get(),
        )
    }

    viewModel {
        StampSaveScreenViewModel(
            stampRepository = get(),
            imageAdjustmentsControllerViewModel = get(),
            sharedViewModel = get(),
        )
    }
}
