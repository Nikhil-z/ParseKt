package me.lekov.parsekt.api

import kotlinx.serialization.Serializable
import me.lekov.parsekt.serializers.LocalDateTimeSerializer
import me.lekov.parsekt.types.ParseObject
import java.time.LocalDateTime

interface IApplicable {
    fun <T : ParseObject> apply(toItem: T): T
}

@Serializable
internal data class SaveResponse(
    val objectId: String,
    val createdAt: @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime,
    val updatedAt: @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime? = null,
) : IApplicable {
    override fun <T : ParseObject> apply(toItem: T): T {
        return toItem.also {
            it.objectId = objectId
            it.createdAt = createdAt
            it.updatedAt = updatedAt?.let { it } ?: createdAt
        }
    }
}

@Serializable
internal data class UpdateResponse(
    val updatedAt: @Serializable(with = LocalDateTimeSerializer::class) LocalDateTime,
) : IApplicable {
    override fun <T : ParseObject> apply(toItem: T): T {
        return toItem.also {
            it.updatedAt = updatedAt
        }
    }
}

@Serializable
internal class FetchResponse : IApplicable {
    override fun <T : ParseObject> apply(toItem: T): T {
        return toItem
    }
}
