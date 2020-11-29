package me.lekov.parsekt.types

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import me.lekov.parsekt.annotations.ParseClassName
import me.lekov.parsekt.api.LoginSignUpResponse
import me.lekov.parsekt.api.ParseApi
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.store.SecureStore

/**
 * Parse user
 *
 * @constructor Create empty Parse user
 */
@Serializable
@ParseClassName("_User")
open class ParseUser : ParseObject<ParseUser>() {

    var username: String? = null
    var email: String? = null

    @PublishedApi
    internal var password: String? = null

    /**
     * Companion
     *
     * @constructor Create empty Companion
     */
    companion object : ParseClassCompanion() {

        @Serializable
        data class CurrentUserContainer<T : ParseUser>(
            var currentUser: T?,
            var sessionToken: String?
        )

        @PublishedApi
        internal var currentUserContainer: CurrentUserContainer<ParseUser>?
            get() = ParseStore.secureStore.getObject(SecureStore.CURRENT_USER_KEY)
            set(value) = if (value != null) {
                ParseStore.secureStore.setObject(SecureStore.CURRENT_USER_KEY, value)
            } else {
                ParseStore.secureStore.delete(SecureStore.CURRENT_USER_KEY)
            }

        val sessionToken = currentUserContainer?.sessionToken
        inline fun <reified T : ParseUser> currentUser() = ParseStore.secureStore.getObject<CurrentUserContainer<T>>(SecureStore.CURRENT_USER_KEY)?.currentUser
    }

    @PublishedApi
    internal inline fun <reified T : ParseUser> signupCommand(
        username: String,
        password: String
    ): ParseApi.Command<ParseUser, T> {

        this.username = username
        this.password = password

        return ParseApi.Command(
            HttpMethod.Post,
            ParseApi.Endpoint.Signup,
            body = this,
            mapper = {
                val raw = json.decodeFromString<LoginSignUpResponse>(it)
                val user = json.decodeFromString<T>(it)

                this.objectId = user.objectId
                this.createdAt = user.createdAt
                this.updatedAt = user.updatedAt

                ParseStore.secureStore.setObject(SecureStore.CURRENT_USER_KEY, CurrentUserContainer(this as T, raw.sessionToken))
                currentUser<T>()!!
            })
    }

    @PublishedApi
    internal inline fun <reified T : ParseUser> loginCommand(
        username: String,
        password: String
    ): ParseApi.Command<Map<String, String>, T> {
        return ParseApi.Command(
            HttpMethod.Get,
            ParseApi.Endpoint.Login,
            body = mapOf("username" to username, "password" to password),
            mapper = {
                val raw = json.decodeFromString<LoginSignUpResponse>(it)
                val user = json.decodeFromString<T>(it)

                this.username = username
                this.password = password

                ParseStore.secureStore.setObject(SecureStore.CURRENT_USER_KEY, CurrentUserContainer(this as T, raw.sessionToken))
                currentUser<T>()!!
            })
    }

    @PublishedApi
    internal fun logoutCommand(): ParseApi.Command<Void, Unit> {
        return ParseApi.Command(HttpMethod.Post, ParseApi.Endpoint.Logout, mapper = {
            ParseStore.secureStore.delete(SecureStore.CURRENT_USER_KEY)
        })
    }


    /**
     * Logout
     *
     */
    suspend fun logout() {
        return logoutCommand().execute(emptySet())
    }

    override fun toString(): String {
        return "ParseUser(className='$className', username=$username, email=$email)"
    }
}

/**
 * Login
 *
 * @param T
 * @param username
 * @param password
 * @return
 */
suspend inline fun <reified T : ParseUser> T.login(username: String, password: String): T {
    return loginCommand<T>(username, password).execute(emptySet())
}

/**
 * Signup
 *
 * @param T
 * @param username
 * @param password
 * @return
 */
suspend inline fun <reified T : ParseUser> T.signup(username: String, password: String): T {
    return signupCommand<T>(username, password).execute(emptySet())
}