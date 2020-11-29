package me.lekov.parsekt.types

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

/**
 * Query constraint
 *
 * @property key
 * @property value
 * @property comparator
 * @constructor Create empty Query constraint
 */
@Serializable
data class QueryConstraint(
    val key: String,
    val value: JsonElement,
    val comparator: String
)

/**
 * Query comparator
 *
 * @property operator
 * @constructor Create empty Query comparator
 */
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

/**
 * Parse query
 *
 * @property query
 * @constructor Create empty Parse query
 */
class ParseQuery internal constructor(@PublishedApi internal val query: Builder) {

    private val or = mutableListOf<MutableList<QueryConstraint>>()
    private val and = mutableListOf<MutableList<QueryConstraint>>()

    /**
     * Or
     *
     * @param value
     * @return
     */
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

    /**
     * And
     *
     * @param value
     * @return
     */
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

    /**
     * Find
     *
     * @param T
     * @param options
     * @return
     */
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

    /**
     * First
     *
     * @param T
     * @param options
     * @return
     */
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

    /**
     * Count
     *
     * @param T
     * @param options
     * @return
     */
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

    /**
     * Get
     *
     * @param T
     * @param id
     * @param options
     * @return
     */
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

    /**
     * Subscribe
     * @param T
     * @return
     */
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

    /**
     * Builder
     *
     * @constructor Create empty Builder
     */
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

        /**
         * Equals to
         *
         * @param key
         * @param value
         */
        fun equalsTo(key: String, value: Any) {
            Equals.attachFor(key, value)
        }

        /**
         * Not equals to
         *
         * @param key
         * @param value
         */
        fun notEqualsTo(key: String, value: Any) {
            NotEquals.attachFor(key, value)
        }

        /**
         * Less than
         *
         * @param key
         * @param value
         */
        fun lessThan(key: String, value: Any) {
            LessThan.attachFor(key, value)
        }

        /**
         * Less than or equal to
         *
         * @param key
         * @param value
         */
        fun lessThanOrEqualTo(key: String, value: Any) {
            LessThanOrEqualTo.attachFor(key, value)
        }

        /**
         * Greater than
         *
         * @param key
         * @param value
         */
        fun greaterThan(key: String, value: Any) {
            GreaterThan.attachFor(key, value)
        }

        /**
         * Greater than or equal to
         *
         * @param key
         * @param value
         */
        fun greaterThanOrEqualTo(key: String, value: Any) {
            GreaterThanOrEqualTo.attachFor(key, value)
        }

        /**
         * Contained it
         *
         * @param key
         * @param values
         */
        fun containedIt(key: String, values: List<String>) {
            ContainedIn.attachFor(key, Json.encodeToJsonElement(values))
        }

        /**
         * Not contained in
         *
         * @param key
         * @param values
         */
        fun notContainedIn(key: String, values: List<String>) {
            NotContainedIn.attachFor(key, Json.encodeToJsonElement(values))
        }

        /**
         * Contains all
         *
         * @param key
         * @param values
         */
        fun containsAll(key: String, values: List<String>) {
            ContainsAll.attachFor(key, Json.encodeToJsonElement(values))
        }

        /**
         * Exists
         *
         * @param key
         */
        fun exists(key: String) {
            Exists.attachFor(key, true)
        }

        /**
         * Not exists
         *
         * @param key
         */
        fun notExists(key: String) {
            Exists.attachFor(key, false)
        }

        /**
         * Matches key in query
         *
         * @param key
         * @param queryKey
         * @param query
         */
        fun matchesKeyInQuery(key: String, queryKey: String, query: ParseQuery) {
            Matches.attachFor(key, buildJsonObject {
                put("key", queryKey)
                put("query", Json.encodeToJsonElement(query.query))
            })
        }

        /**
         * Does not matches key in query
         *
         * @param key
         * @param queryKey
         * @param query
         */
        fun doesNotMatchesKeyInQuery(key: String, queryKey: String, query: ParseQuery) {
            NotMatches.attachFor(key, buildJsonObject {
                put("key", queryKey)
                put("query", Json.encodeToJsonElement(query.query))
            })
        }

        /**
         * Matches query
         *
         * @param key
         * @param query
         */
        fun matchesQuery(key: String, query: ParseQuery) {
            MatchesQuery.attachFor(key, Json.encodeToJsonElement(query.query))
        }

