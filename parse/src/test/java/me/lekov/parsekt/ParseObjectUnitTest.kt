package me.lekov.parsekt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.lekov.parsekt.annotations.ParseClassName
import me.lekov.parsekt.types.ParseClass
import me.lekov.parsekt.types.ParseClassCompanion
import me.lekov.parsekt.types.ParsePointer
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ParseObjectUnitTest {

    @Test
    fun testDecodeNotIncludedPointer() {
        val json = """{
            "objectId": "NPer9Sqsfy",
            "createdAt": "2020-11-30T12:28:05.249Z",
            "updatedAt": "2020-11-30T12:29:31.061Z",
            "type": {
                "__type": "Pointer",
                "className": "GameType",
                "objectId": "yCAJKy6135"
            },
            "score": 50
        }""".trimIndent()

        val gameScore = Json.decodeFromString(GameScore.serializer(), json)
        Assert.assertNotNull(gameScore)
        Assert.assertEquals("NPer9Sqsfy", gameScore.objectId)
        Assert.assertEquals(50, gameScore.score)
        Assert.assertNotNull(gameScore.type)
        Assert.assertEquals("yCAJKy6135", gameScore.type.objectId)
        Assert.assertFalse(gameScore.type.available)
    }

    @Test
    fun testDecodeIncludedPointer() {
        val json = """
            {
            "objectId": "NPer9Sqsfy",
            "createdAt": "2020-11-30T12:28:05.249Z",
            "updatedAt": "2020-11-30T12:29:31.061Z",
            "type": {
                "objectId": "yCAJKy6135",
                "name": "Solo Top",
                "createdAt": "2020-11-30T12:27:56.170Z",
                "updatedAt": "2020-11-30T12:27:56.170Z",
                "__type": "Object",
                "className": "GameType"
            },
            "score": 50
        }
        """.trimIndent()

        val gameScore = Json.decodeFromString(GameScore.serializer(), json)

        Assert.assertNotNull(gameScore)
        Assert.assertEquals("NPer9Sqsfy", gameScore.objectId)
        Assert.assertEquals(50, gameScore.score)
        Assert.assertNotNull(gameScore.type)
        Assert.assertEquals("yCAJKy6135", gameScore.type.objectId)
        Assert.assertTrue(gameScore.type.available)
    }

    @Test
    fun testGetIncludedPointer() {
        val json = """
            {
            "objectId": "NPer9Sqsfy",
            "createdAt": "2020-11-30T12:28:05.249Z",
            "updatedAt": "2020-11-30T12:29:31.061Z",
            "type": {
                "objectId": "yCAJKy6135",
                "name": "Solo Top",
                "createdAt": "2020-11-30T12:27:56.170Z",
                "updatedAt": "2020-11-30T12:27:56.170Z",
                "__type": "Object",
                "className": "GameType"
            },
            "score": 50
        }
        """.trimIndent()

        val gameScore = Json.decodeFromString(GameScore.serializer(), json)

        Assert.assertNotNull(gameScore.type.get<GameType>())
        Assert.assertTrue(gameScore.type.get<GameType>() is GameType)
    }

    @Serializable
    @ParseClassName("GameScore")
    class GameScore(var score: Int) : ParseClass() {
        lateinit var type: ParsePointer
        val types by ParseRelationDelegate()
        companion object : ParseClassCompanion()
    }

    @Serializable
    @ParseClassName("GameType")
    class GameType(var name: String) : ParseClass() {
        companion object : ParseClassCompanion()
    }
}