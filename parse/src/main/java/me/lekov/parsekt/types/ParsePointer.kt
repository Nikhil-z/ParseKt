package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import me.lekov.parsekt.api.ParsePointerSerializer

/**
 * Parse pointer
 *
 * @property __type
 * @property className
 * @property objectId
 * @constructor Create empty Parse pointer
 */
@Serializable(with = ParsePointerSerializer::class)
data class ParsePointer(
    @Required
    private val __type: String = "Pointer",
    internal var className: String? = null,
    @PublishedApi internal var objectId: String? = null,
    @PublishedApi @Transient internal var `object`: JsonObject? = null
) {
    internal constructor(obj: ParseObject<*>) : this(className = obj.className, objectId = obj.objectId)
    internal constructor(obj: ParseUser) : this(className = obj.className, objectId = obj.objectId)

    val available
        get() = `object` != null

    /**
     * Try to get pointer object
     *
     * By default all pointers are returned as references unless the query specified it
     * as included. In such case the pointer is expanded to object.
     *
     * @param T Subtype of [ParseClass] the result to be encoded to
     * @return Returns [T] when data is available, null otherwise
     */
    inline fun <reified T : ParseObject<T>> get(): T? {
        return if (available) Json {
            ignoreUnknownKeys = true
        }.decodeFromJsonElement(`object`!!) else null
    }

    /**
     * Fetch if not available
     *
     * If this pointer is not expanded to object, it try to resolve it by
     * getting it from the Parse Server
     *
     * @param T Subtype of [ParseClass] the result to be encoded to
     * @return  Returns [T] when data is available, null otherwise
     */
    suspend inline fun <reified T : ParseObject<T>> fetch(): T? {
        return get<T>() ?: ParseObject.query { }.get<T>(objectId!!)
    }
}