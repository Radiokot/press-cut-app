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

package ua.com.radiokot.camerapp.envelopes.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.com.radiokot.camerapp.BuildConfig
import ua.com.radiokot.camerapp.envelopes.domain.CreateEnvelopeUseCase

class EnvelopeContentProvider :
    ContentProvider(),
    KoinComponent {

    private val createEnvelopeUseCase: CreateEnvelopeUseCase by inject()

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {

        val envelopeData = envelopeDataByUri[uri]
        checkNotNull(envelopeData) {
            "Requested URI is not provided"
        }

        val writer = PipeDataWriter<Nothing> { output, _, _, _, _ ->
            // Some apps hammer the method multiple times
            // and don't even read the data which breaks the pipe sometimes.
            // So far I didn't find a way to check if the reader is reading.
            runBlocking {
                runCatching {
                    createEnvelopeUseCase(
                        id = envelopeData.id,
                        message = envelopeData.message,
                        stampIds = envelopeData.stampIds,
                        outputStream =
                            ParcelFileDescriptor
                                .AutoCloseOutputStream(output)
                                .buffered(),
                    )
                }
            }
        }

        return openPipeHelper(
            uri,
            "",
            null,
            null,
            writer,
        )
    }

    override fun getType(
        uri: Uri,
    ): String =
        ENVELOPE_CONTENT_TYPE

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor {
        val envelopeData = envelopeDataByUri[uri]
        checkNotNull(envelopeData) {
            "Requested URI is not provided"
        }

        val cursor = MatrixCursor(
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
            ),
            1
        )
        cursor.addRow(
            arrayOf<Any?>(
                uri.toString().substringAfterLast('/'),

                // Gmail WANTS this!
                // It is actually shown in the attachment section.
                // Doesn't need to be the exact size though.
                envelopeData.stampIds.size * 364000,
            )
        )

        return cursor
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String?>?): Int =
        error("Deletions are not allowed")

    override fun insert(p0: Uri, p1: ContentValues?): Uri =
        error("Inserts are not allowed")

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String?>?): Int =
        error("Updates are not allowed")

    private class EnvelopeData(
        val id: String,
        val message: String?,
        val stampIds: Set<String>,
    )

    companion object {
        const val ENVELOPE_CONTENT_TYPE = "application/octet-stream"
        const val AUTHORITY = BuildConfig.envelopeContentProviderAuthority

        private val envelopeDataByUri = mutableMapOf<Uri, EnvelopeData>()

        fun provideNewEnvelope(
            message: String?,
            stampIds: Set<String>,
        ): Uri {
            val id = (System.currentTimeMillis() / 1000).toString()
            val uri = "content://$AUTHORITY/PressCut-Stamps-$id.onestamp".toUri()

            envelopeDataByUri[uri] = EnvelopeData(
                id = id,
                message = message,
                stampIds = stampIds,
            )

            return uri
        }
    }
}
