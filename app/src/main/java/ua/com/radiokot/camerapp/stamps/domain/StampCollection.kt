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

class StampCollection(
    val id: String,
    val name: String,
) {
    /**
     * Primary collection is the default one for the stamps.
     * There's always one primary collection and it can't be deleted.
     */
    val isPrimary: Boolean =
        id == PRIMARY_ID

    fun copy(
        newName: String,
    ) = StampCollection(
        id = id,
        name = newName,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StampCollection) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "StampCollection(id='$id')"
    }

    companion object {
        const val PRIMARY_ID = "0"
    }
}
