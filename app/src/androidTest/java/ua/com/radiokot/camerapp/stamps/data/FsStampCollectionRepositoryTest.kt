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

package ua.com.radiokot.camerapp.stamps.data

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class FsStampCollectionRepositoryTest {

    @get:Rule
    val timeout: Timeout = Timeout.seconds(5)

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var stampDirectory: File
    private lateinit var subject: FsStampCollectionRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        stampDirectory = temporaryFolder.newFolder()
        subject = newRepository()
    }

    @Test(expected = IllegalArgumentException::class)
    fun init_nonExistentDirectory_throws() {
        val missingDirectory = File(stampDirectory, "missing")

        newRepository(missingDirectory)

        fail("The repository must not accept a missing directory")
    }

    @Test(expected = IllegalArgumentException::class)
    fun init_regularFile_throws() {
        val file = File(stampDirectory, "not-a-directory.txt")
        file.writeText("Hi there")

        newRepository(file)

        fail("The repository must not accept a regular file")
    }

    @Test
    fun addStampCollection_returnsGivenId() = runBlocking {
        val id = subject.addStampCollection(
            id = "424242",
            name = COLLECTION_NAME,
        )

        assertEquals("424242", id)
    }

    @Test
    fun addStampCollection_createsDirectoryWithDetailsFile() = runBlocking {
        val collectionId = "424242"

        subject.addStampCollection(
            id = collectionId,
            name = COLLECTION_NAME,
        )

        val collectionDirectory = File(stampDirectory, collectionId)
        assertTrue(
            "The collection directory must be created",
            collectionDirectory.isDirectory
        )
        assertTrue(
            "The collection details file must be created",
            File(collectionDirectory, FsStampCollectionRepository.DETAILS_FILE_NAME).isFile
        )
        assertEquals(
            "The directory must be the one the repository reports for the collection",
            collectionDirectory,
            subject.getStampCollectionDirectory(collectionId)
        )
    }

    @Test
    fun addStampCollection_nameIsPersisted() = runBlocking {
        subject.addStampCollection(
            id = "1",
            name = COLLECTION_NAME,
        )

        val collection = newRepository().getStampCollection("1")

        assertNotNull(collection)
        assertEquals("1", collection!!.id)
        assertEquals(COLLECTION_NAME, collection.name)
    }

    @Test
    fun addStampCollection_afterCacheInit_updatesList() = runBlocking {
        createCollectionOnDisk(id = "1", name = "First")
        assertEquals(
            "Initially there must be 1 collection",
            1,
            subject.getStampCollections().size
        )

        subject.addStampCollection(
            id = "2",
            name = "Second",
        )

        assertEquals(
            mapOf("1" to "First", "2" to "Second"),
            subject.getStampCollections().asMap()
        )
    }

    @Test
    fun addStampCollection_beforeCacheInit_isReadFromDisk() = runBlocking {
        createCollectionOnDisk(id = "1", name = "First")

        subject.addStampCollection(
            id = "2",
            name = "Second",
        )

        assertEquals(
            "The added collection must be listed exactly once",
            mapOf("1" to "First", "2" to "Second"),
            subject.getStampCollections().asMap()
        )
        assertEquals(2, subject.getStampCollections().size)
    }

    @Test
    fun getStampCollections_emptyDirectory_returnsEmptyList() = runBlocking {
        assertTrue(subject.getStampCollections().isEmpty())
    }

    @Test
    fun getStampCollections_readsAllCollections() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        createCollectionOnDisk(id = "2", name = "Tea")
        createCollectionOnDisk(id = "3", name = COLLECTION_NAME)

        val collections = subject.getStampCollections()

        assertEquals(3, collections.size)
        assertEquals(
            mapOf(
                "1" to "Pepsi",
                "2" to "Tea",
                "3" to COLLECTION_NAME,
            ),
            collections.asMap()
        )
    }

    @Test
    fun getStampCollections_doesNotReadRegularFiles() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        File(stampDirectory, "README")
            .writeText("Do not actually read me")
        File(stampDirectory, ".nomedia")
            .createNewFile()
        File(stampDirectory, "1778232389641.webp")
            .writeBytes(ByteArray(10))

        val collections = subject.getStampCollections()

        assertEquals(
            "Only 1 collection must be read",
            1,
            collections.size
        )
        assertEquals("1", collections.single().id)
    }

    @Test
    fun getStampCollections_directoryWithoutDetails_isNamedByIdAndGetsDetails() = runBlocking {
        val collectionDirectory = File(stampDirectory, "legacy")
        collectionDirectory.mkdirs()
        val detailsFile = File(collectionDirectory, FsStampCollectionRepository.DETAILS_FILE_NAME)
        assertFalse(detailsFile.exists())

        val collection = subject.getStampCollections().single()

        assertEquals("legacy", collection.id)
        assertEquals(
            "The collection without details must be named by its ID",
            "legacy",
            collection.name
        )
        assertTrue(
            "The missing details file must be created",
            detailsFile.isFile
        )
        assertEquals(
            "The created details must survive the restart",
            "legacy",
            newRepository().getStampCollection("legacy")!!.name
        )
    }

    @Test
    fun getStampCollection_knownId_returnsCollection() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        createCollectionOnDisk(id = "2", name = "Tea")

        val collection = subject.getStampCollection("2")

        assertNotNull(collection)
        assertEquals("2", collection!!.id)
        assertEquals("Tea", collection.name)
    }

    @Test
    fun getStampCollection_unknownId_returnsNull() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")

        assertNull(subject.getStampCollection("not-found-id"))
    }

    @Test
    fun updateStampCollection_newName_isPersistedAndCacheUpdated() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Old name")
        val collection = subject.getStampCollection("1")!!
        val newName = "New $COLLECTION_NAME"

        subject.updateStampCollection(
            collection = collection,
            newName = newName,
        )

        assertEquals(newName, subject.getStampCollection("1")!!.name)

        val collectionFromFile = newRepository().getStampCollection("1")!!
        assertEquals(newName, collectionFromFile.name)
    }

    @Test
    fun updateStampCollection_nullNewName_keepsName() = runBlocking {
        createCollectionOnDisk(id = "1", name = COLLECTION_NAME)
        val collection = subject.getStampCollection("1")!!

        subject.updateStampCollection(
            collection = collection,
            newName = null,
        )

        assertEquals(COLLECTION_NAME, subject.getStampCollection("1")!!.name)

        val collectionFromFile = newRepository().getStampCollection("1")!!
        assertEquals(COLLECTION_NAME, collectionFromFile.name)
    }

    @Test
    fun updateStampCollection_onlyAffectsGivenCollection() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        createCollectionOnDisk(id = "2", name = "Tea")
        val collectionToUpdate = subject.getStampCollection("1")!!

        subject.updateStampCollection(
            collection = collectionToUpdate,
            newName = "Cola",
        )

        val expected = mapOf("1" to "Cola", "2" to "Tea")
        assertEquals(expected, subject.getStampCollections().asMap())
        assertEquals(expected, newRepository().getStampCollections().asMap())
    }

    @Test
    fun updateStampCollection_keepsDirectoryContent() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        val collection = subject.getStampCollection("1")!!
        val collectionDirectory = subject.getStampCollectionDirectory("1")
        val stampFile = File(collectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFile)
        val stampBytesBefore = stampFile.readBytes()

        subject.updateStampCollection(
            collection = collection,
            newName = "Cola",
        )

        assertTrue(stampFile.exists())
        assertTrue(
            "The stamp must not be changed by the collection update",
            stampBytesBefore.contentEquals(stampFile.readBytes())
        )
    }

    @Test
    fun deleteStampCollection_updatesListImmediatelyAndDeletesDirectoryEventually() =
        runBlocking {
            createCollectionOnDisk(id = "1", name = "To remove")
            createCollectionOnDisk(id = "2", name = "To keep")
            val collectionToRemove = subject.getStampCollection("1")!!
            val directoryToRemove = subject.getStampCollectionDirectory("1")
            val directoryToKeep = subject.getStampCollectionDirectory("2")

            subject.deleteStampCollection(collectionToRemove)

            assertEquals(
                mapOf("2" to "To keep"),
                subject.getStampCollections().asMap()
            )
            assertTrue(directoryToKeep.exists())

            do {
                delay(150.milliseconds)
            } while (directoryToRemove.exists())

            assertTrue(directoryToKeep.exists())
            assertEquals(
                mapOf("2" to "To keep"),
                newRepository().getStampCollections().asMap()
            )
        }

    @Test
    fun deleteStampCollection_deletesStampsInside() = runBlocking {
        createCollectionOnDisk(id = "1", name = "With stamps")
        val collectionDirectory = subject.getStampCollectionDirectory("1")
        val stampFile1 = File(collectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFile1)
        val stampFile2 = File(collectionDirectory, "1778247890443.webp")
        getGiftStamp("1778247890443.webp").copyTo(stampFile2)
        val collection = subject.getStampCollection("1")!!

        subject.deleteStampCollection(collection)

        do {
            delay(150.milliseconds)
        } while (collectionDirectory.exists())

        assertFalse(stampFile1.exists())
        assertFalse(stampFile2.exists())
    }

    @Test
    fun deleteStampCollection_beforeCacheInit_deletesDirectory() = runBlocking {
        createCollectionOnDisk(id = "1", name = "To remove")
        val directory = subject.getStampCollectionDirectory("1")

        subject.deleteStampCollection(StampCollection(id = "1", name = "To remove"))

        do {
            delay(150.milliseconds)
        } while (directory.exists())

        assertTrue(newRepository().getStampCollections().isEmpty())
    }

    @Test
    fun deleteStampCollection_missingDirectory_succeedsChangingNothing() = runBlocking {
        createCollectionOnDisk(id = "1", name = "To keep")
        val collectionsToKeep = subject.getStampCollections()

        subject.deleteStampCollection(StampCollection(id = "not-found-id", name = "Ghost"))

        assertEquals(
            collectionsToKeep.asMap(),
            subject.getStampCollections().asMap()
        )
        assertTrue(subject.getStampCollectionDirectory("1").exists())
    }

    @Test
    fun getStampCollectionsFlow_emitsCurrentCollectionsFirst() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        createCollectionOnDisk(id = "2", name = "Tea")

        val first = subject.getStampCollectionsFlow().first()

        assertEquals(
            mapOf("1" to "Pepsi", "2" to "Tea"),
            first.asMap()
        )
    }

    @Test
    fun getStampCollectionsFlow_newCollector_receivesLatestState() = runBlocking {
        createCollectionOnDisk(id = "1", name = "Pepsi")
        subject.getStampCollections()
        subject.addStampCollection(
            id = "2",
            name = "Tea",
        )

        val first = subject.getStampCollectionsFlow().first()

        assertEquals(
            mapOf("1" to "Pepsi", "2" to "Tea"),
            first.asMap()
        )
    }

    @Test
    fun getStampCollectionsFlow_onCollectionAddUpdateOrDelete_emits() = runBlocking {
        createCollectionOnDisk(id = "1", name = "First")

        val emissions = mutableListOf<Map<String, String>>()

        coroutineScope {
            launch {
                subject
                    .getStampCollectionsFlow()
                    .take(4)
                    .collect { emissions += it.asMap() }
            }

            launch {
                delay(500.milliseconds)
                subject.addStampCollection(
                    id = "2",
                    name = "Second",
                )

                delay(500.milliseconds)
                subject.updateStampCollection(
                    collection = subject.getStampCollection("2")!!,
                    newName = "Renamed",
                )

                delay(500.milliseconds)
                subject.deleteStampCollection(
                    collection = subject.getStampCollection("1")!!,
                )
            }
        }

        assertEquals(
            "Must emit the initial collection",
            mapOf("1" to "First"),
            emissions[0]
        )
        assertEquals(
            "Must emit a list with the newly added collection",
            mapOf("1" to "First", "2" to "Second"),
            emissions[1]
        )
        assertEquals(
            "Must emit a list with the renamed collection",
            mapOf("1" to "First", "2" to "Renamed"),
            emissions[2]
        )
        assertEquals(
            "Must emit a list with the remaining collection",
            mapOf("2" to "Renamed"),
            emissions[3]
        )
    }

    @Test
    fun getStampCollectionDirectory_returnsDirectoryNamedByIdInsideStampDirectory() {
        assertEquals(
            File(stampDirectory, "42"),
            subject.getStampCollectionDirectory("42")
        )
    }

    private fun List<StampCollection>.asMap(): Map<String, String> =
        associate { it.id to it.name }

    private fun newRepository(
        directory: File = stampDirectory,
    ) = FsStampCollectionRepository(
        stampDirectory = directory,
        safFileLocksmith = SafFileLocksmith(
            stampDirectory = directory,
            stampDirectoryDocumentUri = Uri.parse("primary:Pictures/PressCutStampsTest"),
            contentResolver = context.contentResolver,
        ),
    )

    private fun getGiftStamp(fileName: String): File {
        val assetManager = context.assets
        val inputStream = assetManager.open("gift_stamps/$fileName")
        val tempFile = temporaryFolder.newFile(fileName)
        inputStream.use { input ->
            tempFile.outputStream().use(input::copyTo)
        }
        return tempFile
    }

    private suspend fun createCollectionOnDisk(
        id: String,
        name: String,
    ) {
        newRepository().addStampCollection(
            id = id,
            name = name,
        )
    }

    companion object {
        const val COLLECTION_NAME = "Собираем марки 🎟️"
    }
}