        /**
         * Not matches query
         *
         * @param key
         * @param query
         */
        fun notMatchesQuery(key: String, query: ParseQuery) {
            NotMatchesQuery.attachFor(key, Json.encodeToJsonElement(query.query))
        }

        /**
         * Related
         *
         * @param key
         * @param parent
         */
        fun related(key: String, parent: ParseClass) {
            constraints.add(QueryConstraint(RelatedTo.operator, buildJsonObject {
                put("object", Json.encodeToJsonElement(ParsePointer(parent)))
                put("key", key)
            }, RelatedTo.operator))
        }

        /**
         * Match
         *
         * @param key
         * @param regex
         */
        fun match(key: String, regex: String) {
            RexEx.attachFor(key, regex)
        }

        /**
         * Contains
         *
         * @param key
         * @param substring
         */
        fun contains(key: String, substring: String) {
            val regex = regexStringFor(substring)
            RexEx.attachFor(key, regex)
        }

        /**
         * Has prefix
         *
         * @param key
         * @param prefix
         */
        fun hasPrefix(key: String, prefix: String) {
            val regex = "^\\(${regexStringFor(prefix)})"
            RexEx.attachFor(key, regex)
        }

        /**
         * Has suffix
         *
         * @param key
         * @param suffix
         */
        fun hasSuffix(key: String, suffix: String) {
            val regex = "\\(${regexStringFor(suffix)})$"
            RexEx.attachFor(key, regex)
        }

        /**
         * Regex string for
         *
         * @param inputString
         * @return
         */
        private fun regexStringFor(inputString: String): String {
            val escapedString = inputString.replace("\\E", "\\E\\\\E\\Q")
            return "\\Q${escapedString}\\E"
        }

        /**
         * Full text
         *
         * @param key
         * @param value
         */
        fun fullText(key: String, value: String) {
            Text.attachFor(key, buildJsonObject { put(Search.operator, buildJsonObject { put(Term.operator, value) }) })
        }

        /**
         * Near
         *
         * @param key
         * @param value
         */
        fun near(key: String, value: ParseGeoPoint) {
            Near.attachFor(key, value)
        }

        /**
         * Within miles
         *
         * @param key
         * @param value
         * @param miles
         */
        fun withinMiles(key: String, value: ParseGeoPoint, miles: Double) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, (miles / ParseGeoPoint.earthRadiusMiles))
        }

        /**
         * Within kilometers
         *
         * @param key
         * @param value
         * @param km
         */
        fun withinKilometers(key: String, value: ParseGeoPoint, km: Double) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, (km / ParseGeoPoint.earthRadiusKilometers))
        }

        /**
         * Within radians
         *
         * @param key
         * @param value
         * @param radians
         */
        fun withinRadians(key: String, value: ParseGeoPoint, radians: Double) {
            Near.attachFor(key, value)
            MaxDistanceInMiles.attachFor(key, radians)
        }

        /**
         * Within geo box
         *
         * @param key
         * @param southWest
         * @param northEast
         */
        fun withinGeoBox(key: String, southWest: ParseGeoPoint, northEast: ParseGeoPoint) {
            Within.attachFor(
                key,
                buildJsonObject { put(Box.operator, Json.encodeToJsonElement(arrayListOf(southWest, northEast))) })
        }

        /**
         * Within polygon
         *
         * @param key
         * @param polygon
         */
        fun withinPolygon(key: String, polygon: List<ParseGeoPoint>) {
            GeoWithin.attachFor(key, buildJsonObject { put(Polygon.operator, Json.encodeToJsonElement(polygon)) })
        }

        /**
         * Polygon contains
         *
         * @param key
         * @param point
         */
        fun polygonContains(key: String, point: ParseGeoPoint) {
            GeoIntersects.attachFor(key, buildJsonObject { put(Point.operator, Json.encodeToJsonElement(point)) })
        }

        /**
         * Include
         *
         * @param include
         */
        fun include(vararg include: String) {
            this.include.addAll(include.toList())
        }

        /**
         * Include all
         *
         */
        fun includeAll() {
            this.include.clear()
            this.include.add("*")
        }

        /**
         * Exclude
         *
         * @param include
         */
        fun exclude(vararg include: String) {
            this.exclude.addAll(include.toList())
        }

        /**
         * Clear
         *
         * @param key
         */
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

