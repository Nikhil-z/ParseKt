package me.lekov.parsekt.types

import android.text.TextUtils
import kotlinx.serialization.Serializable
import me.lekov.parsekt.BuildConfig
import me.lekov.parsekt.Parse
import me.lekov.parsekt.annotations.ParseClassName
import me.lekov.parsekt.store.ParseStore
import me.lekov.parsekt.store.SecureStore
import java.util.*


/**
 * Parse installation
 *
 * @constructor Create empty Parse installation
 */
@Serializable
@ParseClassName("_Installation")
class ParseInstallation : ParseObject<ParseInstallation>() {
    var deviceType: String? = null
        internal set
    var installationId: String? = null
        internal set
    var deviceToken: String? = null
        internal set
    var badge: Int? = 0
        internal set
    var timeZone: String? = null
        internal set
    var channels: List<String> = emptyList()
        internal set
    var appName: String? = null
        internal set
    var appVersion: String? = null
        internal set
    var appIdentifier: String? = null
        internal set
    var parseVersion: String? = null
        internal set
    var localeIdentifier: String? = null
        internal set

    companion object {

        @PublishedApi
        internal var currentInstallation: ParseInstallation = ParseStore.secureStore.getObject(SecureStore.CURRENT_INSTALLATION_KEY)
                ?: ParseInstallation().apply {
                    val context = Parse.context.applicationContext
                    val packageName = context.packageName
                    val pm = context.packageManager
                    val pkgInfo = pm.getPackageInfo(packageName, 0)
                    val appVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode.toString()
                    } else {
                        pkgInfo.versionCode.toString()
                    }
                    val appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()

                    this.appName = appName
                    this.appVersion = appVersion
                    this.appIdentifier = packageName
                    this.parseVersion = BuildConfig.VERSION_NAME
                    this.localeIdentifier = getLocale()
                    this.timeZone = getTimeZone()
                    this.deviceType = "android"
                    this.installationId = UUID.randomUUID().toString()

                }

        private fun getTimeZone(): String? {
            val zone = TimeZone.getDefault().id
            return if (zone.indexOf('/') > 0 || zone == "GMT") {
                zone
            } else null
        }

        private fun getLocale(): String? {
            val locale = Locale.getDefault()
            var language = locale.language
            val country = locale.country

            if (TextUtils.isEmpty(language)) {
                return null
            }

            // rewrite depreciated two-letter codes
            if (language == "iw") language = "he" // Hebrew
            if (language == "in") language = "id" // Indonesian
            if (language == "ji") language = "yi" // Yiddish

            var localeString = language

            if (!TextUtils.isEmpty(country)) {
                localeString = java.lang.String.format(Locale.US, "%s-%s", language, country)
            }

            return localeString
        }

        val current = currentInstallation
    }
}