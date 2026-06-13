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
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import ua.com.radiokot.camerapp.BuildConfig
import ua.com.radiokot.camerapp.stamps.domain.CreateStampPosterUseCase
import ua.com.radiokot.camerapp.stamps.domain.Stamp
import ua.com.radiokot.camerapp.stamps.domain.StampPosterOptions
import ua.com.radiokot.camerapp.util.MatrixCursor
import ua.com.radiokot.camerapp.util.openPipeHelper

class StampPosterContentProvider :
    ContentProvider(),
    KoinComponent {

    private val createStampPosterUseCase: CreateStampPosterUseCase by inject()

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {

        val (stamp, posterOptions) =
            stampsAndOptionsByUri[uri]
                ?: error("Requested URI is not provided")

        return openPipeHelper { outputStream ->
            val bitmap =
                createStampPosterUseCase(
                    stamp = stamp,
                    options = posterOptions,
                )
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                outputStream,
            )
            bitmap.recycle()
        }
    }

    override fun getType(
        uri: Uri,
    ): String =
        POSTER_FILE_CONTENT_TYPE

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor {

        val (stamp, _) =
            stampsAndOptionsByUri[uri]
                ?: error("Requested URI is not provided")

        return MatrixCursor(
            valuesByColumnName = mapOf(
                OpenableColumns.DISPLAY_NAME to getPosterFileName(
                    stampId = stamp.id,
                ),
                // Gmail WANTS this!
                // It is actually shown in the attachment section.
                // Doesn't need to be the exact size though.
                OpenableColumns.SIZE to 550 * 1024,
            ),
        )
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String?>?): Int =
        error("Deletions are not allowed")

    override fun insert(p0: Uri, p1: ContentValues?): Uri =
        error("Inserts are not allowed")

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String?>?): Int =
        error("Updates are not allowed")

    companion object {
        const val AUTHORITY = BuildConfig.stampPosterContentProviderAuthority
        const val POSTER_FILE_CONTENT_TYPE = "image/png"
        private const val PNG_EXTENSION = "png"

        private val stampsAndOptionsByUri = mutableMapOf<Uri, Pair<Stamp, StampPosterOptions>>()

        fun provide(
            stamp: Stamp,
            posterOptions: StampPosterOptions,
        ): Uri {
            val uri = "content://$AUTHORITY/${getPosterFileName(stamp.id)}".toUri()
            stampsAndOptionsByUri[uri] = stamp to posterOptions
            return uri
        }

        private fun getPosterFileName(
            stampId: String,
        ): String =
            "${stampId}.$PNG_EXTENSION"
    }
}
