package me.lekov.parsekt.store

class ParseStore {

    companion object {
        @PublishedApi
        internal val secureStore by lazy { SecureStore() }
    }
}