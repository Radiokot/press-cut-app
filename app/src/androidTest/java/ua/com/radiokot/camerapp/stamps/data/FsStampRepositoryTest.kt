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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
import java.nio.file.NoSuchFileException
import java.time.LocalDateTime
import java.util.Optional
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class FsStampRepositoryTest {

    @get:Rule
    val timeout: Timeout = Timeout.seconds(5)

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var stampDirectory: File
    private lateinit var subject: FsStampRepository
    private val defaultCollectionId = "0"
    private lateinit var defaultCollectionDirectory: File
    private val sourceCollectionId = "1"
    private lateinit var sourceCollectionDirectory: File
    private val destinationCollectionId = "2"
    private lateinit var destinationCollectionDirectory: File

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

    @Test
    fun getStampFile_returnsCorrectFile() = runBlocking {
        val stampFile = File(defaultCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFile)
        val stamp = subject.getStamps().single()

        val file = subject.getStampFile(stamp)

        assertEquals(stampFile, file)
    }

    @Test
    fun getStampsFlow_onStampAddOrDelete_emits() = runBlocking {
        getGiftStamp("1778232389641.webp")
            .copyTo(File(defaultCollectionDirectory, "1778232389641.webp"))
        val emissions = mutableListOf<List<Stamp>>()

        coroutineScope {
            launch {
                subject
                    .getStampsFlow()
                    .take(3)
                    .collect { emissions += it.toList() }
            }
            launch {
                delay(1.seconds)
                subject.addStamp(
                    collectionId = defaultCollectionId,
                    webpBytes = getGiftStamp("1778247890443.webp").readBytes(),
                    caption = CAPTION,
                    takenAtLocal = TAKEN_AT,
                    shape = StampShapeOneStampSmall,
                )
                delay(1.seconds)
                subject.deleteStamps(
                    collectionId = defaultCollectionId,
                    stampIds = listOf("1778232389641"),
                )
            }
        }

        assertEquals(1, emissions[0].distinct().size)
        assertEquals(
            "Must emit a list with the newly added stamp",
            2,
            emissions[1].distinct().size
        )
        assertEquals(
            "Must emit a list with the remaining stamp",
            1,
            emissions[2].distinct().size
        )
    }

    @Test
    fun getStampsFlow_onStampUpdate_emits() = runBlocking {
        getGiftStamp("1778232389641.webp")
            .copyTo(File(defaultCollectionDirectory, "1778232389641.webp"))
        val emissions = mutableListOf<List<Stamp>>()

        coroutineScope {
            launch {
                subject
                    .getStampsFlow()
                    .take(2)
                    .collect { emissions += it.toList() }
            }
            launch {
                delay(1.seconds)
                val stamp = subject.getStamps().single()
                subject.updateStamp(
                    stamp = stamp,
                    newCaption = Optional.of("New $CAPTION"),
                )
            }
        }

        assertEquals(1, emissions[0].distinct().size)
        assertEquals(
            "Stamp list after update must be equal as stamps are equal by ID",
            emissions[0],
            emissions[1]
        )
        assertEquals(
            "The stamp in the second emission must be updated",
            "New $CAPTION",
            emissions[1].single().caption
        )
    }

    @Test
    fun newStampId_returnsUniqueIncreasingNumericIds() = runBlocking {
        val ids = Array(200) { FsStampRepository.newStampId() }

        assertEquals(ids.size, ids.distinct().size)
        assertTrue(
            "Stamp IDs must be numeric, otherwise the files are not recognized as stamps",
            ids.all { it.isNotEmpty() && it.all(Char::isDigit) }
        )
        assertEquals(
            "Stamp IDs must only grow",
            ids.map(String::toLong).sorted(),
            ids.map(String::toLong)
        )
    }

    @Test
    fun moveStampsBetweenCollections_byIds_movesFilesAndKeepsOthers() = runBlocking {
        val stampFileToMove1 = File(sourceCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFileToMove1)
        val stampFileToMove2 = File(sourceCollectionDirectory, "1778247890443.webp")
        getGiftStamp("1778247890443.webp").copyTo(stampFileToMove2)
        val stampFileToKeep = File(sourceCollectionDirectory, "1778521786065.webp")
        getGiftStamp("1778521786065.webp").copyTo(stampFileToKeep)

        val bytesToMove1 = stampFileToMove1.readBytes()
        val bytesToMove2 = stampFileToMove2.readBytes()
        val bytesToKeep = stampFileToKeep.readBytes()

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf("1778232389641", "1778247890443"),
            )
            .toList()

        assertFalse(
            "The moved stamp file must not remain in the source collection",
            stampFileToMove1.exists()
        )
        assertFalse(
            "The moved stamp file must not remain in the source collection",
            stampFileToMove2.exists()
        )
        assertArrayEquals(
            bytesToMove1,
            File(destinationCollectionDirectory, "1778232389641.webp").readBytes()
        )
        assertArrayEquals(
            bytesToMove2,
            File(destinationCollectionDirectory, "1778247890443.webp").readBytes()
        )
        assertTrue(
            "The stamp not mentioned in the move must stay in the source collection",
            stampFileToKeep.exists()
        )
        assertArrayEquals(bytesToKeep, stampFileToKeep.readBytes())
        assertFalse(
            "The stamp not mentioned in the move must not appear in the destination collection",
            File(destinationCollectionDirectory, "1778521786065.webp").exists()
        )
    }

    @Test
    fun moveStampsBetweenCollections_byIds_emitsProgressFromZeroToTotal() = runBlocking {
        getGiftStamp("1778232389641.webp")
            .copyTo(File(sourceCollectionDirectory, "1778232389641.webp"))
        getGiftStamp("1778247890443.webp")
            .copyTo(File(sourceCollectionDirectory, "1778247890443.webp"))

        val emissions = subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf("1778232389641", "1778247890443"),
            )
            .toList()

        assertEquals(
            "Must emit the initial zero progress, then the progress after each moved stamp",
            listOf(0 to 2, 1 to 2, 2 to 2),
            emissions
        )
    }

    @Test
    fun moveStampsBetweenCollections_byIds_emptyList_emitsOnlyInitialProgress() = runBlocking {
        val stampFile = File(sourceCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFile)

        val emissions = subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = emptyList(),
            )
            .toList()

        assertEquals(listOf(0 to 0), emissions)
        assertTrue(stampFile.exists())
        assertEquals(0, destinationCollectionDirectory.listFiles()!!.size)
    }

    @Test
    fun moveStampsBetweenCollections_byIds_updatesCache() = runBlocking {
        val idToMove = "1778232389641"
        val idToKeep = "1778247890443"
        getGiftStamp("$idToMove.webp")
            .copyTo(File(sourceCollectionDirectory, "$idToMove.webp"))
        getGiftStamp("$idToKeep.webp")
            .copyTo(File(sourceCollectionDirectory, "$idToKeep.webp"))

        // Initialize the cache.
        assertEquals(2, subject.getStamps().size)

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf(idToMove),
            )
            .toList()

        val stamps = subject.getStamps()
        assertEquals(
            "The number of stamps must not change",
            2,
            stamps.size
        )

        val movedStamp = stamps.single { it.id == idToMove }
        assertEquals(destinationCollectionId, movedStamp.collectionId)
        assertEquals(
            "The moved stamp must point to the new file",
            "file://${File(destinationCollectionDirectory, "$idToMove.webp").absolutePath}",
            movedStamp.imageUri
        )

        val keptStamp = stamps.single { it.id == idToKeep }
        assertEquals(sourceCollectionId, keptStamp.collectionId)
        assertEquals(
            "The kept stamp must still point to its file",
            "file://${File(sourceCollectionDirectory, "$idToKeep.webp").absolutePath}",
            keptStamp.imageUri
        )
    }

    @Test
    fun moveStampsBetweenCollections_byIds_cacheNotInitialized_movesFilesAndLoadsMovedStamps() =
        runBlocking {
            val id = "1778232389641"
            getGiftStamp("$id.webp")
                .copyTo(File(sourceCollectionDirectory, "$id.webp"))

            // No stamps are read before the move, so the cache is not initialized.
            subject
                .moveStampsBetweenCollections(
                    sourceCollectionId = sourceCollectionId,
                    destinationCollectionId = destinationCollectionId,
                    stampIds = listOf(id),
                )
                .toList()

            val stamp = subject.getStamps().single()

            assertEquals(id, stamp.id)
            assertEquals(destinationCollectionId, stamp.collectionId)
            assertTrue(File(destinationCollectionDirectory, "$id.webp").exists())
            assertFalse(File(sourceCollectionDirectory, "$id.webp").exists())
        }

    @Test
    fun moveStampsBetweenCollections_byIds_isPersisted() = runBlocking {
        val id = "1778232389641"
        getGiftStamp("$id.webp")
            .copyTo(File(sourceCollectionDirectory, "$id.webp"))

        // Initialize the cache.
        subject.getStamps()

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf(id),
            )
            .toList()

        val stampFromDisk = newRepository().getStamps().single()

        assertEquals(id, stampFromDisk.id)
        assertEquals(destinationCollectionId, stampFromDisk.collectionId)
    }

    @Test
    fun moveStampsBetweenCollections_byIds_keepsStampDetailsAndImage() = runBlocking {
        subject.addStamp(
            collectionId = sourceCollectionId,
            webpBytes = getGiftStamp("1778232389641.webp").readBytes(),
            caption = CAPTION,
            takenAtLocal = TAKEN_AT,
            shape = StampShapeOneStampSmall,
        )
        val stamp = subject.getStamps().single()
        val (bytesBefore, sizeBefore) = subject.getStampImageBytesAndSize(stamp)

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf(stamp.id),
            )
            .toList()

        val fromDisk = newRepository().getStamp(stamp.id)!!

        assertEquals(destinationCollectionId, fromDisk.collectionId)
        assertEquals(CAPTION, fromDisk.caption)
        assertEquals(TAKEN_AT, fromDisk.takenAtLocal)
        assertEquals(StampShapeOneStampSmall, fromDisk.shape)

        val (bytesAfter, sizeAfter) = subject.getStampImageBytesAndSize(fromDisk)

        assertEquals(sizeBefore, sizeAfter)
        assertArrayEquals(
            "Stamp image must not be changed by moving",
            bytesBefore,
            bytesAfter
        )
    }

    @Test
    fun moveStampsBetweenCollections_byIds_doesNotAffectOtherCollections() = runBlocking {
        val thirdCollectionId = "3"
        val thirdCollectionDirectory = File(stampDirectory, thirdCollectionId)
        thirdCollectionDirectory.mkdirs()

        val stampFileToMove = File(sourceCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFileToMove)
        val stampFileInThirdCollection = File(thirdCollectionDirectory, "1778521786065.webp")
        getGiftStamp("1778521786065.webp").copyTo(stampFileInThirdCollection)

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
                stampIds = listOf("1778232389641"),
            )
            .toList()

        assertTrue(
            "The stamp of the third collection must stay where it is",
            stampFileInThirdCollection.exists()
        )

        val stamps = newRepository().getStamps().sortedBy(Stamp::id)

        assertEquals(2, stamps.size)
        assertEquals(destinationCollectionId, stamps[0].collectionId)
        assertEquals(thirdCollectionId, stamps[1].collectionId)
    }

    @Test(expected = NoSuchFileException::class)
    fun moveStampsBetweenCollections_byIds_missingDestinationDirectory_throws() = runBlocking {
        getGiftStamp("1778232389641.webp")
            .copyTo(File(sourceCollectionDirectory, "1778232389641.webp"))

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = "no-such-collection",
                stampIds = listOf("1778232389641"),
            )
            .toList()

        fail("The stamp must not be moved to a missing collection")
    }

    @Test
    fun moveStampsBetweenCollections_all_movesEveryStampOfSourceCollection() = runBlocking {
        val stampFile1 = File(sourceCollectionDirectory, "1778232389641.webp")
        getGiftStamp("1778232389641.webp").copyTo(stampFile1)
        val stampFile2 = File(sourceCollectionDirectory, "1778247890443.webp")
        getGiftStamp("1778247890443.webp").copyTo(stampFile2)
        val stampFile3 = File(sourceCollectionDirectory, "1778521786065.webp")
        getGiftStamp("1778521786065.webp").copyTo(stampFile3)

        val bytes1 = stampFile1.readBytes()
        val bytes2 = stampFile2.readBytes()
        val bytes3 = stampFile3.readBytes()

        subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
            )
            .toList()

        assertEquals(
            "All the stamps must be moved out of the source collection",
            0,
            sourceCollectionDirectory.listFiles()!!.size
        )
        assertArrayEquals(
            bytes1,
            File(destinationCollectionDirectory, "1778232389641.webp").readBytes()
        )
        assertArrayEquals(
            bytes2,
            File(destinationCollectionDirectory, "1778247890443.webp").readBytes()
        )
        assertArrayEquals(
            bytes3,
            File(destinationCollectionDirectory, "1778521786065.webp").readBytes()
        )

        val stamps = newRepository().getStamps()

        assertEquals(3, stamps.size)
        assertTrue(
            "All the stamps must belong to the destination collection",
            stamps.all { it.collectionId == destinationCollectionId }
        )
    }

    @Test
    fun moveStampsBetweenCollections_all_emitsProgressFromZeroToTotal() = runBlocking {
        getGiftStamp("1778232389641.webp")
            .copyTo(File(sourceCollectionDirectory, "1778232389641.webp"))
        getGiftStamp("1778247890443.webp")
            .copyTo(File(sourceCollectionDirectory, "1778247890443.webp"))
        getGiftStamp("1778521786065.webp")
            .copyTo(File(sourceCollectionDirectory, "1778521786065.webp"))

        val emissions = subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
            )
            .toList()

        assertEquals(
            "Must emit the initial zero progress, then the progress after each moved stamp",
            listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3),
            emissions
        )
    }

    @Test
    fun moveStampsBetweenCollections_all_silentlySkipsInvalidFiles() = runBlocking {
        getGiftStamp("1778247890443.webp")
            .copyTo(File(sourceCollectionDirectory, "1778247890443.webp"))

        val invalidPngFile = File(sourceCollectionDirectory, "12345.png")
        invalidPngFile.writeBytes(ByteArray(10))
        val readmeFile = File(sourceCollectionDirectory, "README")
        readmeFile.writeText("Do not actually move me")
        val noMediaFile = File(sourceCollectionDirectory, ".nomedia")
        noMediaFile.createNewFile()

        val emissions = subject
            .moveStampsBetweenCollections(
                sourceCollectionId = sourceCollectionId,
                destinationCollectionId = destinationCollectionId,
            )
            .toList()

        assertEquals(
            "Only 1 valid stamp must be moved",
            listOf(0 to 1, 1 to 1),
            emissions
        )
        assertTrue(File(destinationCollectionDirectory, "1778247890443.webp").exists())
        assertTrue(invalidPngFile.exists())
        assertTrue(readmeFile.exists())
        assertTrue(noMediaFile.exists())
        assertEquals(
            "Only the valid stamp must appear in the destination collection",
            1,
            destinationCollectionDirectory.listFiles()!!.size
        )
    }

    @Test
    fun moveStampsBetweenCollections_all_emptySourceCollection_emitsOnlyInitialProgress() =
        runBlocking {
            val emissions = subject
                .moveStampsBetweenCollections(
                    sourceCollectionId = sourceCollectionId,
                    destinationCollectionId = destinationCollectionId,
                )
                .toList()

            assertEquals(listOf(0 to 0), emissions)
            assertEquals(0, destinationCollectionDirectory.listFiles()!!.size)
        }

    @Test
    fun moveStampsBetweenCollections_all_missingSourceCollection_succeedsChangingNothing() =
        runBlocking {
            val stampFileToKeep = File(destinationCollectionDirectory, "1778521786065.webp")
            getGiftStamp("1778521786065.webp").copyTo(stampFileToKeep)

            val emissions = subject
                .moveStampsBetweenCollections(
                    sourceCollectionId = "no-such-collection",
                    destinationCollectionId = destinationCollectionId,
                )
                .toList()

            assertEquals(listOf(0 to 0), emissions)
            assertTrue(stampFileToKeep.exists())
            assertEquals(1, subject.getStamps().size)
        }

    @Test
    fun getStampsFlow_onStampsMove_emits() = runBlocking {
        val id = "1778232389641"
        getGiftStamp("$id.webp")
            .copyTo(File(sourceCollectionDirectory, "$id.webp"))

        val emissions = mutableListOf<List<Stamp>>()

        coroutineScope {
            launch {
                subject
                    .getStampsFlow()
                    .take(2)
                    .collect { emissions += it.toList() }
            }

            launch {
                delay(1.seconds)

                subject
                    .moveStampsBetweenCollections(
                        sourceCollectionId = sourceCollectionId,
                        destinationCollectionId = destinationCollectionId,
                        stampIds = listOf(id),
                    )
                    .toList()
            }
        }

        assertEquals(
            "The stamp in the first emission must be in the source collection",
            sourceCollectionId,
            emissions[0].single().collectionId
        )
        assertEquals(
            "Stamp list after move must be equal as stamps are equal by ID",
            emissions[0],
            emissions[1]
        )
        assertEquals(
            "The stamp in the second emission must be moved to the destination collection",
            destinationCollectionId,
            emissions[1].single().collectionId
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
