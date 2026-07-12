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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.stamps.domain.StampCollection
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionRepository
import ua.com.radiokot.camerapp.util.NativeLibrary
import ua.com.radiokot.camerapp.util.getNullTerminatedString
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File
import java.nio.ByteBuffer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.system.measureTimeMillis

class FsStampCollectionRepository(
    private val stampDirectory: File,
    private val safFileLocksmith: SafFileLocksmith,
) : StampCollectionRepository {

    private val log by lazyLogger("FsStampCollectionRepo")

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
    private val cache: MutableList<StampCollection> = mutableListOf()
    private val sharedFlow: MutableSharedFlow<List<StampCollection>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = 10,
        )

    override fun getStampCollectionsFlow(): Flow<List<StampCollection>> = flow {

        if (!isCacheInitialized.exchange(true)) {
            initCache()
        }

        sharedFlow.collect(this)
    }

    override suspend fun getStampCollections(): List<StampCollection> =
        getStampCollectionsFlow()
            .first()

    override suspend fun getStampCollection(
        collectionId: String,
    ): StampCollection? =
        getStampCollections()
            .find { it.id == collectionId }

    override suspend fun addStampCollection(
        id: String,
        name: String,
    ): String = withContext(Dispatchers.IO) {

        createCollectionDetailsFile(
            collectionDirectory =
                getStampCollectionDirectory(
                    id = id,
                ),
            name = name,
        )

        if (isCacheInitialized.load()) {
            cache += StampCollection(
                id = id,
                name = name,
            )
            sharedFlow.tryEmit(cache)
        }

        return@withContext id
    }

    private suspend fun createCollectionDetailsFile(
        collectionDirectory: File,
        name: String,
    ): Unit = withContext(Dispatchers.IO) {

        collectionDirectory.mkdirs()

        saveCollectionDetailsFile(
            outputFile = File(collectionDirectory, DETAILS_FILE_NAME),
            name = name,
        )
    }

    override suspend fun deleteStampCollection(
        collection: StampCollection,
    ): Unit = withContext(Dispatchers.IO) {

        val directory = getStampCollectionDirectory(
            id = collection.id
        )

        if (directory.exists()) {
            log.debug {
                "deleteStampCollection(): deleting the collection directory in background:" +
                        "\ndirectory=$directory"
            }

            coroutineScope.launch {

                val rmCommand = "rm -rf -- ${directory.absolutePath}"
                val exitCode =
                    Runtime
                        .getRuntime()
                        .exec(rmCommand)
                        .waitFor()

                if (exitCode != 0) {
                    log.debug {
                        "deleteStampCollection(): rm didn't finish successfully, trying SAF locksmith" +
                                "\ncommand=$rmCommand"
                    }

                    runCatching {
                        safFileLocksmith.unlockAndDelete(directory)
                    }
                }
            }
        }

        if (isCacheInitialized.load()) {
            cache -= collection
            sharedFlow.tryEmit(cache)
        }
    }

    override suspend fun updateStampCollection(
        collection: StampCollection,
        newName: String?,
    ) = withContext(Dispatchers.IO) {

        val directory = getStampCollectionDirectory(
            id = collection.id
        )
        val nameToSet = newName ?: collection.name

        saveCollectionDetailsFile(
            outputFile = File(directory, DETAILS_FILE_NAME),
            name = nameToSet,
        )

        val updatedCollection = collection.copy(
            newName = nameToSet,
        )

        if (isCacheInitialized.load()) {
            cache[cache.indexOf(collection)] = updatedCollection
            sharedFlow.tryEmit(cache)
        }
    }

    private suspend fun saveCollectionDetailsFile(
        outputFile: File,
        name: String,
    ) = withContext(Dispatchers.IO) {
        if (!saveCollectionDetailsFile(
                filePathString = outputFile.absolutePath,
                nameString = name,
            )
        ) {
            error("Failed saving collection details file")
        }
    }

    private external fun saveCollectionDetailsFile(
        filePathString: String,
        nameString: String,
    ): Boolean

    private suspend fun initCache(): Unit = withContext(Dispatchers.IO) {

        val tookMs = measureTimeMillis {
            stampDirectory
                .listFiles(File::isDirectory)
                ?.mapTo(cache) { toStampCollection(it) }
                ?: error("Can't access the directory: $stampDirectory")
        }

        log.debug {
            "getStampCollectionsFlow(): cache initialized:" +
                    "\nsize=${cache.size}" +
                    "\ntook=${tookMs}ms"
        }

        sharedFlow.emit(cache)
    }

    fun getStampCollectionDirectory(
        id: String,
    ) = File(
        stampDirectory,
        id
    )

    private suspend fun toStampCollection(
        directory: File,
    ): StampCollection {

        val detailsFile = File(directory, DETAILS_FILE_NAME)

        if (!detailsFile.exists()) {
            val id = directory.nameWithoutExtension

            createCollectionDetailsFile(
                collectionDirectory =
                    getStampCollectionDirectory(
                        id = id,
                    ),
                name = id,
            )

            return StampCollection(
                id = directory.nameWithoutExtension,
                name = id,
            )
        }

        val detailsFileBytes =
            if (detailsFile.canRead() && detailsFile.canWrite())
                detailsFile.readBytes()
            else
                safFileLocksmith.unlockAndRead(detailsFile)

        val buffer =
            getCollectionDetailsBuffer(
                webpBytes =
                    ByteBuffer
                        .allocateDirect(detailsFileBytes.size)
                        .put(detailsFileBytes),
            )
                ?: error("Failed reading the stamp details")

        val name =
            buffer
                .getNullTerminatedString()
                .takeIf(String::isNotEmpty)

        NativeLibrary.freeDirectByteBuffer(buffer)

        return StampCollection(
            id = directory.nameWithoutExtension,
            name = name ?: "…",
        )
    }

    private external fun getCollectionDetailsBuffer(
        webpBytes: ByteBuffer,
    ): ByteBuffer?

    private companion object {
        private const val DETAILS_FILE_NAME = ".collection.webp"
    }
}
