package ua.com.radiokot.camerapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

@SuppressLint("StaticFieldLeak")
object SAF {
    var uri: Uri? = null
    lateinit var context: Context

    suspend fun unlock(
        file: File,
        destinationFile: File = file,
    ): File = withContext(Dispatchers.IO) {

        val filePathRelativeToStampDirectory =
            file.absolutePath.substringAfter("Pictures/PressCutStamps/")
        val contentResolver = context.contentResolver

        val safFileUri =
            DocumentsContract.buildDocumentUriUsingTree(
                uri,
                "primary:Pictures/PressCutStamps/$filePathRelativeToStampDirectory"
            )

        val tempFile =
            File(
                file.parent!!,
                "tmp." + file.name
            )

        contentResolver.openInputStream(safFileUri)!!.use { safFileInputStream ->
            tempFile.outputStream().use { tempFileOutputStream ->
                safFileInputStream.copyTo(tempFileOutputStream)
            }
        }

        DocumentsContract.deleteDocument(contentResolver, safFileUri)

        Files.move(
            tempFile.toPath(),
            destinationFile.toPath(),

        )
        tempFile.renameTo(destinationFile)

        return@withContext destinationFile
    }
}
