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
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.SharedFlow
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File

@Immutable
class PermissionsScreenViewModel(
    private val stampDirectory: File,
    private val stampDirectoryDocumentUri: Uri,
    private val application: Application,
    parameters: Parameters,
) : AndroidViewModel(application) {

    private val log by lazyLogger("PermissionsScreenVM")

    val events: SharedFlow<Event>
        field = eventSharedFlow()

    private val optionalPermissions: Set<String> = buildSet {
        if (!parameters.isCameraRequiredNow) {
            add(Manifest.permission.CAMERA)
        }
    }

    private val requiredPermissions: Set<String> = buildSet {
        if (parameters.isCameraRequiredNow) {
            add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    val requestedPermissions: PersistentSet<String> =
        (optionalPermissions + requiredPermissions)
            .filterNot(::isPermissionGranted)
            .toPersistentSet()

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
        get() = !requiredPermissions.all(::isPermissionGranted)
                || isDocumentTreeAccessRequired

    fun onGrantAction() {
        log.debug {
            "onGrantAction(): emitting RequestPermissions"
        }

        events.tryEmit(
            Event.RequestPermissions(
                toRequest = (optionalPermissions + requiredPermissions).toPersistentSet()
            )
        )
    }

    fun onPermissionRequestResult(
        result: Map<String, Boolean>,
    ) {
        val grantedPermissions =
            result
                .filter(Map.Entry<String, Boolean>::value)
                .map(Map.Entry<String, Boolean>::key)
                .toSet()

        if (!grantedPermissions.containsAll(requiredPermissions)) {
            log.debug {
                "onPermissionRequestResult(): not all required permissions granted, " +
                        "emitting ShowRequiredPermissionsExplanation"
            }

            events.tryEmit(Event.ShowRequiredPermissionsExplanation)

            return
        }

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

    fun onDocumentTreeAccessRequestResult(
        result: Uri?,
    ) {
        if (result == null) {
            log.debug {
                "onDocumentTreeAccessRequestResult(): document tree access denied, " +
                        "emitting ShowRequiredDocumentTreeAccessExplanation"
            }

            events.tryEmit(Event.ShowRequiredDocumentTreeAccessExplanation)

            return
        }

        if (result != requiredDocumentTreeAccessUri) {
            log.debug {
                "onDocumentTreeAccessRequestResult(): document tree access granted for " +
                        "the wrong directory, emitting ShowDocumentTreeAccessRequiredForExactDirectoryExplanation"
            }

            events.tryEmit(Event.ShowDocumentTreeAccessRequiredForExactDirectoryExplanation)

            return
        }

        log.debug {
            "onDocumentTreeAccessRequestResult(): document tree access granted, emitting Done"
        }

        application.contentResolver.takePersistableUriPermission(
            requiredDocumentTreeAccessUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )

        events.tryEmit(Event.Done)
    }

    private fun isPermissionGranted(permission: String): Boolean =
        application.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    sealed interface Event {
        class RequestPermissions(
            val toRequest: ImmutableSet<String>,
        ) : Event

        class RequestDocumentTreeAccess(
            val initialUri: Uri,
        ) : Event

        object ShowRequiredPermissionsExplanation : Event

        object ShowRequiredDocumentTreeAccessExplanation : Event

        object ShowDocumentTreeAccessRequiredForExactDirectoryExplanation : Event

        object Done : Event
    }

    data class Parameters(
        val isCameraRequiredNow: Boolean,
    )
}
