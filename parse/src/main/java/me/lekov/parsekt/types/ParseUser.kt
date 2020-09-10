package me.lekov.parsekt.types

open class ParseUser : ParseObject() {

    var username: String? = null
    var email: String? = null
    var password: String? = null

    companion object : IDefineable {
        override val className: String = "_User"

        suspend fun login(username: String, password: String) {

        }
    }
}