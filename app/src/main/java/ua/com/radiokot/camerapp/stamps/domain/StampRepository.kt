package ua.com.radiokot.camerapp.stamps.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import java.util.Optional

interface StampRepository {

    suspend fun getStamps(): List<Stamp>

    fun getStampsFlow(): Flow<List<Stamp>>

    suspend fun getStamp(
        id: String,
    ): Stamp?

    suspend fun addStamp(
        collectionId: String,
        imageBitmap: Bitmap,
        caption: String?,
    )

    suspend fun updateStamp(
        stamp: Stamp,
        newCaption: Optional<String>?,
    )

    suspend fun deleteStamp(
        stamp: Stamp,
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
}
