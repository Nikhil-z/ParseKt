package me.lekov.parsekt

import androidx.test.core.app.ApplicationProvider
import me.lekov.parsekt.annotations.ParseClassName
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.types.ParseClassCompanion
import me.lekov.parsekt.types.ParseUser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun testParseUserClassShouldBeRight() {
        Parse.initialize(ApplicationProvider.getApplicationContext(), "appId", null, "masterKey", "http://127.0.0.1:1337/parse", ParseStore())
        val testParseUser = TestParseUser()
        assertEquals("_User", testParseUser.className)
    }
}

@ParseClassName("_User")
class TestParseUser : ParseUser() {
    companion object : ParseClassCompanion()
}