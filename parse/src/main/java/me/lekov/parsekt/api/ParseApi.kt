package me.lekov.parsekt.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.cache.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import me.lekov.parsekt.Parse
import me.lekov.parsekt.types.*
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.set

typealias Options = Set<ParseApi.Option>

class ParseApi {

    sealed class Endpoint {
        object Batch : Endpoint()
        class Objects(val className: String) : Endpoint()
        class Object(val className: String, val objectId: String) : Endpoint()
        object Users: Endpoint()
        class User(val objectId: String) : Endpoint()
        object Installations: Endpoint()
        class Installation(val objectId: String) : Endpoint()
        object Sessions : Endpoint()
        class Session(val objectId: String) : Endpoint()
        class Event(val event: String) : Endpoint()
        object Roles: Endpoint()
        class Role(val objectId: String) : Endpoint()
        object Login : Endpoint()
        object Logout : Endpoint()
        object Signup : Endpoint()
        class File(val fileName: String) : Endpoint()
        object PasswordReset : Endpoint()
        object VerificationEmail : Endpoint()
        class Functions(val name: String) : Endpoint()
        class Jobs(val name: String) : Endpoint()
        class Aggregate(val className: String) : Endpoint()
        object Config : Endpoint()
        object Health : Endpoint()
        class Other(val path: String) : Endpoint()
    }

    sealed class Option {
        object UseMasterKey : Option()
        class SessionToken(val token: String) : Option()
        class InstallationId(val installation: String) : Option()
        class MimeType(val mimeType: String): Option()
        class FileSize(val fileSize: String): Option()
        object RemoveMimeType: Option()
        class Metadata(val data: Map<String, String>): Option()
        class Tags(val data: Map<String, String>): Option()
    }

    data class Command<T, U>(
        val method: HttpMethod,
        val path: Endpoint,
        val params: Map<String, String>? = emptyMap(),
        val body: T? = null,
        var mapper: (String) -> U,
        val serializers: SerializersModule? = null,
        var httpClient: HttpClient = HttpClient(CIO) {
            install(WebSockets)
            install(HttpCache)
            install(JsonFeature) {
                serializer =
                    KotlinxSerializer(json = Json(from = Json.Default) {
                        encodeDefaults = false
                        ignoreUnknownKeys = true
                        serializersModule = SerializersModule {
                            contextual(String.serializer())
                            contextual(Int.serializer())
                            contextual(Long.serializer())
                            contextual(Double.serializer())
                            contextual(Boolean.serializer())
                            contextual(ParseUser.serializer())
                            serializers?.let { include(it) }
                        }
                    })
            }
        }
    ) {
        suspend inline fun execute(options: Options = emptySet()): U {

            val result = kotlin.runCatching {
                httpClient.request<String> {
                    method = this@Command.method

                    header(HttpHeaders.ContentType, ContentType.Application.Json)

                    url.takeFrom("${Parse.serverUrl}${urlComponent(path)}")

                    getHeaders(options).forEach {
                        headers[it.key] = it.value
                    }

                    params?.forEach {
                        url.parameters[it.key] = it.value
                    }

                    this@Command.body?.let {
                        body = it
                    }
                }
            }

            return result.fold(
                {
                    mapper.invoke(it)
                },
                {
                    when (it) {
                        is ClientRequestException ->
                            it.response.let { response ->
                                response.content.readUTF8Line()?.let { raw ->
                                    throw Json.decodeFromString<ParseError>(
                                        raw
                                    )
                                } ?: throw Error()
                            }
                        is UnresolvedAddressException -> {
                            // Internet Error
                            throw ParseError(ParseError.CONNECTION_FAILED)
                        }
                        else -> {
                            // Unhandled error
                            throw it
                        }
                    }
                }
            )
        }
    }

