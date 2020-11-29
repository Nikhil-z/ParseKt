package me.lekov.parsekt

import androidx.test.core.app.ApplicationProvider
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.lekov.parsekt.annotations.ParseClassName
import me.lekov.parsekt.api.ParseApi
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.types.ACL
import me.lekov.parsekt.types.ParseClass
import me.lekov.parsekt.types.ParseClassCompanion
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ParseApiUnitTest {

    lateinit var json: Json

    @Before
    fun setUp() {
        json = Json {}
        Parse.initialize(
            ApplicationProvider.getApplicationContext(),
            "appId",
            null,
            "masterKey",
            "http://127.0.0.1:1337/parse",
            ParseStore()
        )
    }

    @Test
    fun testParseApiEndpointObjects() = runBlockingTest {
        val command = ParseApi.Command<TestParseUser, TestParseUser>(
            HttpMethod.Get,
            ParseApi.Endpoint.Objects("GameScore"),
            httpClient = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        assertEquals("http://127.0.0.1:1337/parse/classes/GameScore", request.url.toString())
                        respondOk()
                    }
                }
                install(JsonFeature)
            },
            mapper = {
                TestParseUser()
            })

        command.execute()
    }

    @Test
    fun testParseApiEndpointObject() = runBlockingTest {
        val command = ParseApi.Command<TestParseUser, TestParseUser>(
            HttpMethod.Get,
            ParseApi.Endpoint.Object("GameScore", "objectId"),
            httpClient = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        assertEquals("http://127.0.0.1:1337/parse/classes/GameScore/objectId", request.url.toString())
                        respondOk()
                    }
                }
                install(JsonFeature)
            },
            mapper = {
                TestParseUser()
            })

        command.execute()
    }

    @Test
    fun testParseApiGetObject() = runBlockingTest {
        val command = ParseApi.Command<GameScore, GameScore>(
            HttpMethod.Get,
            ParseApi.Endpoint.Object("GameScore", "objectId"),
            httpClient = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        val gameScore = GameScore(false, "Test Player", 0)
                        gameScore.objectId = "objectId"
                        gameScore.createdAt = LocalDateTime.now()
                        gameScore.updatedAt = LocalDateTime.now()
                        gameScore.ACL = ACL().apply { publicRead = true; publicWrite = true }
                        respondOk(Json.encodeToString(gameScore))
                    }
                }
                install(JsonFeature)
            },
            mapper = {
                Json.decodeFromString(it)
            })

        val result = command.execute()
        assertEquals("objectId", result.objectId)
        assertEquals(0, result.score)
    }

    @Serializable
    @ParseClassName("GameScore")
    class GameScore(var cheatMode: Boolean? = false, var playerName: String? = null, var score: Int? = 0) :
        ParseClass() {
        companion object : ParseClassCompanion()
    }
}

