package me.lekov.parsekt.types

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import me.lekov.parsekt.api.*
import me.lekov.parsekt.types.QueryComparator.*
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext

@Serializable
data class QueryConstraint(
    val key: String,
    val value: JsonElement,
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
    MatchesQuery("\$inQuery"),
    NotMatchesQuery("\$inQuery"),
    ContainsAll("\$all"),
    RelatedTo("\$relatedTo"),
    RexEx("\$regex"),
    Text("\$text"),
    Search("\$search"),
    Term("\$term"),
    Near("\$nearSphere"),
    MaxDistanceInMiles("\$maxDistanceInMiles"),
    Within("\$within"),
    Box("\$box"),
    GeoWithin("\$geoWithin"),
    Polygon("\$polygon"),
    GeoIntersects("\$geoIntersects"),
    Point("\$point")
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
            Json.encodeToJsonElement(or.map { Json.encodeToJsonElement(QueryConstraintsSerializer, it) }),
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
            Json.encodeToJsonElement(and.map { Json.encodeToJsonElement(QueryConstraintsSerializer, it) }),
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
                polymorphic(Any::class) {
                    subclass(Int::class, Int.serializer())
                    subclass(Boolean::class, Boolean.serializer())
                }
            }).execute(options)
    }

    suspend inline fun <reified T : ParseObject<T>> subscribe(): Flow<List<T>> {
        val className = ParseClasses.valueOf(T::class.simpleName!!).name
        val items = find<T>()

        return ParseLiveQuery.Command(
            ParseApi.Endpoint.Other(className),
            query,
            mapper = {
                val res = Json {
                    encodeDefaults = false
                    ignoreUnknownKeys = true
                }.decodeFromString<LiveQueryResponse<T>>(it)
                res
            },
            SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
                polymorphic(Any::class) {
                    subclass(Int::class, Int.serializer())
                    subclass(Boolean::class, Boolean.serializer())
                }
            },
            items.toMutableList(),
        ).subscribe()
    }

    @Serializable
    class Builder {
        var limit: Int = 100
        var skip: Int = 0
        var count: Int = 0


        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal var keys = mutableSetOf<String>()

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal var exclude = mutableSetOf<String>()

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal var include = mutableSetOf<String>()

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

        fun containedIt(key: String, values: List<String>) {
            ContainedIn.attachFor(key, Json.encodeToJsonElement(values))
        }

        fun notContainedIn(key: String, values: List<String>) {
            NotContainedIn.attachFor(key, Json.encodeToJsonElement(values))
        }

        fun containsAll(key: String, values: List<String>) {
            ContainsAll.attachFor(key, Json.encodeToJsonElement(values))
        }

        fun exists(key: String) {
            Exists.attachFor(key, true)
        }

        fun notExists(key: String) {
            Exists.attachFor(key, false)
        }

        fun matchesKeyInQuery(key: String, queryKey: String, query: ParseQuery) {
            Matches.attachFor(key, buildJsonObject {
                put("key", queryKey)
                put("query", Json.encodeToJsonElement(query.query))
            })
        }

        fun doesNotMatchesKeyInQuery(key: String, queryKey: String, query: ParseQuery) {
            NotMatches.attachFor(key, buildJsonObject {
                put("key", queryKey)
                put("query", Json.encodeToJsonElement(query.query))
            })
        }

        fun matchesQuery(key: String, query: ParseQuery) {
            MatchesQuery.attachFor(key, Json.encodeToJsonElement(query.query))
        }

        fun notMatchesQuery(key: String, query: ParseQuery) {
            NotMatchesQuery.attachFor(key, Json.encodeToJsonElement(query.query))
        }

        fun related(key: String, parent: ParseClass) {
            constraints.add(QueryConstraint(RelatedTo.operator, buildJsonObject {
                put("object", Json.encodeToJsonElement(ParsePointer(parent)))
                put("key", key)
            }, RelatedTo.operator))
        }

        fun match(key: String, regex: String) {
            RexEx.attachFor(key, regex)
        }

        fun contains(key: String, substring: String) {
            val regex = regexStringFor(substring)
            RexEx.attachFor(key, regex)
        }

        fun hasPrefix(key: String, prefix: String) {
            val regex = "^\\(${regexStringFor(prefix)})"
            RexEx.attachFor(key, regex)
        }

        fun hasSuffix(key: String, suffix: String) {
            val regex = "\\(${regexStringFor(suffix)})$"
            RexEx.attachFor(key, regex)
        }

        private fun regexStringFor(inputString: String): String {
            val escapedString = inputString.replace("\\E", "\\E\\\\E\\Q")
            return "\\Q${escapedString}\\E"
        }

        fun fullText(key: String, value: String) {
            Text.attachFor(key, buildJsonObject { put(Search.operator, buildJsonObject { put(Term.operator, value) }) })
        }

        fun near(key: String, value: ParseGeoPoint) {
            Near.attachFor(key, value)
        }

        fun withinMiles(key: String, value: ParseGeoPoint, miles: Double) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, (miles / ParseGeoPoint.earthRadiusMiles))
        }

        fun withinKilometers(key: String, value: ParseGeoPoint, km: Double) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, (km / ParseGeoPoint.earthRadiusKilometers))
        }

        fun withinRadians(key: String, value: ParseGeoPoint, radians: Double) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, radians)
        }

        fun withinGeoBox(key: String, southWest: ParseGeoPoint, northEast: ParseGeoPoint) {
            Within.attachFor(
                key,
                buildJsonObject { put(Box.operator, Json.encodeToJsonElement(arrayListOf(southWest, northEast))) })
        }

        fun withinPolygon(key: String, polygon: List<ParseGeoPoint>) {
            GeoWithin.attachFor(key, buildJsonObject { put(Polygon.operator, Json.encodeToJsonElement(polygon)) })
        }

        fun polygonContains(key: String, point: ParseGeoPoint) {
            GeoIntersects.attachFor(key, buildJsonObject { put(Point.operator, Json.encodeToJsonElement(point)) })
        }

        fun include(vararg include: String) {
            this.include.addAll(include.toList())
        }

        fun includeAll() {
            this.include.clear()
            this.include.add("*")
        }

        fun exclude(vararg include: String) {
            this.exclude.addAll(include.toList())
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

        internal fun addQueryConstraint(key: String, value: JsonElement, comparator: String) {
            constraints.add(QueryConstraint(key, value, comparator))
        }

        private fun QueryComparator.attachFor(key: String, value: Any) {

            val primitiveValue = when (value) {
                is Int -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is LocalDateTime -> Json {
                    serializersModule = SerializersModule {
                        contextual(LocalDateTime::class, LocalDateTimeQuerySerializer)
                    }
                }.encodeToJsonElement(value)
                is JsonObject -> value
                is JsonArray -> value
                is ParsePointer -> Json.encodeToJsonElement(value)
                is ParseGeoPoint -> Json.encodeToJsonElement(value)
                else -> TODO("${value is JsonObject}")
            }

            addQueryConstraint(key, primitiveValue, this.operator)
        }
    }
}

