package me.lekov.parsekt.api

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.cache.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import me.lekov.parsekt.Parse
import me.lekov.parsekt.types.ParseObject
import me.lekov.parsekt.types.ParseQuery
import java.util.Collections.synchronizedMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.set

@PublishedApi
internal class ParseLiveQuery private constructor(@PublishedApi internal val client: HttpClient) {

    private val clients: MutableMap<Int, ParseSubscription> = synchronizedMap(HashMap<Int, ParseSubscription>())
    var webSocketSession: WebSocketSession? = null

    private suspend fun listen() {
        val session = client.webSocketSession(host = "10.10.10.121", port = 1337, path = "parse")
        webSocketSession = session

        session.launch {

            while (session.isActive) {
                try {
                    val receive = session.incoming.receive()
                    val receiveString = String(receive.readBytes())
                    val data = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(receiveString)

                    when (data["op"]!!.jsonPrimitive.content) {
                        "connected", "subscribed", "unsubscribed", "error" -> {
                            Log.i("ParseLiveQuery", receiveString)
                        }
                        else -> clients.get(data["requestId"]?.jsonPrimitive?.int)?.channel?.send(receiveString)
                    }
                } catch (e: Error) {
                    Log.e("ParseLiveQuery", "cannot fetch frame", e)
                }
            }
        }

        session.launch {
            session.send(Frame.Text(buildJsonObject {
                put("op", "connect")
                put("applicationId", Parse.applicationId)
                put("masterKey", Parse.masterKey)
            }.toString()))
        }
    }

    suspend fun subscribe(className: String, query: ParseQuery.Builder): Flow<String> {

        if (webSocketSession == null) {
            listen()
        }

        val subscription = ParseSubscription(webSocketSession!!, className, query)
        clients[subscription.id] = subscription

        webSocketSession?.launch {
            subscription.subscribe()
        }

        return channelFlow {

            subscription.channel.consumeEach {
                this.send(it)
            }

            awaitClose {
                launch {
                    Log.i("ParseLiveQuery", "closing...")
                    subscription.unsubscribe()
                    clients.remove(subscription.id)
                    if (clients.isEmpty()) {
                        webSocketSession?.close()
                        webSocketSession = null
                    }
                }
            }
        }
    }

    companion object : SingletonHolder<ParseLiveQuery, HttpClient>(::ParseLiveQuery)

    data class Command<U : ParseObject<U>>(
        val className: ParseApi.Endpoint.Other,
        val query: ParseQuery.Builder,
        var mapper: (String) -> LiveQueryResponse<U>,
        val serializers: SerializersModule? = null,
        var items: MutableList<U>
    ) {
        val httpClient = HttpClient(CIO) {
            install(WebSockets)
            install(HttpCache)
            install(JsonFeature)
        }

        suspend inline fun subscribe(): Flow<List<U>> {
            return ParseLiveQuery.getInstance(httpClient).subscribe(className.path, query).map {
                mapper.invoke(it)
            }.map { res ->
                when (res.op) {
                    "create", "enter" -> {
                        items.add(res.`object`)
                        items
                    }
                    "update" -> {
                        items = items.map {
                            if (it.objectId == res.`object`.objectId) res.`object` else it
                        }.toMutableList()
                        items
                    }
                    "leave", "delete" -> {
                        items.removeIf { it.objectId == res.`object`.objectId }
                        items
                    }
                    else -> items
                }
            }
        }
    }
}

private class ParseSubscription(
    private val session: WebSocketSession,
    val className: String,
    private val query: ParseQuery.Builder
) {
    companion object {
        var lastId = AtomicInteger(0)
    }

    val channel: Channel<String> = Channel()
    val id = lastId.getAndIncrement()

    suspend fun subscribe() {
        val query = Json.encodeToJsonElement(ParseQuery.Builder.serializer(), query).jsonObject

        session.send(Frame.Text(buildJsonObject {
            put("op", "subscribe")
            put("requestId", id)
            put("query", buildJsonObject {
                put("className", className)
                put("where", query["where"]!!.jsonObject)
            })
        }.toString()))
    }

    suspend fun unsubscribe() {
        session.send(Frame.Text(buildJsonObject {
            put("op", "unsubscribe")
            put("requestId", id)
        }.toString()))
    }
}

open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}
