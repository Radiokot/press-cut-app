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
import java.io.File

@Immutable
class PermissionsScreenViewModel(
    private val stampDirectory: File,
    private val stampDirectoryDocumentUri: Uri,
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

    val requiredDocumentTreeAccessUri: Uri =
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
                            permission.uri == requiredDocumentTreeAccessUri
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
        if (!stampDirectory.exists()) {
            check(stampDirectory.mkdirs()) {
                "Can't create the stamp directory"
            }
            log.debug {
                "onRequestedPermissionsGranted(): stamp directory created"
            }
        }

        if (isDocumentTreeAccessRequired) {
            // Using document URI as initial instead of tree
            // is important for compatibility.
            val initialUri =
                DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    stampDirectoryDocumentUri.toString()
                )

            log.debug {
                "onAllPermissionsGranted(): requesting document tree access:" +
                        "\ninitialUri=$initialUri"
            }

            events.tryEmit(
                Event.RequestDocumentTreeAccess(
                    initialUri = initialUri,
                )
            )
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
            requiredDocumentTreeAccessUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )

        events.tryEmit(Event.Done)
    }

    sealed interface Event {
        object RequestRequiredPermissions : Event

        class RequestDocumentTreeAccess(
            val initialUri: Uri,
        ) : Event

        object Done : Event
    }
}
