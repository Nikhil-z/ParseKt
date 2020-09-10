package me.lekov.parsekt.types

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.lekov.parsekt.api.*
import me.lekov.parsekt.api.FetchResponse
import me.lekov.parsekt.api.SaveResponse
import me.lekov.parsekt.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlin.reflect.KProperty

interface IDefineable {
    val className: String
}

@Serializable
open class ParseObject {
    @Required
    var objectId: String? = null

    @Required
    @Serializable(with = LocalDateTimeSerializer::class)
    var createdAt: LocalDateTime? = null

    @Required
    @Serializable(with = LocalDateTimeSerializer::class)
    var updatedAt: LocalDateTime? = null

    var ACL: ACL? = null

    @Transient
    internal val _className: String
        get() = className

    @Transient
    internal val endpoint: ParseApi.Endpoint
        get() = objectId?.let { ParseApi.Endpoint.Object(_className, it) }
            ?: ParseApi.Endpoint.Objects(_className)

    @Transient
    internal val isSaved = objectId != null

    fun <T : ParseObject> hasSameObjectId(other: T): Boolean {
        return this._className == other._className && this.objectId == other.objectId
    }

    suspend fun save(options: Options = emptySet()): ParseApi.Result<out ParseObject> {
        return ParseApi.saveCommand(this).execute(options)
    }

    suspend fun fetch(options: Options = emptySet()): ParseApi.Result<out ParseObject> {
        return ParseApi.fetchCommand(this).execute(options)
    }

    companion object: IDefineable {
        override val className: String by ClassNameDelegate()
    }
}


class ClassNameDelegate<T> {
    operator fun getValue(thisRef: T, property: KProperty<*>): String {
        return thisRef!!::class.java.enclosingClass.simpleName
    }
}