    companion object {
        @PublishedApi
        internal fun getHeaders(options: Options): Map<String, String> {

            val headers = mutableMapOf(
                "X-Parse-Application-Id" to Parse.applicationId,
            )

            Parse.clientKey?.let {
                headers["X-Parse-Client-Key"] = it
            }

            ParseUser.sessionToken?.let {
                headers["X-Parse-Session-Token"] = it
            }

            headers["X-Parse-Request-Id"] = UUID.randomUUID().toString()

            options.forEach {
                when (it) {
                    is Option.UseMasterKey -> {
                        Parse.masterKey?.let { masterKey ->
                            headers["X-Parse-Master-Key"] = masterKey
                        }
                    }
                    is Option.SessionToken -> {
                        headers["X-Parse-Session-Token"] = it.token
                    }
                    is Option.InstallationId -> {
                        headers["X-Parse-Installation-Id"] = it.installation
                    }
                    is Option.MimeType -> {
                        headers["Content-Type"] = it.mimeType
                    }
                    is Option.FileSize -> {
                        headers["Content-Length"] = it.fileSize
                    }
                    is Option.RemoveMimeType -> {
                        headers.remove("Content-Type")
                    }
                    is Option.Metadata -> {
                        it.data.forEach { meta ->
                            headers[meta.key] = meta.value
                        }
                    }
                    is Option.Tags -> {
                        it.data.forEach { tag ->
                            headers[tag.key] = tag.value
                        }
                    }
                }
            }

            return headers
        }

        @PublishedApi
        internal fun urlComponent(endpoint: Endpoint) = when (endpoint) {
            is Endpoint.Batch -> "/batch"
            is Endpoint.Objects -> "/classes/${endpoint.className}"
            is Endpoint.Object -> "/classes/${endpoint.className}/${endpoint.objectId}"
            is Endpoint.Users -> "/users"
            is Endpoint.User -> "/users/${endpoint.objectId}"
            is Endpoint.Installations -> "/installations"
            is Endpoint.Installation -> "/installations/${endpoint.objectId}"
            is Endpoint.Sessions -> "/sessions"
            is Endpoint.Session -> "/sessions/${endpoint.objectId}"
            is Endpoint.Event -> "/events/${endpoint.event}"
            is Endpoint.Roles -> "/roles"
            is Endpoint.Role -> "/roles/${endpoint.objectId}"
            is Endpoint.Login -> "/login"
            is Endpoint.Signup -> "/users"
            is Endpoint.Logout -> "/logout"
            is Endpoint.PasswordReset -> "/requestPasswordReset"
            is Endpoint.VerificationEmail -> "/verificationEmailRequest"
            is Endpoint.Functions -> "/functions/${endpoint.name}"
            is Endpoint.Jobs -> "/jobs/${endpoint.name}"
            is Endpoint.File -> "/files/${endpoint.fileName}"
            is Endpoint.Aggregate -> "/aggregate/${endpoint.className}"
            is Endpoint.Config -> "/config"
            is Endpoint.Health -> "/health"
            is Endpoint.Other -> endpoint.path
        }

        @PublishedApi
        internal fun <T : ParseObject<T>> createCommand(item: T): Command<T, T> {
            return Command(HttpMethod.Post, item.endpoint, body = item, mapper = {
                val res = ParseObject.json.decodeFromString(SaveResponse.serializer(), it)
                res.apply(item)
            },
                serializers = SerializersModule {
                    contextual(LocalDateTime::class, LocalDateTimeSerializer)
                })
        }

        @PublishedApi
        internal fun <T : ParseObject<T>> updateCommand(item: T): Command<T, T> {
            return Command(HttpMethod.Put, item.endpoint, body = item, mapper = {
                val res = ParseObject.json.decodeFromString(UpdateResponse.serializer(), it)
                res.apply(item)
            }, serializers = SerializersModule {
                contextual(LocalDateTime::class, LocalDateTimeSerializer)
            })
        }

        @PublishedApi
        internal fun <T : ParseObject<T>> saveCommand(item: T): Command<T, T> {
            return if (item.isSaved) {
                updateCommand(item)
            } else {
                createCommand(item)
            }
        }

        @PublishedApi
        internal fun <T : ParseObject<T>> deleteCommand(item: T): Command<T, Any> {
            return Command(
                HttpMethod.Delete,
                item.endpoint,
                body = null,
                mapper = {},
                serializers = SerializersModule {
                    contextual(LocalDateTime::class, LocalDateTimeSerializer)
                })
        }

        @PublishedApi
        internal inline fun <reified T : ParseObject<T>> fetchCommand(item: T): Command<T, T> {

            if (!item.isSaved) {
                throw Error("Cannot fetch an object without id")
            }

            return Command(HttpMethod.Get, item.endpoint, body = item, mapper = {
                val res = ParseObject.json.decodeFromString<T>(it)
                res
            },
                serializers = SerializersModule {
                    contextual(LocalDateTime::class, LocalDateTimeSerializer)
                })
        }
    }
}

/**
 * Save
 *
 * @param T
 * @param options
 * @return
 */
suspend inline fun <reified T : ParseObject<T>> ParseObject<T>.save(options: Options = emptySet()): ParseObject<T> {
    return ParseApi.saveCommand(this as T).execute(options)
}

/**
 * Fetch
 *
 * @param T
 * @param options
 * @return
 */
suspend inline fun <reified T : ParseObject<T>> ParseObject<T>.fetch(options: Options = emptySet()): ParseObject<T> {
    return ParseApi.fetchCommand(this as T).execute(options)
}

/**
 * Delete
 *
 * @param T
 * @param options
 */
suspend inline fun <reified T : ParseObject<T>> ParseObject<T>.delete(options: Options = emptySet()) {
    ParseApi.deleteCommand(this as T).execute(options)
}

/**
 * Relation
 *
 * @param T
 * @param key
 * @return
 */
inline fun <reified T : ParseObject<T>> ParseObject<T>.relation(key: String): ParseQuery.Builder {
    return ParseObject.query { related(key, this@relation as ParseObject<*>) }.query
}