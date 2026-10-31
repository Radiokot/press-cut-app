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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
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
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampSmall
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeOneStampWithoutCornersLandscape
import java.io.File
import java.time.LocalDateTime
import java.util.Optional
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class FsStampRepositoryTest {

    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(120)

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var stampDirectory: File
    private lateinit var subject: FsStampRepository
    private val defaultCollectionId = "0"
    private lateinit var defaultCollectionDirectory: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        stampDirectory = temporaryFolder.newFolder()
        defaultCollectionDirectory = File(stampDirectory, defaultCollectionId)
        defaultCollectionDirectory.mkdirs()

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
    fun addStamp_webpBytes_savesFileWithDetails() = runBlocking {
        val collectionId = "424242"
        val collectionDirectory = File(stampDirectory, collectionId)
        collectionDirectory.mkdirs()
        val stampFile = getGiftStamp("1778232389641.webp")

        subject.addStamp(
            collectionId = collectionId,
            webpBytes = stampFile.readBytes(),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampWithoutCornersLandscape,
        )

        val collectionFiles = collectionDirectory.listFiles()!!
        assertEquals(
            "The stamp must be saved to the collection directory",
            1,
            collectionFiles.size
        )
        assertEquals(
            "The stamp must be saved with the WebP extension",
            FsStampRepository.WEBP_EXTENSION,
            collectionFiles.single().extension
        )
        assertTrue(
            "The stamp file name must be numeric",
            collectionFiles.single().nameWithoutExtension.all(Char::isDigit)
        )

        val stamp = subject.getStamps().single()
        assertEquals(
            "The stamp must be saved with its ID in the name",
            collectionFiles.single().nameWithoutExtension,
            stamp.id
        )
        assertEquals(collectionId, stamp.collectionId)
        assertEquals(CAPTION, stamp.caption)
        assertEquals(TAKEN_AT, stamp.takenAtLocal)
        assertEquals(StampShapeOneStampWithoutCornersLandscape, stamp.shape)
    }

    @Test
    fun addStamp_bitmap_compressesToWebpAndKeepsSize() = runBlocking {
        val bitmap = createBitmap(64, 48, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.MAGENTA)

        subject.addStamp(
            collectionId = defaultCollectionId,
            imageBitmap = bitmap,
            caption = "From bitmap",
            takenAtLocal = TAKEN_AT,
            shape = StampShapeA,
        )

        val stamp = subject.getStamps().single()
        val (bytes, size) = subject.getStampImageBytesAndSize(stamp)
        assertEquals("From bitmap", stamp.caption)
        assertEquals(bitmap.width, size.width)
        assertEquals(bitmap.height, size.height)

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("The saved stamp image must be decodable", decoded)
        assertEquals(bitmap.width, decoded.width)
        assertEquals(bitmap.height, decoded.height)
    }

    @Test
    fun addStamp_withoutTakenAt_usesCurrentTime() = runBlocking {
        val bitmap = createBitmap(64, 48, Bitmap.Config.RGB_565)

        val before = LocalDateTime.now()
        subject.addStamp(
            collectionId = defaultCollectionId,
            imageBitmap = bitmap,
            caption = null,
            shape = StampShapeA,
        )
        val after = LocalDateTime.now()

        val takenAt = subject.getStamps().single().takenAtLocal
        assertFalse("$takenAt is before $before", takenAt.isBefore(before))
        assertFalse("$takenAt is after $after", takenAt.isAfter(after))
    }

    @Test
    fun addStamp_stream_copiesEverything() = runBlocking {
        val sourceStampFile = getGiftStamp("1778232389641.webp")

        subject.addStamp(
            collectionId = defaultCollectionId,
            stampWebpName = sourceStampFile.name,
            stampWebpContent = sourceStampFile.inputStream(),
        )

        assertArrayEquals(
            sourceStampFile.readBytes(),
            File(defaultCollectionDirectory, sourceStampFile.name).readBytes()
        )
        val stamp = subject.getStamps().single()
        assertEquals("1778232389641", stamp.id)
        assertEquals(defaultCollectionId, stamp.collectionId)
        assertEquals("Little guy", stamp.caption)
        assertEquals(LocalDateTime.parse("2026-05-08T12:26:29.641377"), stamp.takenAtLocal)
        assertEquals(StampShapeA, stamp.shape)
    }

    @Test(expected = IllegalStateException::class)
    fun addStamp_missingCollectionDirectory_throws() = runBlocking {
        subject.addStamp(
            collectionId = "no-such-collection",
            webpBytes = getGiftStamp("1778232389641.webp").readBytes(),
            caption = null,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeA,
        )
        fail("The stamp must not be added to a missing collection")
    }

    @Test
    fun getStampImageBytesAndSize_returnsFileBytesAndImageSize() = runBlocking {
        val stampFile = getGiftStamp("1778232389641.webp")

        subject.addStamp(
            collectionId = defaultCollectionId,
            stampWebpName = stampFile.name,
            stampWebpContent = stampFile.inputStream(),
        )

        val stamp = subject.getStamps().single()
        val (bytes, size) = subject.getStampImageBytesAndSize(stamp)

        assertArrayEquals(stampFile.readBytes(), bytes)
        assertEquals(512, size.width)
        assertEquals(636, size.height)
    }

    @Test
    fun getStamps_readsStampsFromAllCollections() = runBlocking {
        val collectionAId = "1"
        val collectionADirectory = File(stampDirectory, collectionAId)
        collectionADirectory.mkdirs()
        getGiftStamp("1778247890443.webp")
            .copyTo(File(collectionADirectory, "1778247890443.webp"))
        val collectionBId = "2"
        val collectionBDirectory = File(stampDirectory, collectionBId)
        collectionBDirectory.mkdirs()
        getGiftStamp("1778521786065.webp")
            .copyTo(File(collectionBDirectory, "1778521786065.webp"))

        val stamps = subject.getStamps().sortedBy(Stamp::takenAtLocal)

        assertEquals(
            "2 stamps must be read",
            2,
            stamps.size
        )

        val (stampA, stampB) = stamps
        assertEquals("1778247890443", stampA.id)
        assertEquals("Pepsi", stampA.caption)
        assertEquals(collectionAId, stampA.collectionId)
        assertEquals(LocalDateTime.parse("2026-05-08T16:44:50.443382"), stampA.takenAtLocal)
        assertEquals(StampShapeA, stampA.shape)

        assertEquals("1778521786065", stampB.id)
        assertEquals("Tea", stampB.caption)
        assertEquals(collectionBId, stampB.collectionId)
        assertEquals(LocalDateTime.parse("2026-05-11T20:49:46.065936"), stampB.takenAtLocal)
        assertEquals(StampShapeA, stampB.shape)
    }

    @Test
    fun getStamps_silentlySkipsInvalidFiles() = runBlocking {
        getGiftStamp("1778247890443.webp")
            .copyTo(File(defaultCollectionDirectory, "1778247890443.webp"))
        File(defaultCollectionDirectory, "12345.png")
            .writeBytes(ByteArray(10))
        File(defaultCollectionDirectory, "README")
            .writeText("Do not actually read me")
        File(defaultCollectionDirectory, ".nomedia")
            .createNewFile()

        val stamps = subject.getStamps()

        assertEquals(
            "Only 1 valid stamp must be read",
            1,
            stamps.size
        )
        val stamp = stamps.single()
        assertEquals("1778247890443", stamp.id)
    }

    @Test
    fun getStamps_doesNotReadStampsWithoutCollection() = runBlocking {
        getGiftStamp("1778247890443.webp")
            .copyTo(File(defaultCollectionDirectory, "1778247890443.webp"))
        getGiftStamp("1778232389641.webp")
            .copyTo(File(stampDirectory, "1778232389641.webp"))

        val stamps = subject.getStamps()

        assertEquals(
            "Only 1 valid stamp must be read",
            1,
            stamps.size
        )
        val stamp = stamps.single()
        assertEquals("1778247890443", stamp.id)
    }

    @Test
    fun getStamp_knownId_returnsStamp() = runBlocking {
        val id = "1778247890443"
        getGiftStamp("1778247890443.webp")
            .copyTo(File(defaultCollectionDirectory, "1778247890443.webp"))

        val stamp = subject.getStamp(id)

        assertNotNull(stamp)
        assertEquals(id, stamp!!.id)
    }

    @Test
    fun getStamp_unknownId_returnsNull() = runBlocking {
        getGiftStamp("1778247890443.webp")
            .copyTo(File(defaultCollectionDirectory, "1778247890443.webp"))

        assertNull(subject.getStamp("1"))
    }

    @Test
    fun getStamp_returnsStampAddedBefore() = runBlocking {
        subject.addStamp(
            collectionId = defaultCollectionId,
            imageBitmap = createBitmap(64, 48, Bitmap.Config.RGB_565),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampWithoutCornersLandscape,
        )
        subject = newRepository()
        val stamp = subject.getStamps().single()

        assertEquals(defaultCollectionId, stamp.collectionId)
        assertEquals(CAPTION, stamp.caption)
        assertEquals(TAKEN_AT, stamp.takenAtLocal)
        assertEquals(StampShapeOneStampWithoutCornersLandscape, stamp.shape)
    }

    @Test
    fun updateStamp_newCaption_isPersistedAndCacheUpdated() = runBlocking {
        subject.addStamp(
            collectionId = defaultCollectionId,
            webpBytes = getGiftStamp("1778232389641.webp").readBytes(),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampSmall,
        )
        val stamp = subject.getStamps().single()

        val newCaption = "New $CAPTION"
        subject.updateStamp(
            stamp = stamp,
            newCaption = Optional.of(newCaption),
        )

        assertEquals(newCaption, subject.getStamp(stamp.id)!!.caption)
        val stampFromFile = newRepository().getStamp(stamp.id)!!
        assertEquals(newCaption, stampFromFile.caption)
    }

    @Test
    fun updateStamp_emptyOptional_removesCaption() = runBlocking {
        subject.addStamp(
            collectionId = defaultCollectionId,
            webpBytes = getGiftStamp("1778232389641.webp").readBytes(),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampSmall,
        )
        val stamp = subject.getStamps().single()

        subject.updateStamp(
            stamp = stamp,
            newCaption = Optional.empty(),
        )

        assertNull(subject.getStamp(stamp.id)!!.caption)
        val fromDisk = newRepository().getStamp(stamp.id)!!
        assertNull(fromDisk.caption)
    }

    @Test
    fun updateStamp_nullNewCaption_keepsCaption() = runBlocking {
        subject.addStamp(
            collectionId = defaultCollectionId,
            webpBytes = getGiftStamp("1778232389641.webp").readBytes(),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampSmall,
        )
        val stamp = subject.getStamps().single()

        subject.updateStamp(
            stamp = stamp,
            newCaption = null,
        )

        assertEquals(CAPTION, subject.getStamp(stamp.id)!!.caption)
        val fromDisk = newRepository().getStamp(stamp.id)!!
        assertEquals(CAPTION, fromDisk.caption)
    }

    @Test
    fun updateStamp_keepsOtherDetailsAndImage() = runBlocking {
        subject.addStamp(
            collectionId = defaultCollectionId,
            webpBytes = getGiftStamp("1778232389641.webp").readBytes(),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampSmall,
        )
        val stamp = subject.getStamps().single()
        val (bytesBefore, sizeBefore) = subject.getStampImageBytesAndSize(stamp)

        subject.updateStamp(
            stamp = stamp,
            newCaption = Optional.of("New $CAPTION"),
        )

        val fromDisk = newRepository().getStamp(stamp.id)!!
        assertEquals(defaultCollectionId, fromDisk.collectionId)
        assertEquals(TAKEN_AT, fromDisk.takenAtLocal)
        assertEquals(StampShapeOneStampSmall, fromDisk.shape)
        val (bytesAfter, sizeAfter) = subject.getStampImageBytesAndSize(fromDisk)
        assertEquals(sizeBefore, sizeAfter)
        val pixelsBefore = BitmapFactory.decodeByteArray(bytesBefore, 0, bytesBefore.size)
        val pixelsAfter = BitmapFactory.decodeByteArray(bytesAfter, 0, bytesAfter.size)
        assertTrue(
            "Stamp image must not be changed by details update",
            pixelsBefore.sameAs(pixelsAfter)
        )
    }

    @Test
    fun deleteStamps_updatesListImmediatelyAndDeleteFilesEventually() = runBlocking {
        val stampFileToRemove1 = File(defaultCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFileToRemove1)
        val stampFileToRemove2 = File(defaultCollectionDirectory, "1778247890443.webp")
        getGiftStamp("1778247890443.webp").copyTo(stampFileToRemove2)
        val stampFileToKeep = File(defaultCollectionDirectory, "1778521786065.webp")
        getGiftStamp("1778521786065.webp").copyTo(stampFileToKeep)
        val stamps = subject.getStamps().sortedBy(Stamp::id)
        val stampsToRemove = stamps.take(2)
        val stampsToKeep = stamps.takeLast(1)

        subject.deleteStamps(
            collectionId = defaultCollectionId,
            stampIds = stampsToRemove.map(Stamp::id),
        )

        assertEquals(stampsToKeep, subject.getStamps())
        assertTrue(stampFileToKeep.exists())
        do {
            delay(150.milliseconds)
        } while (stampFileToRemove1.exists() || stampFileToRemove2.exists())
        assertEquals(stampsToKeep, newRepository().getStamps())
    }

    @Test
    fun deleteStamp_updatesListImmediatelyAndDeleteFileEventually() = runBlocking {
        val stampFileToRemove = File(defaultCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFileToRemove)
        val stampFileToKeep = File(defaultCollectionDirectory, "1778521786065.webp")
        getGiftStamp("1778521786065.webp").copyTo(stampFileToKeep)
        val stamps = subject.getStamps().sortedBy(Stamp::id)
        val stampToRemove = stamps.first()
        val stampToKeep = stamps.last()

        subject.deleteStamp(stampToRemove)

        assertEquals(listOf(stampToKeep), subject.getStamps())
        assertTrue(stampFileToKeep.exists())
        do {
            delay(150.milliseconds)
        } while (stampFileToRemove.exists())
        assertEquals(listOf(stampToKeep), newRepository().getStamps())
    }

    @Test
    fun deleteStamps_onlyAffectsGivenCollection() = runBlocking {
        val collectionAId = "1"
        val collectionADirectory = File(stampDirectory, collectionAId)
        collectionADirectory.mkdirs()
        getGiftStamp("1778247890443.webp")
            .copyTo(File(collectionADirectory, "1111111111.webp"))
        val collectionBId = "2"
        val collectionBDirectory = File(stampDirectory, collectionBId)
        collectionBDirectory.mkdirs()
        getGiftStamp("1778521786065.webp")
            .copyTo(File(collectionBDirectory, "1111111111.webp"))
        val (stampToDelete, stampToKeep) = subject.getStamps().sortedBy(Stamp::collectionId)

        subject.deleteStamps(
            collectionId = collectionAId,
            stampIds = listOf(stampToDelete.id),
        )

        assertEquals(listOf(stampToKeep), subject.getStamps())
        do {
            delay(150.milliseconds)
        } while (File(collectionADirectory, "1111111111.webp").exists())
        assertTrue(File(collectionBDirectory, "1111111111.webp").exists())
        assertEquals(listOf(stampToKeep), newRepository().getStamps())
    }

    @Test
    fun deleteStamps_notFoundId_succeedsChangingNothing() = runBlocking {
        val stampFileToKeep = File(defaultCollectionDirectory, "1778521786065.webp")
        getGiftStamp("1778521786065.webp").copyTo(stampFileToKeep)
        val stampsToKeep = subject.getStamps()

        subject.deleteStamps(
            collectionId = defaultCollectionId,
            stampIds = listOf("not-found-id"),
        )

        assertEquals(stampsToKeep, subject.getStamps())
        assertTrue(stampFileToKeep.exists())
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
