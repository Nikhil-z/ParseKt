package me.lekov.parsekt.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import me.lekov.parsekt.api.Options
import me.lekov.parsekt.api.ParseApi
import me.lekov.parsekt.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

open class ParseObjectCompanion {
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
open class ParseObject {

    var objectId: String? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var createdAt: LocalDateTime? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var updatedAt: LocalDateTime? = null

    @Serializable(with = ACL.ACLSerializer::class)
    var ACL: ACL? = null

    @Transient
    val className: String = ParseClasses.valueOf(this::class.simpleName!!).name

    internal val endpoint: ParseApi.Endpoint
        get() = objectId?.let { ParseApi.Endpoint.Object(className, it) }
            ?: ParseApi.Endpoint.Objects(className)

    @Transient
    internal val isSaved = objectId != null

    fun hasSameObjectId(other: ParseObject): Boolean {
        return this.className == other.className && this.objectId == other.objectId
    }

    suspend fun save(options: Options = emptySet()): ParseObject {
        return ParseApi.saveCommand(this).execute(options)
    }

    suspend fun fetch(options: Options = emptySet()): ParseObject {
        return ParseApi.fetchCommand(this).execute(options)
    }

    override fun toString(): String {
        return "ParseObject(objectId=$objectId, createdAt=$createdAt, updatedAt=$updatedAt, ACL=$ACL, className='$className')"
    }

    companion object : ParseObjectCompanion()
}