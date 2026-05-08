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
