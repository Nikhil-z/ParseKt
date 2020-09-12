package me.lekov.parsekt.types

import io.ktor.http.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import me.lekov.parsekt.api.FindResponse
import me.lekov.parsekt.api.LocalDateTimeQuerySerializer
import me.lekov.parsekt.api.Options
import me.lekov.parsekt.api.ParseApi
import me.lekov.parsekt.types.QueryComparator.*
import org.intellij.lang.annotations.RegExp
import java.time.LocalDateTime
import kotlin.collections.set

@Serializable
data class QueryConstraint(
    val key: String,
    @Contextual val value: Any,
    val comparator: String
)

private enum class QueryComparator(val operator: String) {
    LessThan("\$lt"),
    LessThanOrEqualTo("\$lte"),
    GreaterThan("\$gt"),
    GreaterThanOrEqualTo("\$gte"),
    Equals("\$eq"),
    NotEquals("\$neq"),
    ContainedIn("\$in"),
    NotContainedIn("\$nin"),
    Exists("\$exists"),
    Matches("\$select"),
    NotMatches("\$dontSelect"),
    ContainsAll("\$all"),
    RexEx("\$regex"),
    Text("\$text"),
}

class ParseQuery internal constructor(val query: Builder) {

    suspend inline fun <reified T : ParseObject<T>> find(options: Options = emptySet()): List<T> {
        val className = ParseClasses.valueOf(T::class.simpleName!!).name

        return ParseApi.Command(
            HttpMethod.Get,
            ParseApi.Endpoint.Objects(className),
            body = query,
            mapper = {
                val res = Json {
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                }.decodeFromString<FindResponse<T>>(it)
                res.results ?: emptyList()
            }, serializers = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
            }).execute(options)
    }

    suspend inline fun <reified T : ParseObject<T>> first(options: Options = emptySet()): List<T> {
        val className = ParseClasses.valueOf(T::class.simpleName!!).name

        query.limit = 1

        return ParseApi.Command(
            HttpMethod.Get,
            ParseApi.Endpoint.Objects(className),
            body = query,
            mapper = {
                val res = Json {
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                }.decodeFromString<FindResponse<T>>(it)
                res.results ?: emptyList()
            },serializers = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
            }).execute(options)
    }

    suspend inline fun <reified T : ParseObject<T>> count(options: Options = emptySet()): Int {
        val className = ParseClasses.valueOf(T::class.simpleName!!).name

        query.limit = 0
        query.count = 1

        return ParseApi.Command(
            HttpMethod.Get,
            ParseApi.Endpoint.Objects(className),
            body = query,
            mapper = {
                val res = Json {
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                }.decodeFromString<FindResponse<T>>(it)
                res.count ?: 0
            },serializers = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
            }).execute(options)
    }

    suspend inline fun <reified T : ParseObject<T>> get(
        id: String,
        options: Options = emptySet()
    ): T? {
        val className = ParseClasses.valueOf(T::class.simpleName!!).name

        query.clear()
        query.equalsTo("objectId", id)

        return ParseApi.Command(
            HttpMethod.Get,
            ParseApi.Endpoint.Objects(className),
            body = query,
            mapper = {
                val res = Json {
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                }.decodeFromString<FindResponse<T>>(it)
                res.results?.first()
            },serializers = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
            }).execute(options)
    }

    @Serializable
    class Builder {
        var limit: Int = 100
        var skip: Int = 0
        var count: Int = 0

//        private var keys: Set<String>? = null
//        private var include: Set<String>? = null
//        private var order: List<String>? = null

        @SerialName("where")
        @Serializable(with = ConstraintSerializer::class)
        private val constraints: MutableMap<String, MutableList<QueryConstraint>> = mutableMapOf()

        fun equalsTo(key: String, value: Any) {
            Equals.attachFor(key, value)
        }

        fun notEqualsTo(key: String, value: Any) {
            NotEquals.attachFor(key, value)
        }

        fun lessThan(key: String, value: Any) {
            LessThan.attachFor(key, value)
        }

        fun lessThanOrEqualTo(key: String, value: Any) {
            LessThanOrEqualTo.attachFor(key, value)
        }

        fun greaterThan(key: String, value: Any) {
            GreaterThan.attachFor(key, value)
        }

        fun greaterThanOrEqualTo(key: String, value: Any) {
            GreaterThanOrEqualTo.attachFor(key, value)
        }

        fun containedIt(key: String, value: Any) {
            ContainedIn.attachFor(key, value)
        }

        fun notContainedIn(key: String, value: Any) {
            NotContainedIn.attachFor(key, value)
        }

        fun exists(key: String) {
            Exists.attachFor(key, true)
        }

        fun notExists(key: String) {
            Exists.attachFor(key, false)
        }

        fun matchKey(key: String, value: String) {
            Matches.attachFor(key, value)
        }

        fun notMatchKey(key: String, value: String) {
            NotMatches.attachFor(key, value)
        }

        fun match(key: String, regex: String) {
            RexEx.attachFor(key, regex)
        }

        fun match(key: String, regex: RegExp) {
            RexEx.attachFor(key, regex)
        }

        fun fullText(key: String, value: String) {
            Text.attachFor(key, value)
        }

        fun containsAll(key: String, value: Array<*>) {
            ContainsAll.attachFor(key, value)
        }

        fun clear(key: String? = null) {
            key?.let {
                constraints.remove(key)
            } ?: kotlin.run {
                constraints.clear()
                skip = 0
                limit = 100
                count = 0
            }
        }

        private fun addQueryConstraint(key: String, value: Any, comparator: String) {
            val existing = constraints.getOrDefault(key, mutableListOf())
            existing.add(QueryConstraint(key, value, comparator))
            constraints[key] = existing
        }

        private fun QueryComparator.attachFor(key: String, value: Any) {
            addQueryConstraint(key, value, this.operator)
        }
    }
}

object ConstraintSerializer :
    KSerializer<MutableMap<String, MutableList<QueryConstraint>>> {

    override fun serialize(
        encoder: Encoder,
        value: MutableMap<String, MutableList<QueryConstraint>>
    ) {
        val map =
            value.asIterable().fold(mutableMapOf<String, MutableMap<String, Any>>(), { acc, entry ->
                entry.value.fold(
                    mutableMapOf(),
                    { operations, constraint ->

                        val existing = operations.getOrPut(constraint.key) {
                            mutableMapOf()
                        }

                        existing[constraint.comparator] = constraint.value

                        operations
                    })
            })

        encoder.encodeSerializableValue(
            MapSerializer(
                String.serializer(),
                MapSerializer(String.serializer(), ContextualSerializer(Any::class))
            ), map
        )
    }

    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun deserialize(decoder: Decoder): MutableMap<String, MutableList<QueryConstraint>> {
        TODO("Not yet implemented")
    }
}