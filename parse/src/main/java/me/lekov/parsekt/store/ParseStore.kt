package me.lekov.parsekt.store

abstract class ParseStore {

    companion object {
        internal val secureStore by lazy { SecureStore() }
    }
}