package ua.com.radiokot.camerapp.stamps.ui

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import ua.com.radiokot.camerapp.stamps.domain.StampCollectionWithSamples

@Immutable
data class CollectionListItem(
    val name: String,
    val someStamps: ImmutableList<StampSampleItem>,
    val key: String,
) {
    constructor(
        collectionWithSamples: StampCollectionWithSamples,
    ) : this(
        name = collectionWithSamples.collection.name,
        someStamps =
            collectionWithSamples.samples
                .map { stamp ->
                    StampSampleItem(
                        imageUri = stamp.imageUri.toUri(),
                        key = stamp.id,
                    )
                }
                .toPersistentList(),
        key = collectionWithSamples.collection.id,
    )

    @Immutable
    data class StampSampleItem(
        val imageUri: Uri,
        val key: String,
    )
}
