package me.lekov.parsekt

object Parse {
    internal lateinit var applicationId: String
    internal var masterKey: String? = null
    internal var clientKey: String? = null
    lateinit var serverUrl: String

    fun initialize(
        applicationId: String,
        clientKey: String? = null,
        masterKey: String? = null,
        serverUrl: String
    ) {
        Parse.applicationId = applicationId
        Parse.masterKey = masterKey
        Parse.clientKey = clientKey
        Parse.serverUrl = serverUrl
    }
}