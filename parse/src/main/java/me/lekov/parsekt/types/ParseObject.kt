package me.lekov.parsekt.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import me.lekov.parsekt.api.*
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Parse class companion
 *
 * @constructor Create empty Parse class companion
 */
open class ParseClassCompanion {
    fun query(builder: ParseQuery.Builder.() -> Unit): ParseQuery {
        return ParseQuery(ParseQuery.Builder().apply(builder))
    }

    val json
        get() = Json(from = Json) {
            encodeDefaults = false
            ignoreUnknownKeys = true
        }
}

/**
 * Parse object
 *
 * @param T
 * @constructor Create empty Parse object
 */
@Serializable
open class ParseObject<T> {

    internal var operations = mutableMapOf<String, Operation>()
    val className: String get() = ParseClasses.valueOf(this::class.simpleName!!).parseName

    var objectId: String? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var createdAt: LocalDateTime? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var updatedAt: LocalDateTime? = null


    @Serializable(with = ACLSerializer::class)
    var ACL: ACL? = null

    @PublishedApi
    internal val endpoint: ParseApi.Endpoint
        get() = objectId?.let { ParseApi.Endpoint.Object(className, it) }
            ?: ParseApi.Endpoint.Objects(className)

    @Transient
    @PublishedApi
    internal val isSaved = objectId != null

    /**
     * Has same object id
     *
     * @param T
     * @param other
     * @return
     */
    inline fun <reified T : ParseObject<T>> hasSameObjectId(other: T): Boolean {
        return this.className == other.className && this.objectId == other.objectId
    }

    fun delete(key: String) {
        this.operations[key] = DeleteOperation()
    }

    fun increment(key: String, amount: Int) {
        this.operations[key] = IncrementOperation(amount)
    }

    fun decrement(key: String, amount: Int) {
        this.operations[key] = IncrementOperation(-abs(amount))
    }

    override fun toString(): String {
        return "ParseObject(objectId=$objectId, createdAt=$createdAt, updatedAt=$updatedAt, ACL=$ACL, className='$className')"
    }

    companion object : ParseClassCompanion()
}

/**
 * Parse class
 *
 * @constructor Create empty Parse class
 */
@Serializable
open class ParseClass : ParseObject<ParseClass>()