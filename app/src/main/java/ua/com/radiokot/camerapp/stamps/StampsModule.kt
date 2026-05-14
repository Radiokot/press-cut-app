package ua.com.radiokot.camerapp.stamps

import android.os.Environment
import androidx.core.net.toUri
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.camerapp.stamps.data.FsStampCollectionRepository
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository
import ua.com.radiokot.camerapp.stamps.data.SafFileLocksmith
import ua.com.radiokot.camerapp.stamps.domain.EnsurePrimaryStampCollectionUseCase
import ua.com.radiokot.camerapp.stamps.domain.GetSortedStampCollectionsUseCase
import ua.com.radiokot.camerapp.stamps.domain.GetStampCollectionsWithSamplesUseCase
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.ui.CollectionActionsScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.CollectionsScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.MoveStampsScreenViewModel
import ua.com.radiokot.camerapp.stamps.ui.SelectMoveDestinationCollectionDialogViewModel
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
                check(dir.mkdirs()) {
                    "Can't create the stamps directory"
                }
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
            assetManager = androidApplication().assets,
            giftStampsAssetsDirectoryName = "gift_stamps",
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
        EnsurePrimaryStampCollectionUseCase(
            collectionRepository = get(),
            stampRepository = get(),
            onboardingPreferences = get(),
        )
    }

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
        SelectMoveDestinationCollectionDialogViewModel(
            collectionRepository = get(),
            getSortedStampCollectionsUseCase = get(),
            parameters =
                getOrNull()
                    ?: error("No MoveToCollectionDialogViewModel.Parameters provided"),
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
