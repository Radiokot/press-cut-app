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

package ua.com.radiokot.camerapp.util

import android.content.ContentProvider
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.runBlocking

/**
 * Convenient wrapper for [ContentProvider.openPipeHelper].
 * - Doesn't require a bunch of arguments;
 * - Supports suspended calls;
 * - Is tolerant to unexpected pipe closures.
 */
fun ContentProvider.openPipeHelper(
    writer: suspend (outputStream: ParcelFileDescriptor.AutoCloseOutputStream) -> Unit,
): ParcelFileDescriptor =
    openPipeHelper(
        Uri.EMPTY,
        "",
        null,
        null,
    ) { outputFileDescriptor, _, _, _, _ ->
        // Some apps hammer the method multiple times
        // and don't even read the data which breaks the pipe sometimes.
        // So far I didn't find a way to check if the reader is reading.
        runBlocking {
            runCatching {
                writer(
                    ParcelFileDescriptor.AutoCloseOutputStream(
                        outputFileDescriptor
                    )
                )
            }
        }
    }

/**
 * @return a single row cursor.
 */
fun MatrixCursor(
    valuesByColumnName: Map<String, Any>,
): MatrixCursor {

    val columnNames = Array(valuesByColumnName.size) { "" }
    val columnValues = Array<Any>(valuesByColumnName.size) { }

    valuesByColumnName.entries.forEachIndexed { i, (columnName, columnValue) ->
        columnNames[i] = columnName
        columnValues[i] = columnValue
    }

    val cursor = MatrixCursor(
        columnNames,
        1
    )
    cursor.addRow(columnValues)

    return cursor
}
