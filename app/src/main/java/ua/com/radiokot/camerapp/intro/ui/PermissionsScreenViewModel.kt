package ua.com.radiokot.camerapp.intro.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharedFlow
import ua.com.radiokot.camerapp.util.eventSharedFlow
import ua.com.radiokot.camerapp.util.lazyLogger

@Immutable
class PermissionsScreenViewModel : ViewModel() {

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
            .also {
                log.debug {
                    "init(): required permissions collected:" +
                            "\nrequiredPermissions=$it"
                }
            }

    fun areAllPermissionsGranted(
        context: Context,
    ): Boolean =
        requiredPermissions.all { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

    fun onAllPermissionsGranted() {
        log.debug {
            "onAllPermissionsGranted(): all permissions are granted, emitting Done"
        }

        events.tryEmit(Event.Done)
    }

    sealed interface Event {
        object Done : Event
    }
}
