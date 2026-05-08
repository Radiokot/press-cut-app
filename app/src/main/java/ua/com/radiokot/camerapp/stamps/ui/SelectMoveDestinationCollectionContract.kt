package ua.com.radiokot.camerapp.stamps.ui

import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SelectMoveDestinationCollectionContract(
    private val requestor: NavBackStackEntry?,
) {
    fun setSelectedCollectionId(
        collectionId: String,
    ) {
        requestor
            ?.savedStateHandle
            ?.set(
                key = SELECTED_COLLECTION_ID,
                value = collectionId to System.currentTimeMillis(),
            )
    }

    fun getSelectedCollectionIdFlow(): Flow<String> =
        requestor!!
            .savedStateHandle
            .getStateFlow<Pair<String, Long>?>(
                key = SELECTED_COLLECTION_ID,
                initialValue = null,
            )
            .filterNotNull()
            .distinctUntilChanged()
            .map(Pair<String, *>::component1)

    private companion object {
        private const val SELECTED_COLLECTION_ID = "SMDCCCollectionId"
    }
}

