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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    companion object {
        const val CAPTION = "WebP caption 🤭 Зачем вы это читаете?"
        val TAKEN_AT: LocalDateTime =
            LocalDateTime.of(2026, 10, 31, 14, 14, 14)
    }
}
