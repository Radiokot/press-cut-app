package ua.com.radiokot.camerapp.intro.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ua.com.radiokot.camerapp.intro.ui.PermissionsScreenViewModel.Event
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class PermissionsScreenViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var application: Application
    private lateinit var stampDirectory: File

    private val stampDirectoryDocumentUri: Uri =
        Uri.parse("primary:Pictures/PressCut")

    private val camera = Manifest.permission.CAMERA
    private val readStorage = Manifest.permission.READ_EXTERNAL_STORAGE
    private val writeStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val readMediaImages = Manifest.permission.READ_MEDIA_IMAGES

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        stampDirectory = File(tempFolder.root, "stamps")

        shadowOf(application).denyPermissions(
            camera,
            readStorage,
            writeStorage,
            readMediaImages,
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.Q])
    fun on_pre_sdk_30_legacy_storage_permissions_are_requested() {
        val viewModel = createViewModel(isCameraRequiredNow = false)

        assertEquals(
            setOf(camera, readStorage, writeStorage),
            viewModel.requestedPermissions,
        )
        assertTrue(viewModel.isActionRequired)
        assertFalse(
            viewModel.isDocumentTreeAccessRequired,
            "Document tree access must not be required on Q and below",
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R, Build.VERSION_CODES.S_V2])
    fun on_pre_sdk_33_document_tree_is_requested_instead_of_legacy_storage_permissions() {
        val viewModel = createViewModel(isCameraRequiredNow = false)

        assertEquals(setOf(camera), viewModel.requestedPermissions)
        assertTrue(viewModel.isDocumentTreeAccessRequired)
        // Camera is optional, but the document tree is required.
        assertTrue(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun on_sdk_33_and_newer_document_tree_and_read_media_images_are_requested() {
        val viewModel = createViewModel(isCameraRequiredNow = false)

        assertEquals(
            setOf(camera, readMediaImages),
            viewModel.requestedPermissions,
        )
        assertTrue(viewModel.isDocumentTreeAccessRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun already_granted_permissions_are_not_in_requested_permissions() {
        shadowOf(application).grantPermissions(camera, readStorage)

        val viewModel = createViewModel(isCameraRequiredNow = true)

        assertEquals(setOf(writeStorage), viewModel.requestedPermissions)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun no_action_required_if_camera_is_denied_but_not_required_now() {
        shadowOf(application).grantPermissions(readStorage, writeStorage)

        val viewModel = createViewModel(isCameraRequiredNow = false)

        assertEquals(setOf(camera), viewModel.requestedPermissions)
        assertFalse(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun action_required_if_camera_is_denied_but_required_now() {
        shadowOf(application).grantPermissions(readStorage, writeStorage)

        val viewModel = createViewModel(isCameraRequiredNow = true)

        assertTrue(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun action_required_when_storage_permissions_granted_partially() {
        // Only READ, WRITE is missing.
        shadowOf(application).grantPermissions(camera, readStorage)

        val viewModel = createViewModel(isCameraRequiredNow = true)

        assertTrue(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun no_action_required_if_everything_is_granted() {
        shadowOf(application).grantPermissions(camera, readMediaImages)
        val viewModel = createViewModel(isCameraRequiredNow = true)
        grantDocumentTreeAccess(viewModel.requiredDocumentTreeAccessUri)

        assertTrue(viewModel.requestedPermissions.isEmpty())
        assertFalse(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun no_document_tree_access_required_if_already_granted() {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        assertTrue(viewModel.isDocumentTreeAccessRequired)

        grantDocumentTreeAccess(viewModel.requiredDocumentTreeAccessUri)

        assertFalse(viewModel.isDocumentTreeAccessRequired)
        assertFalse(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun document_tree_access_is_required_if_granted_for_different_directory() {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val anotherUri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:DCIM",
        )
        grantDocumentTreeAccess(anotherUri)

        assertTrue(viewModel.isDocumentTreeAccessRequired)
        assertTrue(viewModel.isActionRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun on_sdk_28_optional_and_required_permissions_are_requested() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)

        viewModel.onGrantAction()

        val event = assertIs<Event.RequestPermissions>(events.single())
        assertEquals(setOf(camera, readStorage, writeStorage), event.toRequest)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun on_sdk_33_optional_and_required_permissions_are_requested() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = true)
        val events = collectEvents(viewModel)

        viewModel.onGrantAction()

        val event = assertIs<Event.RequestPermissions>(events.single())
        assertEquals(setOf(camera, readMediaImages), event.toRequest)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun already_granted_permissions_are_still_in_permission_request() = runTest {
        shadowOf(application).grantPermissions(camera)
        val viewModel = createViewModel(isCameraRequiredNow = true)
        val events = collectEvents(viewModel)

        viewModel.onGrantAction()

        val event = assertIs<Event.RequestPermissions>(events.single())
        assertEquals(setOf(camera), event.toRequest)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun when_storage_permissions_granted_partially_explanation_is_shown_and_no_directory_created() =
        runTest {
            val viewModel = createViewModel(isCameraRequiredNow = false)
            val events = collectEvents(viewModel)

            viewModel.onPermissionRequestResult(
                mapOf(
                    camera to true,
                    readStorage to true,
                    writeStorage to false,
                )
            )

            assertIs<Event.ShowRequiredPermissionsExplanation>(events.single())
            assertFalse(stampDirectory.exists())
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun when_all_permissions_denied_explanation_is_shown_and_no_directory_created() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = true)
        val events = collectEvents(viewModel)

        viewModel.onPermissionRequestResult(
            mapOf(
                camera to false,
                readStorage to false,
                writeStorage to false,
            )
        )

        assertIs<Event.ShowRequiredPermissionsExplanation>(events.single())
        assertFalse(stampDirectory.exists())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun when_camera_denied_but_required_now_explanation_is_shown() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = true)
        val events = collectEvents(viewModel)

        viewModel.onPermissionRequestResult(
            mapOf(
                camera to false,
                readStorage to true,
                writeStorage to true,
            )
        )

        assertIs<Event.ShowRequiredPermissionsExplanation>(events.single())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun when_camera_denied_but_not_required_now_it_is_ok() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)

        viewModel.onPermissionRequestResult(
            mapOf(
                camera to false,
                readStorage to true,
                writeStorage to true,
            )
        )

        assertIs<Event.Done>(events.single())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.Q])
    fun when_all_permissions_granted_and_no_document_tree_access_required_directory_is_created_and_we_are_done() =
        runTest {
            val viewModel = createViewModel(isCameraRequiredNow = true)
            val events = collectEvents(viewModel)
            assertFalse(stampDirectory.exists())
            assertFalse(viewModel.isDocumentTreeAccessRequired)

            viewModel.onPermissionRequestResult(
                mapOf(
                    camera to true,
                    readStorage to true,
                    writeStorage to true,
                )
            )

            assertTrue(stampDirectory.isDirectory)
            assertIs<Event.Done>(events.single())
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R, Build.VERSION_CODES.TIRAMISU])
    fun when_all_permissions_granted_and_document_tree_access_required_directory_is_created_and_access_is_requested() =
        runTest {
            val viewModel = createViewModel(isCameraRequiredNow = true)
            val events = collectEvents(viewModel)
            assertTrue(viewModel.isDocumentTreeAccessRequired)

            viewModel.onPermissionRequestResult(
                mapOf(
                    camera to true,
                    readMediaImages to true,
                )
            )

            val event = assertIs<Event.RequestDocumentTreeAccess>(events.single())
            // The initial URI must be a document URI, not a tree one.
            assertEquals(
                DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    stampDirectoryDocumentUri.toString(),
                ),
                event.initialUri,
            )
            assertTrue(stampDirectory.isDirectory)
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun when_all_permissions_and_document_tree_access_granted_we_are_done() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = true)
        grantDocumentTreeAccess(viewModel.requiredDocumentTreeAccessUri)
        val events = collectEvents(viewModel)

        viewModel.onPermissionRequestResult(
            mapOf(
                camera to true,
                readMediaImages to true,
            )
        )

        assertIs<Event.Done>(events.single())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun existing_stamp_directory_is_not_recreated() = runTest {
        stampDirectory.mkdirs()
        val marker = File(stampDirectory, "marker.txt").apply { writeText("keep") }
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)

        viewModel.onPermissionRequestResult(
            mapOf(
                camera to true,
                readStorage to true,
                writeStorage to true,
            )
        )

        assertTrue(marker.exists())
        assertIs<Event.Done>(events.single())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun if_stamp_directory_can_not_be_created_an_exception_is_thrown() = runTest {
        // A regular file in place of the parent makes mkdirs() fail.
        val blocker = tempFolder.newFile("blocker")
        stampDirectory = File(blocker, "stamps")
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)

        assertFailsWith<IllegalStateException> {
            viewModel.onPermissionRequestResult(
                mapOf(
                    camera to true,
                    readStorage to true,
                    writeStorage to true,
                )
            )
        }

        assertTrue(events.isEmpty())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun if_document_tree_access_denied_an_explanation_is_shown_and_it_remains_required() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)

        viewModel.onDocumentTreeAccessRequestResult(null)

        assertIs<Event.ShowRequiredDocumentTreeAccessExplanation>(events.single())
        assertTrue(viewModel.isDocumentTreeAccessRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun if_document_tree_access_granted_for_another_directory_an_explanation_is_shown_and_it_remains_required() =
        runTest {
            val viewModel = createViewModel(isCameraRequiredNow = false)
            val events = collectEvents(viewModel)
            val wrongUri = DocumentsContract.buildTreeDocumentUri(
                "com.android.externalstorage.documents",
                "primary:DCIM",
            )

            viewModel.onDocumentTreeAccessRequestResult(wrongUri)

            assertIs<Event.ShowDocumentTreeAccessRequiredForExactDirectoryExplanation>(
                events.single()
            )
            assertTrue(viewModel.isDocumentTreeAccessRequired)
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun if_document_tree_access_granted_for_parent_directory_it_is_still_rejected() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)
        val parentUri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Pictures",
        )

        viewModel.onDocumentTreeAccessRequestResult(parentUri)

        assertIs<Event.ShowDocumentTreeAccessRequiredForExactDirectoryExplanation>(
            events.single()
        )
        assertTrue(viewModel.isDocumentTreeAccessRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun if_document_tree_access_granted_it_is_no_longer_required_and_we_are_done() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = false)
        val events = collectEvents(viewModel)

        viewModel.onDocumentTreeAccessRequestResult(viewModel.requiredDocumentTreeAccessUri)

        assertIs<Event.Done>(events.single())
        assertFalse(viewModel.isDocumentTreeAccessRequired)
        assertFalse(createViewModel(isCameraRequiredNow = false).isDocumentTreeAccessRequired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun it_is_possible_to_deny_at_first_and_grant_later() = runTest {
        val viewModel = createViewModel(isCameraRequiredNow = true)
        val events = collectEvents(viewModel)

        viewModel.onGrantAction()
        assertTrue(events.last() is Event.RequestPermissions)

        viewModel.onPermissionRequestResult(
            mapOf(camera to true, readMediaImages to false)
        )
        assertTrue(events.last() is Event.ShowRequiredPermissionsExplanation)

        viewModel.onGrantAction()
        assertTrue(events.last() is Event.RequestPermissions)

        viewModel.onPermissionRequestResult(
            mapOf(camera to true, readMediaImages to true)
        )
        assertTrue(events.last() is Event.RequestDocumentTreeAccess)

        viewModel.onDocumentTreeAccessRequestResult(null)
        assertTrue(events.last() is Event.ShowRequiredDocumentTreeAccessExplanation)

        viewModel.onDocumentTreeAccessRequestResult(viewModel.requiredDocumentTreeAccessUri)
        assertTrue(events.last() is Event.Done)
    }

    private fun createViewModel(isCameraRequiredNow: Boolean) =
        PermissionsScreenViewModel(
            stampDirectory = stampDirectory,
            stampDirectoryDocumentUri = stampDirectoryDocumentUri,
            application = application,
            parameters = PermissionsScreenViewModel.Parameters(
                isCameraRequiredNow = isCameraRequiredNow,
            ),
        )

    private fun grantDocumentTreeAccess(uri: Uri) {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    private fun TestScope.collectEvents(
        viewModel: PermissionsScreenViewModel,
    ): List<Event> {
        val collected = mutableListOf<Event>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect(collected::add)
        }
        return collected
    }
}
