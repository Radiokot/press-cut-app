package ua.com.radiokot.camerapp.stamps.ui

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import ua.com.radiokot.camerapp.stamps.domain.Stamp

@Immutable
data class StampsScreenItem(
    val imageUri: Uri,
    val isSelected: Boolean,
    val key: String,
) {
    constructor(
        stamp: Stamp,
        selectedStampIds: Set<String>,
    ) : this(
        imageUri = stamp.imageUri.toUri(),
        isSelected = stamp.id in selectedStampIds,
        key = stamp.id,
    )
}
