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

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.ashampoo.kim.format.webp.WebPImageParser
import com.ashampoo.kim.format.webp.chunk.WebPChunk
import com.ashampoo.kim.input.ByteArrayByteReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ua.com.radiokot.camerapp.util.lazyLogger
import java.io.File

class SafFileLocksmith(
    stampDirectory: File,
    private val stampDirectoryDocumentUri: Uri,
    private val contentResolver: ContentResolver,
) {
    private val log by lazyLogger("SafFileLocksmith")

    private val stampDirectoryDocumentTreeUri: Uri =
        DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            stampDirectoryDocumentUri.toString()
        )
    private val stampDirectoryPath: String =
        stampDirectory.absolutePath

    suspend fun unlockAndReadWebpChunks(
        file: File,
        onlyMetadataChunks: Boolean,
    ): List<WebPChunk> = withContext(Dispatchers.IO) {

        val fileDocumentUri = getFileDocumentUri(file)

        val unlockedFile =
            File(
                file.parent!!,
                "unlocked." + file.name
            )

        val webpChunks: List<WebPChunk>
        try {
            log.debug {
                "unlockAndReadWebpChunks(): copying from the locked file to the unlocked:" +
                        "\nfileDocumentUri=$fileDocumentUri"
            }

            contentResolver
                .openInputStream(fileDocumentUri)!!
                .use { safFileInputStream ->
                    val fileBytes = safFileInputStream.readBytes()
                    unlockedFile.writeBytes(fileBytes)
                    webpChunks = WebPImageParser.readChunks(
                        byteReader = ByteArrayByteReader(fileBytes),
                        stopAfterMetadataRead = onlyMetadataChunks,
                    )
                }
        } catch (e: Exception) {
            ensureActive()

            log.error(e) {
                "unlockAndReadWebpChunks(): failed copying"
            }

            throw e
        }

        log.debug {
            "unlockAndReadWebpChunks(): deleting the locked file:" +
                    "\nfileDocumentUri=$fileDocumentUri"
        }

        check(DocumentsContract.deleteDocument(contentResolver, fileDocumentUri)) {
            log.error {
                "unlockAndReadWebpChunks(): failed deleting the locked file" +
                        "\nfileDocumentUri=$fileDocumentUri"
            }

            "Failed deleting the locked file"
        }

        check(unlockedFile.renameTo(file)) {
            log.error {
                "unlockAndReadWebpChunks(): failed renaming the unlocked file to the original" +
                        "\nunlockedFile=${unlockedFile.absolutePath}" +
                        "\noriginalFile=${file.absolutePath}"
            }

            "Failed renaming the unlocked file to the original"
        }

        return@withContext webpChunks
    }

    suspend fun move(
        lockedSourceFile: File,
        destinationFile: File,
    ): Unit = withContext(Dispatchers.IO) {

        val fileDocumentUri = getFileDocumentUri(lockedSourceFile)

        try {
            log.debug {
                "move(): copying from the locked file to the destination:" +
                        "\nfileDocumentUri=$fileDocumentUri" +
                        "\ndestinationFile=${destinationFile.absolutePath}"
            }

            contentResolver
                .openInputStream(fileDocumentUri)!!
                .use { safFileInputStream ->
                    destinationFile.outputStream().use { destinationOutputStream ->
                        safFileInputStream.copyTo(destinationOutputStream)
                    }
                }
        } catch (e: Exception) {
            ensureActive()

            log.error(e) {
                "unlockAndMove(): failed copying"
            }

            throw e
        }

        log.debug {
            "unlockAndMove(): deleting the locked file:" +
                    "\nfileDocumentUri=$fileDocumentUri"
        }

        check(DocumentsContract.deleteDocument(contentResolver, fileDocumentUri)) {
            log.error {
                "unlockAndMove(): failed deleting the locked file" +
                        "\nfileDocumentUri=$fileDocumentUri"
            }

            "Failed deleting the locked file"
        }
    }

    suspend fun delete(
        file: File,
    ): Unit = withContext(Dispatchers.IO) {

        val fileDocumentUri = getFileDocumentUri(file)

        log.debug {
            "delete(): deleting the file:" +
                    "\nfileDocumentUri=$fileDocumentUri"
        }

        check(DocumentsContract.deleteDocument(contentResolver, fileDocumentUri)) {
            log.error {
                "delete(): failed deleting the file" +
                        "\nfileDocumentUri=$fileDocumentUri"
            }

            "Failed deleting the file"
        }
    }

    private fun getFileDocumentUri(
        file: File,
    ): Uri {
        val filePathRelativeToStampDirectory =
            file.absolutePath.substringAfter(stampDirectoryPath)

        val fileDocumentId =
            stampDirectoryDocumentUri.toString() +
                    filePathRelativeToStampDirectory

        log.debug {
            "getFileDocumentUri(): creating document uri:" +
                    "\nfilePathRelativeToStampDirectory=$filePathRelativeToStampDirectory" +
                    "\nfileDocumentId=$fileDocumentId"
        }

        return DocumentsContract.buildDocumentUriUsingTree(
            stampDirectoryDocumentTreeUri,
            fileDocumentId,
        )
    }
}
