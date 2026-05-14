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

/**
 * Stores sets of selected stamp IDs.
 */
object StampSelections {
    private val selections = mutableListOf<Set<String>>()

    operator fun get(index: Int): Set<String> =
        selections[index]

    fun add(selection: Set<String>): Int {
        selections.add(selection)
        return selections.indices.last
    }

    operator fun plus(selection: Set<String>) =
        add(selection)
}
