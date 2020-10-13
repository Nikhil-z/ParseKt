package me.lekov.parsekt.types

import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import me.lekov.parsekt.api.*
import me.lekov.parsekt.types.QueryComparator.*
import org.intellij.lang.annotations.RegExp
import java.time.LocalDateTime

data class QueryConstraint(
    val key: String,
    val value: Any,
    val comparator: String
)

internal enum class QueryComparator(val operator: String) {
    Or("\$or"),
    And("\$and"),
    Not("\$not"),
    Nor("\$nor"),
    LessThan("\$lt"),
    LessThanOrEqualTo("\$lte"),
    GreaterThan("\$gt"),
    GreaterThanOrEqualTo("\$gte"),
    Equals("\$eq"),
    NotEquals("\$ne"),
    ContainedIn("\$in"),
    NotContainedIn("\$nin"),
    ContainedBy("\$containedBy"),
    Exists("\$exists"),
    Matches("\$select"),
    NotMatches("\$dontSelect"),
    ContainsAll("\$all"),
    RexEx("\$regex"),
    Text("\$text"),
    Near("\$nearSphere"),
    MaxDistanceInMiles("\$maxDistanceInMiles"),
}

class ParseQuery internal constructor(@PublishedApi internal val query: Builder) {

    private val or = mutableListOf<MutableList<QueryConstraint>>()
    private val and = mutableListOf<MutableList<QueryConstraint>>()

    fun or(vararg value: ParseQuery): ParseQuery {
        or.clear()
        or.add(query.constraints.toMutableList())
        or.addAll(value.map { it.query.constraints })

        query.clear()
        query.addQueryConstraint(
            Or.operator,
            or.map { Json.encodeToJsonElement(QueryConstraintsSerializer, it) },
            Or.operator
        )
        return this
    }

    fun and(vararg value: ParseQuery): ParseQuery {
        and.clear()
        and.add(query.constraints.toMutableList())
        and.addAll(value.map { it.query.constraints })

        query.clear()
        query.addQueryConstraint(
            And.operator,
            and.map { Json.encodeToJsonElement(QueryConstraintsSerializer, it) },
            And.operator
        )

        return this
    }

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
            }, serializers = SerializersModule {
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
            }, serializers = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
            }).execute(options)
    }

    suspend inline fun <reified T : ParseObject<T>> get(id: String, options: Options = emptySet()): T? {
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
            }, serializers = SerializersModule {
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

        @PublishedApi
        @SerialName("where")
        @Serializable(with = QueryConstraintsSerializer::class)
        internal val constraints: MutableList<@Contextual QueryConstraint> = mutableListOf()

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

        fun near(key: String, value: ParseGeoPoint) {
            Near.attachFor(key, value)
        }

        fun withinMiles(key: String, value: ParseGeoPoint, miles: Number) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, value)
        }

        fun clear(key: String? = null) {
            key?.let {
                constraints.removeIf { it.key == key }
            } ?: kotlin.run {
                constraints.clear()
                skip = 0
                limit = 100
                count = 0
            }
        }

        internal fun addQueryConstraint(key: String, value: Any, comparator: String) {
            constraints.add(QueryConstraint(key, value, comparator))
        }

        private fun QueryComparator.attachFor(key: String, value: Any) {
            addQueryConstraint(key, value, this.operator)
        }
    }
}

