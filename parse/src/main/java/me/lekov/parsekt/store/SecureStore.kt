package me.lekov.parsekt.store

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.lekov.parsekt.Parse

class SecureStore {

    private val masterKeyAlias = MasterKey.Builder(Parse.context).build()
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

    fun getInt(key: String): Int? {
        return sharedPreferences.getInt(key, -1)
    }

    fun setString(key: String, value: String) {
        return sharedPreferences.edit().putString(key, value).apply()
    }

    fun setString(key: String, value: Int) {
        return sharedPreferences.edit().putInt(key, value).apply()
    }

    inline fun <reified T> setObject(key: String, value: T) {
        sharedPreferences.edit().putString(key, Json.encodeToString(value)).apply()
    }

    inline fun <reified T> getObject(key: String): T? {
        if (sharedPreferences.contains(key)) {
            return Json.decodeFromString(sharedPreferences.getString(key, null)!!)
        }

        return null
    }

    companion object {
        private const val SECURE_PREFERENCE_FILENAME = "parse_secure_storage"
        internal const val CURRENT_USER_KEY = "_currentUser"
    }
}