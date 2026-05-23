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

package ua.com.radiokot.camerapp.stamps.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import ua.com.radiokot.camerapp.stamps.domain.shape.StampShape
import java.time.LocalDateTime
import java.util.Optional

interface StampRepository {

    suspend fun getStamps(): List<Stamp>

    fun getStampsFlow(): Flow<List<Stamp>>

    suspend fun getStamp(
        id: String,
    ): Stamp?

    suspend fun addStamp(
        id: String = System.currentTimeMillis().toString(),
        collectionId: String,
        imageBitmap: Bitmap,
        caption: String?,
        takenAtLocal: LocalDateTime = LocalDateTime.now(),
        shape: StampShape,
    )

    suspend fun updateStamp(
        stamp: Stamp,
        newCaption: Optional<String>?,
    )

    suspend fun deleteStamp(
        stamp: Stamp,
    ) = deleteStamps(
        stampIds = listOf(stamp.id),
        collectionId = stamp.collectionId,
    )

    /**
     * Deletes the stamps in the background
     * while the [flow]([getStampsFlow]) is updated immediately.
     */
    suspend fun deleteStamps(
        collectionId: String,
        stampIds: Collection<String>,
    )

    /**
     * @return progress flow, where each value is
     * the moved file count and the total file count.
     */
    fun moveStampsBetweenCollections(
        sourceCollectionId: String,
        destinationCollectionId: String,
    ): Flow<Pair<Int, Int>>

    /**
     * @return progress flow, where each value is
     * the moved file count and the total file count.
     */
    fun moveStampsBetweenCollections(
        sourceCollectionId: String,
        destinationCollectionId: String,
        stampIds: Collection<String>,
    ): Flow<Pair<Int, Int>>

    suspend fun addGiftStamps(
        collectionId: String,
    )
}
