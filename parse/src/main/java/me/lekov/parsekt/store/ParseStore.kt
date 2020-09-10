package me.lekov.parsekt.store

class ParseStore {

    companion object {
        internal val secureStore by lazy { SecureStore() }
    }
}