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

        val fileDocumentUri =getFileDocumentUri(file)

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

    suspend fun delete(
        file: File,
    ): Unit = withContext(Dispatchers.IO) {

        val fileDocumentUri = getFileDocumentUri(file)

        log.debug {
            "unlockAndReadWebpChunks(): deleting the file:" +
                    "\nfileDocumentUri=$fileDocumentUri"
        }

        check(DocumentsContract.deleteDocument(contentResolver, fileDocumentUri)) {
            log.error {
                "unlockAndReadWebpChunks(): failed deleting the file" +
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
