package me.lekov.parsekt.types

import kotlinx.serialization.Serializable
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.store.SecureStore

@Serializable
open class ParseUser : ParseObject() {

    override val className: String = "_User"

    var username: String? = null
    var email: String? = null
    var password: String? = null

    companion object {

        data class CurrentUserContainer(var currentUser: ParseUser?, var sessionToken: String?)

        private var currentUserContainer: CurrentUserContainer?
            get() = ParseStore.secureStore.getObject<CurrentUserContainer>(SecureStore.CURRENT_USER_KEY)
            set(value) = ParseStore.secureStore.setObject(SecureStore.CURRENT_USER_KEY, value)

        var currentUser: ParseUser?
            get() = currentUserContainer?.currentUser
            set(value) {
                currentUserContainer?.currentUser = value
            }

        suspend fun login(username: String, password: String) {

        }
    }
}