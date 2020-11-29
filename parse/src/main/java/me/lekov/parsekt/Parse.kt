package me.lekov.parsekt

import android.content.Context
import me.lekov.parsekt.store.ParseStore

/**
 * Parse
 *
 * @constructor Create empty Parse
 */
object Parse {
    internal lateinit var context: Context
    internal lateinit var applicationId: String
    internal var masterKey: String? = null
    internal var clientKey: String? = null
    lateinit var serverUrl: String
    lateinit var storage: ParseStore

    /**
     * Initialize
     *
     * @param context
     * @param applicationId
     * @param clientKey
     * @param masterKey
     * @param serverUrl
     * @param storage
     */
    fun initialize(
        context: Context,
        applicationId: String,
        clientKey: String? = null,
        masterKey: String? = null,
        serverUrl: String,
        storage: ParseStore
    ) {
        Parse.context = context
        Parse.applicationId = applicationId
        Parse.masterKey = masterKey
        Parse.clientKey = clientKey
        Parse.serverUrl = serverUrl
        Parse.storage = storage
    }
}