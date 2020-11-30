package me.lekov.parsekt

import android.content.Context

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
        serverUrl: String
    ) {
        Parse.context = context
        Parse.applicationId = applicationId
        Parse.masterKey = masterKey
        Parse.clientKey = clientKey
        Parse.serverUrl = serverUrl
    }
}