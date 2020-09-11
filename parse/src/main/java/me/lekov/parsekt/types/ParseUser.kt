package me.lekov.parsekt.types

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import me.lekov.parsekt.annotations.ParseClass
import me.lekov.parsekt.api.LoginSignUpResponse
import me.lekov.parsekt.api.ParseApi
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.store.SecureStore

@Serializable
@ParseClass("_User")
open class ParseUser : ParseObject() {

    var username: String? = null
    var email: String? = null
    var password: String? = null

    companion object: ParseObjectCompanion() {

        @Serializable
        data class CurrentUserContainer(var currentUser: ParseUser?, var sessionToken: String?)

        private var currentUserContainer: CurrentUserContainer?
            get() = ParseStore.secureStore.getObject<CurrentUserContainer>(SecureStore.CURRENT_USER_KEY)
            set(value) = if (value != null) {
                ParseStore.secureStore.setObject(SecureStore.CURRENT_USER_KEY, value)
            } else {
                ParseStore.secureStore.delete(SecureStore.CURRENT_USER_KEY)
            }

        val sessionToken = currentUserContainer?.sessionToken

        var currentUser: ParseUser?
            get() = currentUserContainer?.currentUser
            set(value) {
                currentUserContainer?.currentUser = value
            }

        private inline fun <reified T : ParseUser> loginCommand(
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

                    user.username = username
                    user.password = password

                    currentUserContainer = CurrentUserContainer(user, raw.sessionToken)
                    user
                })
        }

        private fun logoutCommand(): ParseApi.Command<Void, Unit> {
            return ParseApi.Command(HttpMethod.Post, ParseApi.Endpoint.Logout, mapper = {
                currentUserContainer = null
            })
        }

        private inline fun <reified T : ParseUser> signupCommand(
            username: String,
            password: String
        ): ParseApi.Command<Map<String, String>, T> {
            return ParseApi.Command(
                HttpMethod.Post,
                ParseApi.Endpoint.Signup,
                body = mapOf("username" to username, "password" to password),
                mapper = {
                    val raw = json.decodeFromString<LoginSignUpResponse>(it)
                    val user = json.decodeFromString<T>(it)

                    user.username = username
                    user.password = password
                    user.createdAt = raw.createdAt
                    user.updatedAt = raw.createdAt

                    currentUserContainer = CurrentUserContainer(user, raw.sessionToken)
                    user
                })
        }

        suspend fun login(username: String, password: String): ParseUser {
            return loginCommand<ParseUser>(username, password).execute(emptySet())
        }

        suspend fun signup(username: String, password: String): ParseUser {
            return signupCommand<ParseUser>(username, password).execute(emptySet())
        }

        suspend fun logout() {
            return logoutCommand().execute(emptySet())
        }
    }

    override fun toString(): String {
        return "ParseUser(className='$className', username=$username, email=$email, password=$password)"
    }
}