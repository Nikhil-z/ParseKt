package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.lekov.parsekt.api.*
import me.lekov.parsekt.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
abstract class ParseObject {

    var objectId: String? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var createdAt: LocalDateTime? = null

    @Serializable(with = LocalDateTimeSerializer::class)
    var updatedAt: LocalDateTime? = null

    var ACL: ACL? = null

    abstract val className: String

    internal val endpoint: ParseApi.Endpoint
        get() = objectId?.let { ParseApi.Endpoint.Object(className, it) }
            ?: ParseApi.Endpoint.Objects(className)

    @Transient
    internal val isSaved = objectId != null

    fun <T : ParseObject> hasSameObjectId(other: T): Boolean {
        return this.className == other.className && this.objectId == other.objectId
    }

    suspend fun save(options: Options = emptySet()): ParseApi.Result<out ParseObject> {
        return ParseApi.saveCommand(this).execute(options)
    }

    suspend fun fetch(options: Options = emptySet()): ParseApi.Result<out ParseObject> {
        return ParseApi.fetchCommand(this).execute(options)
    }
}