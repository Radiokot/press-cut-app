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

    suspend fun moveStampsBetweenCollections(
        sourceCollectionId: String,
        destinationCollectionId: String,
    )
}
