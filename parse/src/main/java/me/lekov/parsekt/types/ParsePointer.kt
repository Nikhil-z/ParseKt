package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * Parse pointer
 *
 * @property __type
 * @property className
 * @property objectId
 * @constructor Create empty Parse pointer
 */
@Serializable
internal data class ParsePointer(
    @Required
    private val __type: String = "Pointer",
    var className: String? = null,
    var objectId: String? = null
) {
    internal constructor(obj: ParseClass) : this(className = obj.className, objectId = obj.objectId)
    internal constructor(obj: ParseUser) : this(className = obj.className, objectId = obj.objectId)
}