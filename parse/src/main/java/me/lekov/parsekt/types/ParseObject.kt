package me.lekov.parsekt.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import me.lekov.parsekt.api.LocalDateTimeSerializer
import me.lekov.parsekt.api.ParseApi
import java.time.LocalDateTime

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

@Serializable
open class ParseObject<T> {

    val className: String get() = ParseClasses.valueOf(this::class.simpleName!!).name
    
    var objectId: String? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var createdAt: LocalDateTime? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var updatedAt: LocalDateTime? = null

    @Serializable(with = ACL.ACLSerializer::class)
    var ACL: ACL? = null

    @PublishedApi
    internal val endpoint: ParseApi.Endpoint
        get() = objectId?.let { ParseApi.Endpoint.Object(className, it) }
            ?: ParseApi.Endpoint.Objects(className)

    @Transient
    @PublishedApi
    internal val isSaved = objectId != null

    inline fun <reified T : ParseObject<T>> hasSameObjectId(other: T): Boolean {
        return this.className == other.className && this.objectId == other.objectId
    }

    override fun toString(): String {
        return "ParseObject(objectId=$objectId, createdAt=$createdAt, updatedAt=$updatedAt, ACL=$ACL, className='$className')"
    }

    companion object : ParseClassCompanion()
}

@Serializable
open class ParseClass : ParseObject<ParseClass>()