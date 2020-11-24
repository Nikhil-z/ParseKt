package me.lekov.parsekt

import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.lekov.parsekt.annotations.ParseClassName
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.types.ParseClass
import me.lekov.parsekt.types.ParseClassCompanion
import me.lekov.parsekt.types.ParseGeoPoint
import me.lekov.parsekt.types.ParseQuery
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ParseQueryUnitTest {


    lateinit var json: Json


    @ParseClassName("GameScore")
    class GameScore(var score: Int) : ParseClass() {
        companion object : ParseClassCompanion()
    }


    @ParseClassName("GameType")
    class GameType() : ParseClass() {
        companion object : ParseClassCompanion()
    }

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
    fun testQueryStaticProperties() {
        val score = GameScore(42)
        Assert.assertEquals("GameScore", score.className)
    }

    @Test
    fun testEmptyQuery() {
        GameScore.query {
            Assert.assertEquals(0, this.constraints.size)
        }
    }

    @Test
    fun testQueryConstraintsConstructions() {
        GameScore.query {
            greaterThan(GameScore::score.name, 100)
            greaterThan(GameScore::createdAt.name, LocalDateTime.now())
            Assert.assertEquals(2, this.constraints.size)
        }
    }

    @Test
    fun testQueryIncludeSingle() {
        GameScore.query {
            include("one")
            Assert.assertEquals(1, this.include.size)
            Assert.assertArrayEquals(arrayOf("one"), this.include.toTypedArray())
        }
    }

    @Test
    fun testQueryIncludeWithDuplicates() {
        GameScore.query {
            include("one")
            include("one", "two")
            Assert.assertEquals(2, this.include.size)
            Assert.assertArrayEquals(arrayOf("one", "two"), this.include.toTypedArray())
        }
    }

    @Test
    fun testQueryIncludeAll() {
        GameScore.query {
            includeAll()
            Assert.assertEquals(1, this.include.size)
            Assert.assertArrayEquals(arrayOf("*"), this.include.toTypedArray())
        }
    }

    @Test
    fun testQueryIncludeAllAfterSome() {
        GameScore.query {
            include("disappearing")
            includeAll()
            Assert.assertEquals(1, this.include.size)
            Assert.assertArrayEquals(arrayOf("*"), this.include.toTypedArray())
        }
    }

    @Test
    fun testQueryExcludeSingle() {
        GameScore.query {
            exclude("one")
            Assert.assertEquals(1, this.exclude.size)
            Assert.assertArrayEquals(arrayOf("one"), this.exclude.toTypedArray())
        }
    }

    @Test
    fun testQueryExcludeWithDuplicates() {
        GameScore.query {
            exclude("one")
            exclude("one", "two")
            Assert.assertEquals(2, this.exclude.size)
            Assert.assertArrayEquals(arrayOf("one", "two"), this.exclude.toTypedArray())
        }
    }

    @Test
    fun testWhereKeyExists() {
        GameScore.query {
            exists("score")
            Assert.assertEquals("{\"where\":{\"score\":{\"\$exists\":true}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereKeyDoesNotExists() {
        GameScore.query {
            notExists("score")
            Assert.assertEquals("{\"where\":{\"score\":{\"\$exists\":false}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereEqualTo() {
        GameScore.query {
            equalsTo("score", 100)
            Assert.assertEquals("{\"where\":{\"score\":{\"\$eq\":100}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereNotEqualTo() {
        GameScore.query {
            notEqualsTo("score", 100)
            Assert.assertEquals(
                "{\"where\":{\"score\":{\"\$ne\":100}}}",
                json.encodeToString(ParseQuery.Builder.serializer(), this)
            )
        }
    }

    @Test
    fun testWhereLessThan() {
        GameScore.query {
            lessThan("score", 100)
            Assert.assertEquals("{\"where\":{\"score\":{\"\$lt\":100}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereLessThanOrEqualTo() {
        GameScore.query {
            lessThanOrEqualTo("score", 100)
            Assert.assertEquals("{\"where\":{\"score\":{\"\$lte\":100}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereGreaterThan() {
        GameScore.query {
            greaterThan("score", 100)
            Assert.assertEquals("{\"where\":{\"score\":{\"\$gt\":100}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereGreaterThanOrEqualTo() {
        GameScore.query {
            greaterThanOrEqualTo("score", 100)
            Assert.assertEquals("{\"where\":{\"score\":{\"\$gte\":100}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereFullTextSearch() {
        GameScore.query {
            fullText("name", "Habibi")
            Assert.assertEquals(
                "{\"where\":{\"name\":{\"\$text\":{\"\$search\":{\"\$term\":\"Habibi\"}}}}}",
                json.encodeToString(this)
            )
        }
    }

    @Test
    fun testWhereMatchesRegex() {
        GameScore.query {
            match("name", "Habibi")
            Assert.assertEquals("{\"where\":{\"name\":{\"\$regex\":\"Habibi\"}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereKeyContains() {
        GameScore.query {
            contains("name", "Habibi")
            Assert.assertEquals("{\"where\":{\"name\":{\"\$regex\":\"\\\\QHabibi\\\\E\"}}}", json.encodeToString(this))
        }
    }

    @Test
    fun testWhereKeyHasPrefix() {
        GameScore.query {
            hasPrefix("name", "Habibi")
            Assert.assertEquals(
                "{\"where\":{\"name\":{\"\$regex\":\"^\\\\(\\\\QHabibi\\\\E)\"}}}",
                json.encodeToString(this)
            )
        }
    }

    @Test
    fun testWhereKeyHasSuffix() {
        GameScore.query {
            hasSuffix("name", "Habibi")
            Assert.assertEquals(
                "{\"where\":{\"name\":{\"\$regex\":\"\\\\(\\\\QHabibi\\\\E)\$\"}}}",
                json.encodeToString(this)
            )
        }
    }

    @Test
    fun testOrQuery() {
        val or = GameScore.query { equalsTo(GameScore::score.name, 50) }
            .or(GameScore.query { equalsTo(GameScore::score.name, 100) })

        Assert.assertEquals("{\"where\":{\"\$or\":[{\"score\":{\"\$eq\":50}},{\"score\":{\"\$eq\":100}}]}}", json.encodeToString(or.query))
    }

    @Test
    fun testAndQuery() {
        val and = GameScore.query { equalsTo(GameScore::score.name, 50) }
            .and(GameScore.query { equalsTo(GameScore::score.name, 100) })

        Assert.assertEquals("{\"where\":{\"\$and\":[{\"score\":{\"\$eq\":50}},{\"score\":{\"\$eq\":100}}]}}", json.encodeToString(and.query))
    }

    @Test
    fun testWhereKeyMatchesInQuery() {
        val query = GameScore.query {
            matchesKeyInQuery("type", "type", GameType.query { equalsTo("map", "Summons Rift") })
        }

        Assert.assertEquals("{\"where\":{\"type\":{\"\$select\":{\"key\":\"type\",\"query\":{\"where\":{\"map\":{\"\$eq\":\"Summons Rift\"}}}}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereKeyDoesNotMatchesInQuery() {
        val query = GameScore.query {
            doesNotMatchesKeyInQuery("type", "type", GameType.query { equalsTo("map", "Summons Rift") })
        }

        Assert.assertEquals("{\"where\":{\"type\":{\"\$dontSelect\":{\"key\":\"type\",\"query\":{\"where\":{\"map\":{\"\$eq\":\"Summons Rift\"}}}}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereKeyMatchesQuery() {
        val query = GameScore.query {
            matchesQuery("type", GameType.query { equalsTo("map", "Summons Rift") })
        }

        Assert.assertEquals("{\"where\":{\"type\":{\"\$inQuery\":{\"where\":{\"map\":{\"\$eq\":\"Summons Rift\"}}}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereKeyDoesNotMatchesQuery() {
        val query = GameScore.query {
            notMatchesQuery("type", GameType.query { equalsTo("map", "Summons Rift") })
        }

        Assert.assertEquals("{\"where\":{\"type\":{\"\$inQuery\":{\"where\":{\"map\":{\"\$eq\":\"Summons Rift\"}}}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereContainedIn() {
        val query = GameScore.query {
            containedIt("gender", listOf("Male", "Female", "Other"))
        }

        Assert.assertEquals("{\"where\":{\"gender\":{\"\$in\":[\"Male\",\"Female\",\"Other\"]}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereNotContainedIn() {
        val query = GameScore.query {
            notContainedIn("gender", listOf("Male", "Female", "Other"))
        }

        Assert.assertEquals("{\"where\":{\"gender\":{\"\$nin\":[\"Male\",\"Female\",\"Other\"]}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereContainsAll() {
        val query = GameScore.query {
            containsAll("gender", listOf("Male", "Female", "Other"))
        }

        Assert.assertEquals("{\"where\":{\"gender\":{\"\$all\":[\"Male\",\"Female\",\"Other\"]}}}", json.encodeToString(query.query))
    }

    @Test
    fun testWhereKeyRelated() {
        val query = GameScore.query {
            related("type", GameType().also { it.objectId = "1234" })
        }

        Assert.assertEquals("{\"where\":{\"\$relatedTo\":{\"object\":{\"__type\":\"Pointer\",\"className\":\"GameType\",\"objectId\":\"1234\"},\"key\":\"type\"}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryWithRelated() {
        val query = GameScore.query {
            equalsTo("score", 50)
            related("type", GameType().also { it.objectId = "1234" })
        }

        Assert.assertEquals("{\"where\":{\"\$relatedTo\":{\"object\":{\"__type\":\"Pointer\",\"className\":\"GameType\",\"objectId\":\"1234\"},\"key\":\"type\"},\"score\":{\"\$eq\":50}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryNear() {
        val point = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val query = GameScore.query {
           near("location", point)
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$nearSphere\":{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryWithinMiles() {
        val point = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val query = GameScore.query {
            withinMiles("location", point, 3958.8)
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$nearSphere\":{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0},\"\$maxDistanceInMiles\":1.0}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryWithinKilometers() {
        val point = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val query = GameScore.query {
            withinKilometers("location", point, 6371.0)
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$nearSphere\":{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0},\"\$maxDistanceInMiles\":1.0}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryWithinRadians() {
        val point = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val query = GameScore.query {
            withinRadians("location", point, 10.0)
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$nearSphere\":{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0},\"\$maxDistanceInMiles\":10.0}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryWithinGeoBox() {
        val point1 = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val point2 = ParseGeoPoint(latitude = 20.0, longitude = 30.0)
        val query = GameScore.query {
            withinGeoBox("location", point1, point2)
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$within\":{\"\$box\":[{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0},{\"__type\":\"GeoPoint\",\"latitude\":20.0,\"longitude\":30.0}]}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryWithinPolygon() {
        val point1 = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val point2 = ParseGeoPoint(latitude = 20.0, longitude = 30.0)
        val point3 = ParseGeoPoint(latitude = 30.0, longitude = 40.0)
        val query = GameScore.query {
            withinPolygon("location", arrayListOf(point1, point2, point3))
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$geoWithin\":{\"\$polygon\":[{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0},{\"__type\":\"GeoPoint\",\"latitude\":20.0,\"longitude\":30.0},{\"__type\":\"GeoPoint\",\"latitude\":30.0,\"longitude\":40.0}]}}}}", json.encodeToString(query.query))
    }

    @Test
    fun testQueryPolygonContains() {
        val point = ParseGeoPoint(latitude = 10.0, longitude = 20.0)
        val query = GameScore.query {
            polygonContains("location", point)
        }

        Assert.assertEquals("{\"where\":{\"location\":{\"\$geoIntersects\":{\"\$point\":{\"__type\":\"GeoPoint\",\"latitude\":10.0,\"longitude\":20.0}}}}}", json.encodeToString(query.query))
    }
}