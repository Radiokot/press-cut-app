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
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCornersLandscape
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class FsStampRepositoryReasonableTest {

    @get:Rule
    val timeout: Timeout = Timeout.seconds(5)

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var stampDirectory: File
    private val defaultCollectionId = "0"
    private lateinit var defaultCollectionDirectory: File
    private val sourceCollectionId = "1"
    private lateinit var sourceCollectionDirectory: File
    private val destinationCollectionId = "2"
    private lateinit var destinationCollectionDirectory: File

    private lateinit var subject: FsStampRepository

    private val giftWebpBytes: ByteArray by lazy {
        getGiftStamp("1778232389641.webp").readBytes()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        stampDirectory = temporaryFolder.newFolder()
        defaultCollectionDirectory = File(stampDirectory, defaultCollectionId)
        defaultCollectionDirectory.mkdirs()
        sourceCollectionDirectory = File(stampDirectory, sourceCollectionId)
        sourceCollectionDirectory.mkdirs()
        destinationCollectionDirectory = File(stampDirectory, destinationCollectionId)
        destinationCollectionDirectory.mkdirs()

        subject = newRepository()
    }

    @Test
    fun repository_can_only_be_created_from_a_readable_directory(

    ) = runBlocking {

        assertThrows(
            "Can't be created from a directory that doesn't exist",
            Exception::class.java
        ) {
            newRepository(File(stampDirectory, "does not exist"))
        }

        assertThrows(
            "Can't be created from a File that is not a directory",
            Exception::class.java
        ) {
            newRepository(temporaryFolder.newFile())
        }

        assertThrows(
            "Can't be created from a directory that is not readable",
            Exception::class.java
        ) {
            val notReadableDirectory = temporaryFolder.newFolder()
            notReadableDirectory.setReadable(false)
            newRepository(notReadableDirectory)
        }

        // OK for the directory to be not writable
        // (SAF file locksmith is used in this case).
        val notWritableDirectory = temporaryFolder.newFolder()
        notWritableDirectory.setWritable(false)
        newRepository(notWritableDirectory)

        // OK.
        newRepository(stampDirectory)

        Unit
    }

    @Test
    fun added_stamp_is_saved_as_a_file_in_the_collection_directory(

    ) = runBlocking {

        val collectionAId = "1"
        val collectionADirectory = File(stampDirectory, collectionAId)
        collectionADirectory.mkdirs()
        val collectionBId = "2"
        val collectionBDirectory = File(stampDirectory, collectionBId)
        collectionBDirectory.mkdirs()
        val collectionCId = "3"
        val collectionCDirectory = File(stampDirectory, collectionCId)
        collectionCDirectory.mkdirs()
        val bitmap = createBitmap(64, 48, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.MAGENTA)
        val webpBytes = getGiftStamp("1778232389641.webp").readBytes()

        subject.addStamp(
            collectionId = collectionCId,
            stampWebpName = "1778232389641.webp",
            stampWebpContent = ByteArrayInputStream(webpBytes),
        )
        val stampC = subject.getStamps().single { it.collectionId == collectionCId }
        val (stampCBytes, stampCSize) = subject.getStampImageBytesAndSize(stampC)
        val stampCFile = subject.getStampFile(stampC)
        subject = newRepository()
        val retrievedStampC = subject.getStamps().single { it.collectionId == collectionCId }
        val (retrievedStampCBytes, retrievedStampCSize) = subject.getStampImageBytesAndSize(
            retrievedStampC
        )

        subject.addStamp(
            collectionId = collectionAId,
            imageBitmap = bitmap,
            caption = null,
            shape = StampShapeA,
        )
        val stampA = subject.getStamps().single { it.collectionId == collectionAId }
        val (stampABytes, stampASize) = subject.getStampImageBytesAndSize(stampA)
        val stampAFile = subject.getStampFile(stampA)
        subject = newRepository()
        val retrievedStampA = subject.getStamps().single { it.collectionId == collectionAId }
        val (retrievedStampABytes, retrievedStampASize) = subject.getStampImageBytesAndSize(
            retrievedStampA
        )

        subject.addStamp(
            collectionId = collectionBId,
            webpBytes = webpBytes,
            caption = null,
            shape = StampShapeA,
            takenAtLocal = TAKEN_AT,
        )
        val stampB = subject.getStamps().single { it.collectionId == collectionBId }
        val (stampBBytes, stampBSize) = subject.getStampImageBytesAndSize(stampB)
        val stampBFile = subject.getStampFile(stampB)
        subject = newRepository()
        val retrievedStampB = subject.getStamps().single { it.collectionId == collectionBId }
        val (retrievedStampBBytes, retrievedStampBSize) =
            subject.getStampImageBytesAndSize(retrievedStampB)

        fun assertTestStatement(
            stamp: Stamp,
            stampFile: File,
            stampBytes: ByteArray,
            stampSize: Size,
            collectionDirectory: File,
            retrieved: Stamp,
            retrievedBytes: ByteArray,
            retrievedSize: Size,
        ) {
            assertTrue(stampFile.exists())
            assertEquals(
                "The file must be named with the stamp ID",
                stamp.id,
                stampFile.nameWithoutExtension
            )
            assertEquals(
                "The file must be a WebP image",
                "webp",
                stampFile.extension.lowercase()
            )
            assertEquals(
                "The file must be in the collection directory",
                collectionDirectory,
                stampFile.parentFile
            )
            assertEquals(stamp.id, retrieved.id)
            assertEquals(stamp.caption, retrieved.caption)
            assertEquals(stamp.collectionId, retrieved.collectionId)
            assertEquals(stamp.imageUri, retrieved.imageUri)
            assertEquals(stamp.takenAtLocal, retrieved.takenAtLocal)
            assertEquals(stamp.shape, retrieved.shape)
            assertArrayEquals(stampBytes, retrievedBytes)
            assertNotNull(BitmapFactory.decodeByteArray(retrievedBytes, 0, retrievedBytes.size))
            assertEquals(stampSize, retrievedSize)
        }

        assertTestStatement(
            stamp = stampA,
            stampFile = stampAFile,
            stampBytes = stampABytes,
            stampSize = stampASize,
            collectionDirectory = collectionADirectory,
            retrieved = retrievedStampA,
            retrievedBytes = retrievedStampABytes,
            retrievedSize = retrievedStampASize,
        )
        assertTestStatement(
            stamp = stampB,
            stampFile = stampBFile,
            stampBytes = stampBBytes,
            stampSize = stampBSize,
            collectionDirectory = collectionBDirectory,
            retrieved = retrievedStampB,
            retrievedBytes = retrievedStampBBytes,
            retrievedSize = retrievedStampBSize,
        )
        assertTestStatement(
            stamp = stampC,
            stampFile = stampCFile,
            stampBytes = stampCBytes,
            stampSize = stampCSize,
            collectionDirectory = collectionCDirectory,
            retrieved = retrievedStampC,
            retrievedBytes = retrievedStampCBytes,
            retrievedSize = retrievedStampCSize,
        )
    }

    @Test
    fun stamp_added_as_bitmap_has_adding_time_as_taken_at(

    ) = runBlocking {

        val bitmap = createBitmap(64, 48, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.MAGENTA)

        subject.addStamp(
            collectionId = defaultCollectionId,
            imageBitmap = bitmap,
            caption = CAPTION,
            shape = StampShapeOneStampWithoutCornersLandscape,
        )
        val stamp = subject.getStamps().single()

        assertEquals(CAPTION, stamp.caption)
        assertEquals(defaultCollectionId, stamp.collectionId)
        assertTrue(
            "Stamp takenAtLocal must be set to the adding time (now)",
            Duration.between(stamp.takenAtLocal, LocalDateTime.now())
                .abs() < Duration.ofSeconds(10)
        )
        assertEquals(StampShapeOneStampWithoutCornersLandscape, stamp.shape)
    }

    @Test
    fun stamp_added_as_webp_bytes_has_overwritten_details(

    ) = runBlocking {
        val webpBytes = getGiftStamp("1778232389641.webp").readBytes()

        subject.addStamp(
            collectionId = defaultCollectionId,
            webpBytes = webpBytes,
            caption = CAPTION,
            shape = StampShapeOneStampWithoutCornersLandscape,
            takenAtLocal = TAKEN_AT
        )
        val stamp = subject.getStamps().single()

        assertEquals(CAPTION, stamp.caption)
        assertEquals(defaultCollectionId, stamp.collectionId)
        assertEquals(TAKEN_AT, stamp.takenAtLocal)
        assertEquals(StampShapeOneStampWithoutCornersLandscape, stamp.shape)
    }

    @Test
    fun stamp_added_as_input_stream_has_original_details(

    ) = runBlocking {
        val giftStampFile = getGiftStamp("1778232389641.webp")

        subject.addStamp(
            collectionId = defaultCollectionId,
            stampWebpName = giftStampFile.name,
            stampWebpContent = giftStampFile.inputStream(),
        )
        val stamp = subject.getStamps().single()

        assertEquals("1778232389641", stamp.id)
        assertEquals(defaultCollectionId, stamp.collectionId)
        assertEquals("Little guy", stamp.caption)
        assertEquals(LocalDateTime.parse("2026-05-08T12:26:29.641377"), stamp.takenAtLocal)
        assertEquals(StampShapeA, stamp.shape)
    }

    @Test
    fun new_stamp_IDs_are_numeric_unique_and_incremental(

    ) = runBlocking {

        val idsFromNewStampId = Array(100) { FsStampRepository.newStampId() }
        val bitmap = createBitmap(64, 48, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.MAGENTA)
        repeat(100) {
            subject.addStamp(
                collectionId = defaultCollectionId,
                imageBitmap = bitmap,
                caption = CAPTION,
                shape = StampShapeOneStampWithoutCornersLandscape,
            )
        }
        val idsFromAddStamp = subject.getStamps().map(Stamp::id)
        val ids = idsFromNewStampId + idsFromAddStamp

        assertEquals(
            "Stamp IDs must be unique",
            ids.size,
            ids.distinct().size
        )
        assertTrue(
            "Stamp IDs must be numeric, otherwise the files are not recognized as stamps",
            ids.all { it.isNotEmpty() && it.all(Char::isDigit) }
        )
        assertEquals(
            "Stamp IDs must be incremental",
            ids.map(String::toLong).sorted(),
            ids.map(String::toLong)
        )
    }

    @Test
    fun updated_stamp_caption_is_emitted_right_after_the_update(

    ) = runBlocking {

        seedStamps(defaultCollectionId, "To update", "Bystander")
        val stampToUpdate = findStampByCaption("To update")

        withStampsFlowEmissions { emissions ->
            emissions.awaitEmission()

            subject.updateStamp(
                stamp = stampToUpdate,
                newCaption = Optional.of(NEW_CAPTION),
            )

            val emitted = emissions.awaitEmission { stamps ->
                stamps.any { it.caption == NEW_CAPTION }
            }
            assertEquals(
                "The updated stamp must be neither duplicated nor lost",
                2,
                emitted.size
            )
            assertEquals(
                stampToUpdate.id,
                emitted.single { it.caption == NEW_CAPTION }.id
            )
            assertEquals(
                "Other stamps must stay as they were",
                "Bystander",
                emitted.single { it.id != stampToUpdate.id }.caption
            )
        }
    }

    @Test
    fun updated_stamp_caption_is_persisted_with_other_details_intact(

    ) = runBlocking {

        seedStamps(defaultCollectionId, "To update")
        val stamp = findStampByCaption("To update")
        val (bytesBeforeUpdate, sizeBeforeUpdate) = subject.getStampImageBytesAndSize(stamp)

        subject.updateStamp(
            stamp = stamp,
            newCaption = Optional.of(NEW_CAPTION),
        )
        subject = newRepository()
        val retrieved = subject.getStamps().single()
        val (retrievedBytes, retrievedSize) = subject.getStampImageBytesAndSize(retrieved)

        assertEquals(NEW_CAPTION, retrieved.caption)
        assertEquals(stamp.id, retrieved.id)
        assertEquals(stamp.collectionId, retrieved.collectionId)
        assertEquals(stamp.imageUri, retrieved.imageUri)
        assertEquals(stamp.takenAtLocal, retrieved.takenAtLocal)
        assertEquals(stamp.shape, retrieved.shape)
        assertEquals(
            "The image must not be affected by the update",
            sizeBeforeUpdate,
            retrievedSize
        )
        assertTrue(
            BitmapFactory.decodeByteArray(retrievedBytes, 0, retrievedBytes.size)
                .sameAs(
                    BitmapFactory.decodeByteArray(bytesBeforeUpdate, 0, bytesBeforeUpdate.size)
                )
        )
    }

    @Test
    fun stamp_caption_can_be_removed_by_update_with_empty_optional(

    ) = runBlocking {

        seedStamps(defaultCollectionId, "To clear")
        val stamp = findStampByCaption("To clear")

        withStampsFlowEmissions { emissions ->
            emissions.awaitEmission()

            subject.updateStamp(
                stamp = stamp,
                newCaption = Optional.empty(),
            )

            val emitted = emissions.awaitEmission { stamps ->
                stamps.single().caption == null
            }
            assertEquals(stamp.id, emitted.single().id)
        }

        subject = newRepository()
        assertNull(
            "The removed caption must be persisted",
            subject.getStamps().single().caption
        )
    }

    @Test
    fun stamp_caption_is_kept_when_update_has_no_new_caption(

    ) = runBlocking {

        seedStamps(defaultCollectionId, "Keep me")
        val stamp = findStampByCaption("Keep me")

        subject.updateStamp(
            stamp = stamp,
            newCaption = null,
        )
        assertEquals("Keep me", subject.getStamps().single().caption)

        subject = newRepository()
        assertEquals("Keep me", subject.getStamps().single().caption)
    }

    @Test
    fun deleted_stamps_are_emitted_right_after_the_deletion(

    ) = runBlocking {

        seedStamps(defaultCollectionId, "Keep", "Delete 1", "Delete 2")
        seedStamps(sourceCollectionId, "Other collection")
        val stampsToDelete = listOf(
            findStampByCaption("Delete 1"),
            findStampByCaption("Delete 2"),
        )

        withStampsFlowEmissions { emissions ->
            val initial = emissions.awaitEmission()
            assertEquals(4, initial.size)

            subject.deleteStamps(
                collectionId = defaultCollectionId,
                stampIds = stampsToDelete.map(Stamp::id),
            )

            val emitted = emissions.awaitEmission { stamps -> stamps.size == 2 }
            assertEquals(
                "Only the deleted stamps must be gone",
                setOf("Keep", "Other collection"),
                emitted.map(Stamp::caption).toSet()
            )
        }
    }

    @Test
    fun deleted_stamps_are_removed_from_the_file_system_and_stay_deleted(

    ) = runBlocking {

        seedStamps(defaultCollectionId, "Keep", "Delete 1", "Delete 2")
        seedStamps(sourceCollectionId, "Other collection")
        val keptStamps =
            listOf(findStampByCaption("Keep"), findStampByCaption("Other collection"))
        val deletedStamps =
            listOf(findStampByCaption("Delete 1"), findStampByCaption("Delete 2"))
        val deletedStampFiles = deletedStamps.map { subject.getStampFile(it) }
        assertTrue(deletedStampFiles.all(File::exists))

        subject.deleteStamps(
            collectionId = defaultCollectionId,
            stampIds = deletedStamps.map(Stamp::id),
        )

        awaitCondition("The files of the deleted stamps must be removed") {
            deletedStampFiles.none(File::exists)
        }
        subject = newRepository()
        val retrieved = subject.getStamps()
        assertEquals(
            "The deleted stamps must not come back after the repository is recreated",
            setOf("Keep", "Other collection"),
            retrieved.map(Stamp::caption).toSet()
        )
        assertTrue(
            "The files of the other stamps must be untouched",
            keptStamps.all { subject.getStampFile(it).exists() }
        )
    }

    @Test
    fun moved_stamps_are_emitted_with_the_destination_collection_right_after_the_move(

    ) = runBlocking {

        seedStamps(sourceCollectionId, "Moved 1", "Moved 2", "Stays")
        seedStamps(destinationCollectionId, "Already here")
        val stampsToMove = listOf(findStampByCaption("Moved 1"), findStampByCaption("Moved 2"))

        val emitted = withStampsFlowEmissions { emissions ->
            emissions.awaitEmission()

            subject.moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = stampsToMove.map(Stamp::id),
            ).collect()

            emissions.awaitEmission { stamps ->
                stamps
                    .filter { it.caption?.startsWith("Moved") == true }
                    .all { it.collectionId == destinationCollectionId }
            }
        }

        assertEquals(4, emitted.size)
        assertEquals(
            setOf("Moved 1", "Moved 2", "Already here"),
            emitted
                .filter { it.collectionId == destinationCollectionId }
                .map(Stamp::caption)
                .toSet()
        )
        assertEquals(
            "The stamp that was not asked to move must stay in the source collection",
            listOf("Stays"),
            emitted
                .filter { it.collectionId == sourceCollectionId }
                .map(Stamp::caption)
        )
        assertEquals(
            "The emitted stamps must be the same as the ones retrieved from the disk, " +
                    "including the image URI pointing to the new location",
            newRepository().getStamps().associateBy(Stamp::id),
            emitted.associateBy(Stamp::id)
        )
    }

    @Test
    fun moved_stamps_are_persisted_in_the_destination_collection_directory(

    ) = runBlocking {

        seedStamps(sourceCollectionId, "Moved 1", "Moved 2", "Stays")
        val stampsToMove = listOf(findStampByCaption("Moved 1"), findStampByCaption("Moved 2"))
        val stayingStamp = findStampByCaption("Stays")
        val bytesBeforeMove = stampsToMove.associate {
            it.id to subject.getStampImageBytesAndSize(it).first
        }

        subject.moveStampsBetweenCollections(
            sourceCollectionId = sourceCollectionId,
            destinationCollectionId = destinationCollectionId,
            stampIds = stampsToMove.map(Stamp::id),
        ).collect()

        assertEquals(
            "The stamp files must be in the destination collection directory",
            stampsToMove.map(Stamp::id).toSet(),
            stampFilesIn(destinationCollectionDirectory).map(File::nameWithoutExtension).toSet()
        )
        assertEquals(
            "The stamp files must be gone from the source collection directory, " +
                    "except for the one that was not moved",
            listOf(stayingStamp.id),
            stampFilesIn(sourceCollectionDirectory).map(File::nameWithoutExtension)
        )

        subject = newRepository()
        val retrieved = subject.getStamps().associateBy(Stamp::id)
        stampsToMove.forEach { stampBeforeMove ->
            val retrievedStamp = retrieved.getValue(stampBeforeMove.id)

            assertEquals(destinationCollectionId, retrievedStamp.collectionId)
            assertEquals(stampBeforeMove.caption, retrievedStamp.caption)
            assertEquals(stampBeforeMove.takenAtLocal, retrievedStamp.takenAtLocal)
            assertEquals(stampBeforeMove.shape, retrievedStamp.shape)
            assertArrayEquals(
                "The stamp content must not change when moving",
                bytesBeforeMove.getValue(stampBeforeMove.id),
                subject.getStampImageBytesAndSize(retrievedStamp).first
            )
        }
        assertEquals(sourceCollectionId, retrieved.getValue(stayingStamp.id).collectionId)
    }

    @Test
    fun moving_stamps_emits_progress_from_zero_to_the_total_count(

    ) = runBlocking {

        seedStamps(sourceCollectionId, "Moved 1", "Moved 2", "Moved 3", "Stays")
        val stampsToMove = listOf("Moved 1", "Moved 2", "Moved 3").map { findStampByCaption(it) }

        val progress = subject.moveStampsBetweenCollections(
            sourceCollectionId = sourceCollectionId,
            destinationCollectionId = destinationCollectionId,
            stampIds = stampsToMove.map(Stamp::id),
        )
            .onEach { (moved, _) ->
                assertTrue(
                    "The progress must not outrun the stamps that were actually moved",
                    stampFilesIn(destinationCollectionDirectory).size >= moved
                )
            }
            .toList()

        assertEquals(
            "Progress must start with nothing moved, grow one stamp at a time " +
                    "and finish with everything moved",
            listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3),
            progress
        )
    }

    @Test
    fun moving_the_whole_collection_moves_every_stamp_and_emits_progress(

    ) = runBlocking {

        seedStamps(sourceCollectionId, "First", "Second", "Third")
        seedStamps(destinationCollectionId, "Already here")

        val progress = subject.moveStampsBetweenCollections(
            sourceCollectionId = sourceCollectionId,
            destinationCollectionId = destinationCollectionId,
        ).toList()

        assertEquals(
            listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3),
            progress
        )
        assertTrue(
            "No stamps must be left in the source collection directory",
            stampFilesIn(sourceCollectionDirectory).isEmpty()
        )

        subject = newRepository()
        val retrieved = subject.getStamps()
        assertTrue(
            "Source collection must be empty after the move",
            retrieved.none { it.collectionId == sourceCollectionId }
        )
        assertEquals(
            setOf("First", "Second", "Third", "Already here"),
            retrieved
                .filter { it.collectionId == destinationCollectionId }
                .map(Stamp::caption)
                .toSet()
        )
    }

    @Test
    fun moving_the_whole_collection_is_emitted_right_after_the_move(

    ) = runBlocking {

        seedStamps(sourceCollectionId, "First", "Second")
        seedStamps(defaultCollectionId, "Bystander")
        findStampByCaption("Bystander")

        withStampsFlowEmissions { emissions ->
            emissions.awaitEmission()

            subject.moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
            ).collect()

            val emitted = emissions.awaitEmission { stamps ->
                stamps.none { it.collectionId == sourceCollectionId }
            }
            assertEquals(
                setOf("First", "Second"),
                emitted
                    .filter { it.collectionId == destinationCollectionId }
                    .map(Stamp::caption)
                    .toSet()
            )
            assertEquals(
                "Stamps of other collections must not be affected",
                "Bystander",
                emitted.single { it.collectionId == defaultCollectionId }.caption
            )
        }
    }

    @Test
    fun moving_an_empty_collection_emits_only_zero_progress(

    ) = runBlocking {

        val progress = subject.moveStampsBetweenCollections(
            sourceCollectionId = sourceCollectionId,
            destinationCollectionId = destinationCollectionId,
        ).toList()

        assertEquals(listOf(0 to 0), progress)
        assertTrue(stampFilesIn(destinationCollectionDirectory).isEmpty())
    }

    // endregion

    private fun newRepository(
        directory: File = stampDirectory,
    ) = FsStampRepository(
        stampDirectory = directory,
        safFileLocksmith = SafFileLocksmith(
            stampDirectory = directory,
            stampDirectoryDocumentUri = Uri.parse("primary:Pictures/PressCutStampsTest"),
            contentResolver = context.contentResolver,
        ),
        scanFilesWithMediaScanner = ScanFilesWithMediaScanner(context),
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

    /**
     * Adds stamps to the collection, each with its caption.
     * The captions must be unique within the test, as they are used
     * to find the stamps with [findStampByCaption].
     */
    private suspend fun seedStamps(
        collectionId: String,
        vararg captions: String,
    ) {
        captions.forEach { caption ->
            subject.addStamp(
                collectionId = collectionId,
                webpBytes = giftWebpBytes,
                caption = caption,
                takenAtLocal = TAKEN_AT,
                shape = StampShapeOneStampWithoutCornersLandscape,
            )
        }
    }

    private suspend fun findStampByCaption(caption: String): Stamp =
        subject.getStamps().single { it.caption == caption }

    private fun stampFilesIn(directory: File): List<File> =
        directory
            .listFiles { file -> file.extension == FsStampRepository.WEBP_EXTENSION }
            ?.toList()
            ?: emptyList()

    private suspend fun <T> withStampsFlowEmissions(
        block: suspend (emissions: Channel<List<Stamp>>) -> T,
    ): T = coroutineScope {
        val emissions = Channel<List<Stamp>>(Channel.UNLIMITED)
        val collectingJob = launch(Dispatchers.IO) {
            subject.getStampsFlow().collect { emissions.send(it.toList()) }
        }

        try {
            block(emissions)
        } finally {
            collectingJob.cancel()
        }
    }

    private suspend fun Channel<List<Stamp>>.awaitEmission(
        predicate: (List<Stamp>) -> Boolean = { true },
    ): List<Stamp> =
        withTimeoutOrNull(AWAIT_TIMEOUT) {
            var emission = receive()
            while (!predicate(emission)) {
                emission = receive()
            }
            emission
        }
            ?: run {
                fail("The expected stamp list was not emitted in time")
                throw IllegalStateException()
            }

    private suspend fun awaitCondition(
        failureMessage: String,
        condition: () -> Boolean,
    ) {
        val isSatisfied = withTimeoutOrNull(AWAIT_TIMEOUT) {
            while (!condition()) {
                delay(20.milliseconds)
            }
            true
        }

        assertTrue(failureMessage, isSatisfied == true)
        assertFalse(failureMessage, isSatisfied == null)
    }

    companion object {
        const val CAPTION = "WebP caption 🤭 Зачем вы это читаете?"
        const val NEW_CAPTION = "THE CAPTION 🖋 is updated"
        val AWAIT_TIMEOUT = 3_000L.milliseconds
        val TAKEN_AT: LocalDateTime =
            LocalDateTime.of(2026, 10, 31, 14, 14, 14)
    }
}
