package ua.com.radiokot.camerapp.intro.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharedFlow
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class PermissionsScreenViewModel(
    stampDirectoryDocumentUri: Uri,
    private val application: Application,
) : AndroidViewModel(application) {

    private val log by lazyLogger("PermissionsScreenVM")

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    val requiredPermissions: ImmutableList<String> =
        buildList {
            add(Manifest.permission.CAMERA)

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
            .toPersistentList()

    val documentTreeAccessUri: Uri =
        DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            stampDirectoryDocumentUri.toString()
        )

    val isDocumentTreeAccessRequired: Boolean
        get() =
            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q &&
                    application
                        .contentResolver
                        .persistedUriPermissions
                        .none { permission ->
                            permission.uri == documentTreeAccessUri
                        }

    val isActionRequired: Boolean
        get() =
            requiredPermissions
                .any { application.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                    || isDocumentTreeAccessRequired

    fun onGrantAction() {
        log.debug {
            "onGrantAction(): emitting RequestRequiredPermissions"
        }

        events.tryEmit(Event.RequestRequiredPermissions)
    }

    fun onRequestedPermissionsGranted() {
        if (isDocumentTreeAccessRequired) {
            log.debug {
                "onAllPermissionsGranted(): requesting document tree access:" +
                        "\nuri=$documentTreeAccessUri"
            }

            events.tryEmit(Event.RequestDocumentTreeAccess)
        } else {
            log.debug {
                "onAllPermissionsGranted(): all permissions are granted, emitting Done"
            }

            events.tryEmit(Event.Done)
        }
    }

    fun onDocumentTreeAccessGranted() {
        log.debug {
            "onDocumentTreeAccessGranted(): document tree access granted, emitting Done"
        }

        application.contentResolver.takePersistableUriPermission(
            documentTreeAccessUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )

        events.tryEmit(Event.Done)
    }

    sealed interface Event {
        object RequestRequiredPermissions : Event

        object RequestDocumentTreeAccess : Event

        object Done : Event
    }
}
