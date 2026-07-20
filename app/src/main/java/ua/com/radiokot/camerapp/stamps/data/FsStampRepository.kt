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

@file:OptIn(ExperimentalAtomicApi::class)

package ua.com.radiokot.camerapp.stamps.data

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import dalvik.annotation.optimization.FastNative
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.util.NativeLibrary
import ua.com.radiokot.camerapp.util.directByteBufferOf
import ua.com.radiokot.camerapp.util.getNullTerminatedString
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.Optional
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.io.path.absolutePathString
import kotlin.jvm.optionals.getOrNull
import kotlin.system.measureTimeMillis

class FsStampRepository(
    private val stampDirectory: File,
    private val safFileLocksmith: SafFileLocksmith,
    private val scanFilesWithMediaScanner: ScanFilesWithMediaScanner,
) : StampRepository {

    private val log by lazyLogger("FsStampRepo")

    private val coroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("FsStampRepo"))

    init {
        require(stampDirectory.exists()) {
            "Provided file doesn't exist: $stampDirectory"
        }

        require(stampDirectory.isDirectory) {
            "Provided file is not a directory: $stampDirectory"
        }

        require(stampDirectory.canRead()) {
            "Can't read the directory: $stampDirectory"
        }
    }

    private val isCacheInitialized = AtomicBoolean(false)
    private val cache: MutableList<Stamp> = mutableListOf()
    private val sharedFlow: MutableSharedFlow<List<Stamp>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 10,
        )

    override fun getStampsFlow(): Flow<List<Stamp>> = flow {

        if (!isCacheInitialized.exchange(true)) {
            initCache()
        }

        sharedFlow.collect(this)
    }

    override suspend fun getStamps(): List<Stamp> =
        getStampsFlow()
            .first()

    override suspend fun getStamp(
        id: String,
    ): Stamp? =
        getStamps()
            .find { it.id == id }

    suspend fun getStampImageBytesAndSize(
        stamp: Stamp,
    ): Pair<ByteArray, Size> = withContext(Dispatchers.IO) {

        val file = getStampFile(stamp)
        val fileBytes =
            if (file.canRead() && file.canWrite())
                file.readBytes()
            else
                safFileLocksmith.unlockAndRead(file)

        val sizeArray = IntArray(2)
        if (!getStampImageSize(
                webpBytes = directByteBufferOf(fileBytes),
                resultArray = sizeArray,
            )
        ) {
            error("Failed reading the stamp image size")
        }

        return@withContext Pair(fileBytes, Size(sizeArray[0], sizeArray[1]))
    }

    private external fun getStampImageSize(
        webpBytes: ByteBuffer,
        resultArray: IntArray,
    ): Boolean

    override suspend fun addStamp(
        collectionId: String,
        imageBitmap: Bitmap,
        caption: String?,
        shape: StampShape,
    ) =
        addStamp(
            collectionId = collectionId,
            imageBitmap = imageBitmap,
            caption = caption,
            takenAtLocal = LocalDateTime.now(),
            shape = shape,
        )

    suspend fun addStamp(
        collectionId: String,
        imageBitmap: Bitmap,
        caption: String?,
        takenAtLocal: LocalDateTime,
        shape: StampShape,
    ): Unit = withContext(Dispatchers.IO) {

        val webpBytes = ByteArrayOutputStream().use { stream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                imageBitmap.compress(
                    Bitmap.CompressFormat.WEBP_LOSSY,
                    90,
                    stream,
                )
            } else {
                imageBitmap.compress(
                    Bitmap.CompressFormat.WEBP,
                    90,
                    stream,
                )
            }

            stream.flush()
            stream.toByteArray()
        }

        addStamp(
            collectionId = collectionId,
            webpBytes = webpBytes,
            caption = caption,
            takenAtLocal = takenAtLocal,
            shape = shape,
        )
    }

    suspend fun addStamp(
        collectionId: String,
        webpBytes: ByteArray,
        caption: String?,
        takenAtLocal: LocalDateTime,
        shape: StampShape,
    ): Unit = withContext(Dispatchers.IO) {

        val id = newStampId()

        val outputFile = getStampFile(
            id = id,
            collectionId = collectionId,
        )

        saveStampWithDetails(
            outputFile = outputFile,
            webpBytes = webpBytes,
            caption = caption,
            takenAtLocal = takenAtLocal,
            shape = shape,
        )

        scanFilesWithMediaScanner(
            pathsWithMimeType = listOf(outputFile.absolutePath to STAMP_FILE_CONTENT_TYPE),
        )

        if (isCacheInitialized.load()) {
            cache += Stamp(
                id = id,
                collectionId = collectionId,
                caption = caption,
                imageUri = outputFile.absolutePath.toImageUri(),
                takenAtLocal = takenAtLocal,
                shape = shape,
            )
            sharedFlow.emit(cache)
        }
    }

    suspend fun addStamp(
        collectionId: String,
        stampWebpName: String,
        stampWebpContent: InputStream,
    ): Unit = withContext(Dispatchers.IO) {

        check(!isCacheInitialized.load()) {
            "This method can't be called when the cache is already initialized"
        }

        getStampFileByName(
            fileName = stampWebpName,
            collectionId = collectionId,
        )
            .outputStream()
            .use(stampWebpContent::copyTo)
    }

    override suspend fun updateStamp(
        stamp: Stamp,
        newCaption: Optional<String>?,
    ) = withContext(Dispatchers.IO) {

        val file = getStampFile(stamp)
        val captionToSet =
            if (newCaption != null)
                newCaption.getOrNull()
            else
                stamp.caption

        val webpBytes =
            if (file.canRead() && file.canWrite())
                file.readBytes()
            else
                safFileLocksmith.unlockAndRead(file)

        saveStampWithDetails(
            outputFile = file,
            webpBytes = webpBytes,
            caption = captionToSet,
            takenAtLocal = stamp.takenAtLocal,
            shape = stamp.shape,
        )

        val updatedStamp = stamp.copy(
            newCaption = captionToSet,
        )

        if (isCacheInitialized.load()) {
            cache[cache.indexOf(stamp)] = updatedStamp
            sharedFlow.emit(cache)
        }
    }

    private suspend fun saveStampWithDetails(
        outputFile: File,
        webpBytes: ByteArray,
        caption: String?,
        takenAtLocal: LocalDateTime,
        shape: StampShape,
    ) = withContext(Dispatchers.IO) {
        if (!saveStampWithDetails(
                filePathString = outputFile.absolutePath,
                webpBytes = directByteBufferOf(webpBytes),
                captionStringOptional = caption,
                takenAtLocalString = takenAtLocal.toString(),
                shapeStringOptional =
                    if (shape !is StampShapeA)
                        shape.name
                    else
                        null,
            )
        ) {
            error("Failed saving the stamp")
        }
    }

    private external fun saveStampWithDetails(
        filePathString: String,
        webpBytes: ByteBuffer,
        captionStringOptional: String?,
        takenAtLocalString: String,
        shapeStringOptional: String?,
    ): Boolean

    override suspend fun deleteStamps(
        collectionId: String,
        stampIds: Collection<String>,
    ) {
        coroutineScope.launch {
            log.debug {
                "deleteStamps(): deleting the files async:" +
                        "\ncollectionId=$collectionId" +
                        "\nstamps=${stampIds.size}"
            }

            val toScanWithMediaScanner = mutableListOf<Pair<String, String>>()

            stampIds.forEach { stampId ->
                launch {
                    val file = getStampFile(
                        id = stampId,
                        collectionId = collectionId,
                    )

                    toScanWithMediaScanner += file.absolutePath to STAMP_FILE_CONTENT_TYPE

                    if (file.exists()) {
                        if (file.canWrite()) {
                            file.delete()
                        } else {
                            safFileLocksmith.unlockAndDelete(file)
                        }
                    }
                }
            }

            scanFilesWithMediaScanner(
                pathsWithMimeType = toScanWithMediaScanner,
            )
        }

        if (isCacheInitialized.load()) {
            cache.removeAll { it.id in stampIds }
            sharedFlow.emit(cache)
        }
    }

    override fun moveStampsBetweenCollections(
        sourceCollectionId: String,
        destinationCollectionId: String,
    ): Flow<Pair<Int, Int>> = flow {

        val sourceCollectionDirectory =
            File(stampDirectory, sourceCollectionId)
        val stampIdsToMove =
            sourceCollectionDirectory
                .listFiles(::isStamp)
                ?.map(File::nameWithoutExtension)
                ?: emptyList()

        moveStampsBetweenCollections(
            sourceCollectionId = sourceCollectionId,
            destinationCollectionId = destinationCollectionId,
            stampIds = stampIdsToMove,
        ).collect(this)
    }.flowOn(Dispatchers.IO)

    override fun moveStampsBetweenCollections(
        sourceCollectionId: String,
        destinationCollectionId: String,
        stampIds: Collection<String>,
    ): Flow<Pair<Int, Int>> {

        val movedStampPathsById = mutableMapOf<String, Pair<String, String>>()

        return channelFlow {
            val movedStampsChannel = this.channel

            log.debug {
                "moveStampsBetweenCollections(): moving the files async:" +
                        "\nsourceCollectionId=$sourceCollectionId" +
                        "\ndestinationCollectionId=$destinationCollectionId" +
                        "\nstamps=${stampIds.size}"
            }

            stampIds.forEach { stampId ->
                launch {
                    val stampSourceFile =
                        getStampFile(
                            id = stampId,
                            collectionId = sourceCollectionId,
                        )
                    val stampSourcePath =
                        FileSystems.getDefault().getPath(stampSourceFile.path)
                    val stampDestinationFile =
                        getStampFile(
                            id = stampId,
                            collectionId = destinationCollectionId,
                        )
                    val stampDestinationPath =
                        FileSystems.getDefault().getPath(stampDestinationFile.path)

                    if (stampSourceFile.canWrite()) {
                        Files.move(
                            stampSourcePath,
                            stampDestinationPath,
                            StandardCopyOption.ATOMIC_MOVE,
                        )
                    } else {
                        safFileLocksmith.unlockAndMove(
                            lockedSourceFile = stampSourceFile,
                            destinationFile = stampDestinationFile,
                        )
                    }

                    movedStampsChannel.send(
                        Triple(
                            stampId,
                            stampSourcePath.absolutePathString(),
                            stampDestinationPath.absolutePathString(),
                        )
                    )
                }
            }
        }
            .map { (stampId, sourcePath, destinationPath) ->
                movedStampPathsById[stampId] = sourcePath to destinationPath
                movedStampPathsById.size to stampIds.size
            }
            .onStart {
                emit(0 to stampIds.size)
            }
            .onCompletion {
                if (isCacheInitialized.load()) {
                    cache.indices.forEach { i ->
                        val stamp = cache[i]

                        if (!movedStampPathsById.containsKey(stamp.id)) {
                            return@forEach
                        }

                        val (_, destinationPath) = movedStampPathsById.getValue(stamp.id)

                        cache[i] = stamp.copy(
                            newCollectionId = destinationCollectionId,
                            newImageUri = destinationPath.toImageUri(),
                        )
                    }
                    sharedFlow.emit(cache)
                }

                scanFilesWithMediaScanner(
                    pathsWithMimeType =
                        movedStampPathsById
                            .values
                            .flatMapTo(mutableListOf()) { (sourcePath, destinationPath) ->
                                sequenceOf(
                                    sourcePath to STAMP_FILE_CONTENT_TYPE,
                                    destinationPath to STAMP_FILE_CONTENT_TYPE,
                                )
                            },
                )
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun initCache() = withContext(Dispatchers.IO) {
        val stampDirectoryAbsolutePath = stampDirectory.absolutePath

        val tookMs = measureTimeMillis {
            val detailsBuffer =
                getStampDetailsBuffer(
                    stampDirectoryPath = stampDirectoryAbsolutePath,
                )
                    ?: error("Failed reading the stamp details")

            while (detailsBuffer.hasRemaining()) {
                val stampId = detailsBuffer.getNullTerminatedString()
                val stampCollectionId = detailsBuffer.getNullTerminatedString()

                val stampCaption =
                    detailsBuffer
                        .getNullTerminatedString()
                        .takeIf(String::isNotEmpty)

                val stampTakenAtLocal =
                    LocalDateTime
                        .parse(detailsBuffer.getNullTerminatedString())

                val stampShape =
                    detailsBuffer
                        .getNullTerminatedString()
                        .takeIf(String::isNotEmpty)
                        ?.let(StampShape::fromName)
                        ?: StampShapeA

                cache += Stamp(
                    id = stampId,
                    collectionId = stampCollectionId,
                    imageUri =
                        getStampFile(
                            id = stampId,
                            collectionId = stampCollectionId
                        )
                            .absolutePath
                            .toImageUri(),
                    caption = stampCaption,
                    takenAtLocal = stampTakenAtLocal,
                    shape = stampShape,
                )
            }

            NativeLibrary.freeDirectByteBuffer(detailsBuffer)
        }

        log.debug {
            "initCache(): cache initialized:" +
                    "\nsize=${cache.size}" +
                    "\ntook=${tookMs}ms"
        }

        sharedFlow.emit(cache)
    }

    private external fun getStampDetailsBuffer(
        stampDirectoryPath: String,
    ): ByteBuffer?

    fun getStampFile(
        stamp: Stamp,
    ) =
        getStampFile(
            id = stamp.id,
            collectionId = stamp.collectionId,
        )

    private fun getStampFile(
        id: String,
        collectionId: String,
    ) =
        getStampFileByName(
            fileName = "$id.$WEBP_EXTENSION",
            collectionId = collectionId,
        )

    private fun getStampFileByName(
        fileName: String,
        collectionId: String,
    ) =
        File(
            stampDirectory,
            "$collectionId/$fileName"
        )

    private fun isStamp(file: File): Boolean =
        isStampFile(
            path = file.absolutePath,
        )

    @FastNative
    private external fun isStampFile(path: String): Boolean

    companion object {
        const val WEBP_EXTENSION = "webp"
        const val STAMP_FILE_CONTENT_TYPE = "image/webp"
        private val stampIdCounter = AtomicLong(System.currentTimeMillis())

        fun newStampId(): String =
            stampIdCounter
                .incrementAndFetch()
                .toString()
    }
}

private fun String.toImageUri(): String =
    "file://$this"
