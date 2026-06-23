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

package ua.com.radiokot.camerapp.stamps

import android.os.Environment
import androidx.core.net.toUri
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.stamps.data.CpCreateSendStampIntentUseCase
import ua.com.radiokot.camerapp.stamps.data.FsAddGiftStampsToPrimaryCollectionUseCase
import ua.com.radiokot.camerapp.stamps.data.FsStampCollectionRepository
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository
import ua.com.radiokot.camerapp.stamps.data.SafFileLocksmith
import ua.com.radiokot.camerapp.stamps.domain.AddGiftStampsToPrimaryCollectionUseCase
import ua.com.radiokot.camerapp.stamps.domain.CreateSendStampIntentUseCase
import ua.com.radiokot.camerapp.stamps.domain.EnsurePrimaryStampCollectionUseCase
import ua.com.radiokot.camerapp.stamps.domain.GetSortedStampCollectionsUseCase
import ua.com.radiokot.camerapp.stamps.domain.GetStampCollectionsWithSamplesUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.ui.CollectionActionsScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.CollectionsScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.MoveStampsScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.StampScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.StampsScreenViewModel
import java.io.File

const val DIRECTORY_STAMPS = "stamps-dir"

val stampsModule = module {

    val stampDirectoryName = "PressCutStamps"

    single(named(DIRECTORY_STAMPS)) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            stampDirectoryName
        ).also { dir ->
            if (!dir.exists()) {
                // This is expected to fail until the permissions are given.
                // The directory is created in a VM at that point.
                dir.mkdirs()
            }
        }
    }

    single(named(DIRECTORY_STAMPS)) {
        "primary:${Environment.DIRECTORY_PICTURES}/$stampDirectoryName".toUri()
    }

    single {
        SafFileLocksmith(
            stampDirectory = get(named(DIRECTORY_STAMPS)),
            stampDirectoryDocumentUri = get(named(DIRECTORY_STAMPS)),
            contentResolver = androidApplication().contentResolver,
        )
    }

    single {
        FsStampRepository(
            stampDirectory = get(named(DIRECTORY_STAMPS)),
            safFileLocksmith = get(),
        )
    } bind StampRepository::class

    single {
        FsStampCollectionRepository(
            stampDirectory = get(named(DIRECTORY_STAMPS)),
            safFileLocksmith = get(),
        )
    } bind StampCollectionRepository::class

    single {
        GetSortedStampCollectionsUseCase(
            collectionRepository = get(),
            ensurePrimaryStampCollectionUseCase = get(),
        )
    }
    single {
        GetStampCollectionsWithSamplesUseCase(
            stampRepository = get(),
            getSortedStampCollectionsUseCase = get(),
        )
    }

    single {
        FsAddGiftStampsToPrimaryCollectionUseCase(
            stampRepository = get(),
            assetManager = androidApplication().assets,
            giftStampsAssetsDirectoryName = "gift_stamps",
            onboardingPreferences = get(),
        )
    } bind AddGiftStampsToPrimaryCollectionUseCase::class

    single {
        EnsurePrimaryStampCollectionUseCase(
            collectionRepository = get(),
            addGiftStampsToPrimaryCollectionUseCase = get(),
        )
    }

    single {
        CpCreateSendStampIntentUseCase()
    } bind CreateSendStampIntentUseCase::class

    viewModel {
        StampsScreenViewModel(
            stampRepository = get(),
            collectionRepository = get(),
            onboardingPreferences = get(),
            parameters =
                getOrNull()
                    ?: error("No StampsScreenViewModel.Parameters provided"),
        )
    }

    viewModel {
        StampScreenViewModel(
            stampRepository = get(),
            createSendStampIntentUseCase = get(),
            createSendStampPosterIntentUseCase = get(),
            parameters =
                getOrNull()
                    ?: error("No StampScreenViewModel.Parameters provided"),
        )
    }

    viewModel {
        CollectionsScreenViewModel(
            collectionRepository = get(),
            getStampCollectionsWithSamplesUseCase = get(),
        )
    }

    viewModel {
        CollectionActionsScreenViewModel(
            collectionRepository = get(),
            getStampCollectionsWithSamplesUseCase = get(),
            parameters =
                getOrNull()
                    ?: error("No CollectionActionsScreenViewModel.Parameters provided"),
        )
    }

    viewModel {
        MoveStampsScreenViewModel(
            stampRepository = get(),
            parameters =
                getOrNull()
                    ?: error("No MoveStampsScreenViewModel.Parameters provided"),
        )
    }
}
