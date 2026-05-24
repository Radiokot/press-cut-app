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

package ua.com.radiokot.camerapp.envelopes

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.envelopes.data.FsAddStampsFromOneStampEnvelopeUseCase
import ua.com.radiokot.camerapp.envelopes.data.FsGetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.envelopes.domain.AddStampsFromOneStampEnvelopeUseCase
import ua.com.radiokot.camerapp.envelopes.domain.GetOneStampEnvelopePreviewUseCase
import ua.com.radiokot.camerapp.envelopes.ui.EnvelopePreviewViewModel
import java.io.File

const val DIRECTORY_TEMP_STAMP_IMAGES = "temp-stamp-images-dir"

val envelopesModule = module {

    single(named(DIRECTORY_TEMP_STAMP_IMAGES)) {
        File(
            androidApplication().cacheDir,
            "temp-stamp-images",
        )
    }

    single {
        FsGetOneStampEnvelopePreviewUseCase(
            contentResolver = androidApplication().contentResolver,
            tempStampImageDirectory = get(named(DIRECTORY_TEMP_STAMP_IMAGES)),
        )
    } bind GetOneStampEnvelopePreviewUseCase::class

    single {
        FsAddStampsFromOneStampEnvelopeUseCase(
            stampRepository = get(),
            contentResolver = androidApplication().contentResolver,
        )
    } bind AddStampsFromOneStampEnvelopeUseCase::class

    viewModel {
        EnvelopePreviewViewModel(
            getOneStampEnvelopePreviewUseCase = get(),
            parameters =
                getOrNull()
                    ?: error("No EnvelopePreviewViewModel.Parameters provided"),
        )
    }
}
