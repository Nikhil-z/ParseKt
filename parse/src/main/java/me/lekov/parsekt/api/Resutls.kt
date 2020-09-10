package me.lekov.parsekt.api

import me.lekov.parsekt.types.ParseObject

internal data class FindResult<T: ParseObject>(val results: Array<T>, val count: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FindResult<*>

        if (!results.contentEquals(other.results)) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = results.contentHashCode()
        result = 31 * result + count
        return result
    }
}