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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import ua.com.radiokot.camerapp.BuildConfig
import ua.com.radiokot.camerapp.stamps.domain.Stamp

class StampFileContentProvider :
    ContentProvider(),
    KoinComponent {

    private val stampRepository: FsStampRepository by inject()

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {

        val stamp = stampsByUri[uri]
        checkNotNull(stamp) {
            "Requested URI is not provided"
        }

        return ParcelFileDescriptor.open(
            stampRepository.getStampFile(stamp),
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
    }

    override fun getType(
        uri: Uri,
    ): String =
        STAMP_FILE_CONTENT_TYPE

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor {

        val stamp = stampsByUri[uri]
        checkNotNull(stamp) {
            "Requested URI is not provided"
        }

        val file = stampRepository.getStampFile(stamp)

        val cursor = MatrixCursor(
            arrayOf(
                MediaStore.MediaColumns.DATA,
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
                MediaStore.MediaColumns.DATE_TAKEN,
            ),
            1
        )
        cursor.addRow(
            arrayOf(
                file.absolutePath,
                file.name,
                file.length(),
                stamp.takenAtLocal.toString(),
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

    companion object {
        const val AUTHORITY = BuildConfig.stampFileContentProviderAuthority
        const val STAMP_FILE_CONTENT_TYPE = "image/webp"

        private val stampsByUri = mutableMapOf<Uri, Stamp>()

        fun provide(
            stamp: Stamp,
        ): Uri {
            val uri = "content://$AUTHORITY/${stamp.id}.${FsStampRepository.WEBP_EXTENSION}".toUri()
            stampsByUri[uri] = stamp
            return uri
        }
    }
}
