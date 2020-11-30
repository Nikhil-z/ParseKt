package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.lekov.parsekt.api.AddRelationOperation
import me.lekov.parsekt.api.RemoveRelationOperation

/**
 * Parse relation
 *
 * @property __type
 * @property className
 * @constructor Create empty Parse relation
 */
@Serializable
data class ParseRelation(
    @Required
    private val __type: String = "Relation",
    var className: String? = null,
    @Transient var key: String? = null,
    @Transient var `object`: ParseObject<*>? = null
) {

    /**
     * Query
     *
     * @return
     */
    fun query(): ParseQuery.Builder {
        return ParseObject.query { related(key!!, `object` as ParseObject<Any>) }.query
    }

    /**
     * Add
     *
     * @param T
     * @param element
     */
    fun <T : ParseObject<T>> add(element: T) {
        `object`?.operations?.put(
            key!!,
            AddRelationOperation(
                listOf(
                    ParsePointer(
                        className = element.className,
                        objectId = element.objectId
                    )
                )
            )
        )
    }

    /**
     * Add all
     *
     * @param T
     * @param elements
     */
    fun <T : ParseObject<T>> addAll(elements: List<T>) {
        `object`?.operations?.put(
            key!!,
            AddRelationOperation(elements.map {
                ParsePointer(
                    className = it.className,
                    objectId = it.objectId
                )
            })
        )
    }

    /**
     * Remove
     *
     * @param T
     * @param element
     */
    fun <T : ParseObject<T>> remove(element: T) {
        `object`?.operations?.put(
            key!!,
            RemoveRelationOperation(
                arrayListOf(
                    ParsePointer(
                        className = element.className,
                        objectId = element.objectId
                    )
                )
            )
        )
    }

    /**
     * Remove all
     *
     * @param T
     * @param elements
     */
    fun <T : ParseObject<T>> removeAll(elements: List<T>) {
        `object`?.operations?.put(
            key!!,
            RemoveRelationOperation(elements.map {
                ParsePointer(
                    className = it.className,
                    objectId = it.objectId
                )
            })
        )
    }

}