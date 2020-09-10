package me.lekov.parsekt.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.json
import me.lekov.parsekt.GameScore
import me.lekov.parsekt.Parse
import me.lekov.parsekt.types.ParseObject
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.forEach
import kotlin.collections.mutableMapOf
import kotlin.collections.set

typealias Options = Set<ParseApi.Option>

class ParseApi {

    sealed class Endpoint {
        object Batch : Endpoint()
        class Objects(val className: String) : Endpoint()
        class Object(val className: String, val objectId: String) : Endpoint()
        object Login : Endpoint()
        object Signup : Endpoint()
        object Logout : Endpoint()
        class Other(val path: String) : Endpoint()
    }

    sealed class Option {
        object UseMasterKey : Option()
        class SessionToken(val token: String) : Option()
        class InstallationId(val instalattion: String) : Option()
    }

    sealed class Result<T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Failure(val error: Throwable) : Result<Nothing>()
        object Empty : Result<Nothing>()
    }

    data class Command<T, U>(
        val method: HttpMethod,
        val path: Endpoint,
        val params: Map<String, String>? = emptyMap(),
        val body: T? = null,
        var mapper: (String) -> U
    ) {
        val httpClient = HttpClient(CIO) {
            install(JsonFeature) {
                serializer =
                    KotlinxSerializer(json = Json(from = Json.Default) { encodeDefaults = false })
            }
        }

        suspend inline fun execute(options: Options): Result<out U> {

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
                    Result.Success(mapper.invoke(it))
                },
                { Result.Failure(it) }
            )
        }
    }

    companion object {
        fun getHeaders(options: Options): Map<String, String> {

            val headers = mutableMapOf(
                "X-Parse-Application-Id" to Parse.applicationId,
            )

            Parse.clientKey?.let {
                headers["X-Parse-Client-Key"] = it
            }

            // TODO: Same for the session token
            //  headers["X-Parse-Session-Token"] = token

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
                        headers["X-Parse-Installation-Id"] = it.instalattion
                    }
                }
            }

            return headers
        }

        fun urlComponent(endpoint: Endpoint) = when (endpoint) {
            is Endpoint.Batch -> "/batch"
            is Endpoint.Objects -> "/classes/${endpoint.className}"
            is Endpoint.Object -> "/classes/${endpoint.className}/${endpoint.objectId}"
            is Endpoint.Login -> "/login"
            is Endpoint.Signup -> "/users"
            is Endpoint.Logout -> "/users/logout"
            is Endpoint.Other -> endpoint.path
        }

        fun <T : ParseObject> createCommand(item: T): Command<T, T> {
            return Command(HttpMethod.Post, item.endpoint, body = item, mapper = {
                val res = Json.decodeFromString(SaveResponse.serializer(), it)
                res.apply(item)
            })
        }

        fun <T : ParseObject> updateCommand(item: T): Command<T, T> {
            return Command(HttpMethod.Put, item.endpoint, body = item, mapper = {
                val res = Json.decodeFromString(UpdateResponse.serializer(), it)
                res.apply(item)
            })
        }

        fun <T : ParseObject> saveCommand(item: T): Command<T, T> {
            return if (item.isSaved) {
                updateCommand(item)
            } else {
                createCommand(item)
            }
        }

        internal inline fun <reified T : ParseObject> fetchCommand(item: T): Command<T, T> {

            if (!item.isSaved) {
                throw Error("Cannot fetch an object without id")
            }

            return Command(HttpMethod.Get, item.endpoint, body = item, mapper = {
                val res = Json.decodeFromString<T>(it)
                res
            })
        }
    }
}