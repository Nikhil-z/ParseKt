package me.lekov.parsekt.store

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.lekov.parsekt.Parse

class SecureStore {

    private val masterKeyAlias =
        MasterKey.Builder(Parse.context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val sharedPreferences = EncryptedSharedPreferences
        .create(
            Parse.context,
            SECURE_PREFERENCE_FILENAME,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getInt(key: String): Int {
        return sharedPreferences.getInt(key, -1)
    }

    fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun setString(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    inline fun <reified T> setObject(key: String, value: T) {
        sharedPreferences.edit()
            .putString(key, Json { ignoreUnknownKeys = true }.encodeToString(value)).apply()
    }

    inline fun <reified T> getObject(key: String): T? {
        if (sharedPreferences.contains(key)) {
            return Json { ignoreUnknownKeys = true }.decodeFromString(
                sharedPreferences.getString(
                    key,
                    null
                )!!
            )
        }

        return null
    }

    companion object {
        private const val SECURE_PREFERENCE_FILENAME = "parse_secure_storage"

        @PublishedApi
        internal const val CURRENT_USER_KEY = "_currentUser"
        internal const val CURRENT_INSTALLATION_KEY = "_currentInstallation"
    }
}