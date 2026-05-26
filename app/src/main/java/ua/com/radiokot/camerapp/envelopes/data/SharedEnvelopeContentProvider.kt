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
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.com.radiokot.camerapp.stamps.data.FsStampRepository

class SharedEnvelopeContentProvider :
    ContentProvider(),
    KoinComponent {

    private val fsStampRepository: FsStampRepository by inject()

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {

        val writer = PipeDataWriter<Nothing> { output, _, _, _, _ ->
            runBlocking {
                ParcelFileDescriptor.AutoCloseOutputStream(output).use { outputStream ->

                    CreateOneStampEnvelopeUseCase(fsStampRepository)
                        .invoke(
                            message = "From Oleg!",
                            stampIds = setOf(
                                "1779703713622",
                                "1779722012660",
                                "1779722012663",
//                                "1779827805776",
                            ),
                            outputStream = outputStream,
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

    override fun getType(uri: Uri): String =
        "application/octet-stream"

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor? {
        // TODO return display name.
        return null
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String?>?): Int =
        error("Deletions are not allowed")

    override fun insert(p0: Uri, p1: ContentValues?): Uri =
        error("Inserts are not allowed")

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String?>?): Int =
        error("Updates are not allowed")
}
