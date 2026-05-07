package ua.com.radiokot.camerapp.stamps.ui

import androidx.compose.runtime.Immutable
import ua.com.radiokot.camerapp.stamps.domain.Stamp

@Immutable
data class StampsScreenItem(
    val thumbnailUrl: String,
    val isSelected: Boolean,
    val key: String,
) {
    constructor(
        stamp: Stamp,
        selectedStampIds: Set<String>,
    ) : this(
        thumbnailUrl = stamp.imageUri,
        isSelected = stamp.id in selectedStampIds,
        key = stamp.id,
    )
}
