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
