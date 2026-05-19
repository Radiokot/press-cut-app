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

import ua.com.radiokot.camerapp.stamps.domain.shape.StampShape
import java.time.LocalDateTime

class Stamp(
    val id: String,
    val collectionId: String,
    val imageUri: String,
    val caption: String?,
    val takenAtLocal: LocalDateTime,
    val shape: StampShape,
) {
    fun copy(
        newCollectionId: String = this.collectionId,
        newImageUri: String = this.imageUri,
        newCaption: String? = this.caption,
    ) = Stamp(
        id = id,
        collectionId = newCollectionId,
        imageUri = newImageUri,
        caption = newCaption,
        takenAtLocal = takenAtLocal,
        shape = shape,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Stamp) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Stamp(id='$id', caption=$caption)"
    }
}
