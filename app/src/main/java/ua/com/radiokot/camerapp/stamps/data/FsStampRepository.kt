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
import com.ashampoo.kim.format.webp.WebPImageParser
import com.ashampoo.kim.format.webp.WebPWriter
import com.ashampoo.kim.input.AndroidInputStreamByteReader
import com.ashampoo.kim.input.ByteArrayByteReader
import com.ashampoo.kim.input.use
import com.ashampoo.kim.model.ImageSize
import com.ashampoo.kim.output.ByteArrayByteWriter
import com.ashampoo.kim.output.OutputStreamByteWriter
import com.ashampoo.xmp.XMPMeta
import com.ashampoo.xmp.XMPMetaFactory
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.stamps.data.PressCutXmpNamespace.setStampShape
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.util.getNullTerminatedString
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
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
) : StampRepository {

    private val log by lazyLogger("FsStampRepo")

    private val coroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("FsStampCollectionRepo"))

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
    ): Pair<ByteArray, ImageSize> = withContext(Dispatchers.IO) {

        val file = getStampFile(stamp)

        val webpChunks =
            AndroidInputStreamByteReader(
                inputStream = file.inputStream(),
                contentLength = file.length(),
            )
                .use(WebPImageParser::readChunks)

        val size =
            WebPImageParser
                .parseMetadataFromChunks(webpChunks)
                .imageSize!!

        val bytes =
            ByteArrayByteWriter().use { writer ->
                WebPWriter.writeImage(
                    chunks = webpChunks,
                    byteWriter = writer,
                    exifBytes = null,
                    xmp = null,
                )

                writer.flush()
                writer.toByteArray()
            }

        return@withContext Pair(bytes, size)
    }

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

        val xmpMeta = XMPMetaFactory.create().setStampDetails(
            caption = caption,
            takenAtLocal = takenAtLocal,
            shape = shape,
        )

        WebPWriter.writeImage(
            byteReader = ByteArrayByteReader(webpBytes),
            byteWriter = OutputStreamByteWriter(
                FileOutputStream(outputFile)
            ),
            xmp = XMPMetaFactory.serializeToString(xmpMeta),
            exifBytes = null,
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

        val webpChunks =
            if (file.canRead() && file.canWrite())
                AndroidInputStreamByteReader(
                    inputStream = file.inputStream(),
                    contentLength = file.length(),
                )
                    .use(WebPImageParser::readChunks)
            else
                safFileLocksmith
                    .unlockAndReadWebpChunks(
                        file = file,
                        onlyMetadataChunks = false,
                    )

        val xmpMeta =
            WebPImageParser
                .parseMetadataFromChunks(webpChunks)
                .xmp
                ?.let(XMPMetaFactory::parseFromString)
                ?: XMPMetaFactory.create()
        xmpMeta.setStampDetails(
            caption = captionToSet,
            takenAtLocal = stamp.takenAtLocal,
            shape = stamp.shape,
        )

        WebPWriter
            .writeImage(
                chunks = webpChunks,
                byteWriter = OutputStreamByteWriter(
                    FileOutputStream(file)
                ),
                xmp = XMPMetaFactory.serializeToString(xmpMeta),
                exifBytes = null,
            )

        val updatedStamp = stamp.copy(
            newCaption = captionToSet,
        )

        if (isCacheInitialized.load()) {
            cache[cache.indexOf(stamp)] = updatedStamp
            sharedFlow.emit(cache)
        }
    }

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

            stampIds.forEach { stampId ->
                launch {
                    val file = getStampFile(
                        id = stampId,
                        collectionId = collectionId,
                    )

                    if (file.exists()) {
                        if (file.canWrite()) {
                            file.delete()
                        } else {
                            safFileLocksmith.delete(file)
                        }
                    }
                }
            }
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

        //                        Hi Revolut 👋
        val movedStampPathsById = ConcurrentHashMap<String, String>(mutableMapOf())

        return channelFlow {
            val progressChannel = this.channel

            progressChannel.send(0 to stampIds.size)

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
                    val stampDestinationFile =
                        getStampFile(
                            id = stampId,
                            collectionId = destinationCollectionId,
                        )
                    val stampDestinationPath =
                        FileSystems.getDefault().getPath(stampDestinationFile.path)

                    if (stampSourceFile.canWrite()) {
                        Files.move(
                            FileSystems.getDefault().getPath(stampSourceFile.path),
                            stampDestinationPath,
                            StandardCopyOption.ATOMIC_MOVE,
                        )
                    } else {
                        safFileLocksmith.move(
                            lockedSourceFile = stampSourceFile,
                            destinationFile = stampDestinationFile,
                        )
                    }
                    movedStampPathsById[stampId] = stampDestinationPath.absolutePathString()
                    progressChannel.send(movedStampPathsById.size to stampIds.size)
                }
            }
        }
            .onCompletion {
                if (isCacheInitialized.load()) {
                    cache.indices.forEach { i ->
                        val stamp = cache[i]
                        if (movedStampPathsById.containsKey(stamp.id)) {
                            cache[i] = stamp.copy(
                                newCollectionId = destinationCollectionId,
                                newImageUri =
                                    movedStampPathsById
                                        .getValue(stamp.id)
                                        .toImageUri(),
                            )
                        }
                    }
                    sharedFlow.emit(cache)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun initCache() = withContext(Dispatchers.IO) {
        val stampDirectoryAbsolutePath = stampDirectory.absolutePath

        val tookMs = measureTimeMillis {
            val buffer =
                getStampDetailsBuffer(
                    stampDirectoryPath = stampDirectoryAbsolutePath,
                )
                    ?: error("Failed reading the stamp details")

            while (buffer.hasRemaining()) {
                val stampId = buffer.getNullTerminatedString()
                val stampCollectionId = buffer.getNullTerminatedString()

                val stampCaption =
                    buffer
                        .getNullTerminatedString()
                        .takeIf(String::isNotEmpty)

                val stampTakenAtLocal =
                    LocalDateTime
                        .parse(buffer.getNullTerminatedString())

                val stampShape =
                    buffer
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
        private val stampIdCounter = AtomicLong(System.currentTimeMillis())

        fun newStampId(): String =
            stampIdCounter
                .incrementAndFetch()
                .toString()
    }
}

private fun XMPMeta.setStampDetails(
    caption: String?,
    takenAtLocal: LocalDateTime,
    shape: StampShape,
) = apply {
    setTitle(caption)
    setDateTimeOriginal(takenAtLocal.toString())
    if (shape != StampShapeA) {
        setStampShape(shape.name)
    }
}

private fun String.toImageUri(): String =
    "file://$this"
