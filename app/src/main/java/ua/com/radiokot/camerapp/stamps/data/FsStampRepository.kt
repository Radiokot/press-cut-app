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

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.text.isDigitsOnly
import com.ashampoo.kim.format.webp.WebPImageParser
import com.ashampoo.kim.format.webp.WebPWriter
import com.ashampoo.kim.input.AndroidInputStreamByteReader
import com.ashampoo.kim.input.ByteArrayByteReader
import com.ashampoo.kim.input.use
import com.ashampoo.kim.output.OutputStreamByteWriter
import com.ashampoo.xmp.XMPMeta
import com.ashampoo.xmp.XMPMetaFactory
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.stamps.data.PressCutXmpNamespace.getStampShape
import ua.com.radiokot.camerapp.stamps.data.PressCutXmpNamespace.setStampShape
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampRepository
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShape
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShapeA
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.absolutePathString
import kotlin.jvm.optionals.getOrNull
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

class FsStampRepository(
    private val stampDirectory: File,
    private val assetManager: AssetManager,
    private val giftStampsAssetsDirectoryName: String,
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

    override suspend fun getStamps(): PersistentList<Stamp> = withContext(Dispatchers.IO) {

        val fileSequence =
            stampDirectory
                .listFiles(File::isDirectory)
                ?.asSequence()
                ?.flatMap { collectionDirectory ->
                    collectionDirectory
                        .listFiles(::isStamp)
                        ?.asSequence()
                        ?: error("Can't access the directory: $collectionDirectory")
                }
                ?: error("Can't access the directory: $stampDirectory")

        coroutineScope {
            fileSequence
                .chunked(40)
                .mapTo(mutableListOf()) { chunk ->
                    async { chunk.map(File::toStamp) }
                }
                .awaitAll()
                .flatten()
                .toPersistentList()
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
            val tookMs = measureTimeMillis {
                cache += getStamps()
            }
            log.debug {
                "getStampsFlow(): cache initialized:" +
                        "\nsize=${cache.size}" +
                        "\ntook=${tookMs.milliseconds}"
            }
            sharedFlow.emit(cache)
        }

        sharedFlow.collect(this)
    }

    override suspend fun getStamp(
        id: String,
    ): Stamp? =
        getStampsFlow()
            .first()
            .find { it.id == id }

    override suspend fun addStamp(
        collectionId: String,
        imageBitmap: Bitmap,
        shape: StampShape,
        caption: String?,
    ): Unit = withContext(Dispatchers.IO) {

        val id = System.currentTimeMillis().toString()
        val takenAtLocal = LocalDateTime.now()

        val outputFile = getStampFile(
            id = id,
            collectionId = collectionId,
        )

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
                imageUri = outputFile.toPath().toImageUri(),
                takenAtLocal = takenAtLocal,
                shape = shape,
            )
            sharedFlow.emit(cache)
        }
    }

    override suspend fun updateStamp(
        stamp: Stamp,
        newCaption: Optional<String>?,
    ) = withContext(Dispatchers.IO) {

        val file = getStampFile(
            id = stamp.id,
            collectionId = stamp.collectionId,
        )
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
        val movedStampPathsById = ConcurrentHashMap<String, Path>(mutableMapOf())

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
                    movedStampPathsById[stampId] = stampDestinationPath
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

    override suspend fun addGiftStamps(
        collectionId: String,
    ): Unit = withContext(Dispatchers.IO) {

        val collectionDirectoryPath =
            FileSystems
                .getDefault()
                .getPath(
                    stampDirectory.absolutePath,
                    collectionId,
                )
                .toString()

        var id = System.currentTimeMillis()

        val addedFiles: List<File> =
            assetManager
                .list(giftStampsAssetsDirectoryName)
                ?.map { giftStampFileName ->

                    // Avoid ID conflicts with existing stamps.
                    val destinationStampFile = File(
                        collectionDirectoryPath,
                        "${id++}.${giftStampFileName.substringAfterLast('.', "")}"
                    )

                    assetManager
                        .open("$giftStampsAssetsDirectoryName/$giftStampFileName")
                        .use { giftStampFileInputStream ->
                            destinationStampFile
                                .outputStream()
                                .use { destinationStampFileOutputStream ->
                                    giftStampFileInputStream.copyTo(destinationStampFileOutputStream)
                                }
                        }

                    destinationStampFile
                }
                ?: emptyList()

        if (isCacheInitialized.load()) {
            cache += addedFiles
                .map(File::toStamp)
            sharedFlow.emit(cache)
        }
    }

    private fun getStampFile(
        id: String,
        collectionId: String,
    ) = File(
        stampDirectory,
        "$collectionId/$id.$EXTENSION_WEBP"
    )

    private fun isStamp(file: File): Boolean =
        file.extension in EXTENSIONS
                && file.nameWithoutExtension.isDigitsOnly()

    private companion object {
        private const val EXTENSION_WEBP = "webp"
        private val EXTENSIONS = setOf(
            EXTENSION_WEBP,
        )
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

private fun File.toStamp(): Stamp {
    val path = toPath()
    val xmpMeta: XMPMeta? =
        AndroidInputStreamByteReader(
            inputStream = inputStream(),
            contentLength = length(),
        )
            .use(WebPImageParser::parseMetadata)
            .xmp
            ?.let(XMPMetaFactory::parseFromString)
    val parent = parentFile!!

    return Stamp(
        id = nameWithoutExtension,
        collectionId = parent.name,
        imageUri = path.toImageUri(),
        caption = xmpMeta?.getTitle(),
        takenAtLocal =
            xmpMeta
                ?.getDateTimeOriginal()
                ?.let(LocalDateTime::parse)
                ?: Files
                    .readAttributes(path, BasicFileAttributes::class.java)
                    .creationTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime(),
        shape =
            xmpMeta
                ?.getStampShape()
                ?.let(StampShape::fromName)
                ?: StampShapeA,
    )
}

private fun Path.toImageUri(): String =
    "file://${absolutePathString()}"
