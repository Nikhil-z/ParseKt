package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class ParsePointer(
    @Required
    private val __type: String = "Pointer",
    var className: String? = null,
    var objectId: String? = null
) {
    internal constructor(obj: ParseClass) : this(className = obj.className, objectId = obj.objectId)
    internal constructor(obj: ParseUser) : this(className = obj.className, objectId = obj.objectId)
}