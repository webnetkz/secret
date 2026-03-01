package com.appswebnetkz.secret.data.local

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var avatarUrl: String?
        get() = prefs.getString(KEY_AVATAR_URL, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_URL, value).apply()

    var localeCode: String
        get() = prefs.getString(KEY_LOCALE_CODE, null) ?: systemDefaultLocaleCode()
        set(value) = prefs.edit().putString(KEY_LOCALE_CODE, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, null) ?: systemDefaultThemeMode()
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var chatOrderJson: String
        get() = prefs.getString(KEY_CHAT_ORDER_JSON, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_CHAT_ORDER_JSON, value).apply()

    var unreadByChatJson: String
        get() = prefs.getString(KEY_UNREAD_BY_CHAT_JSON, "{}") ?: "{}"
        set(value) = prefs.edit().putString(KEY_UNREAD_BY_CHAT_JSON, value).apply()

    fun saveUserSession(
        userId: String,
        nickname: String?,
        avatarUrl: String?
    ) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .commit()
    }

    fun clearUserSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_NICKNAME)
            .remove(KEY_AVATAR_URL)
            .commit()
    }

    private fun systemDefaultLocaleCode(): String {
        val configuration = appContext.resources.configuration
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        } ?: Locale.ENGLISH
        return if (locale.language.equals("ru", ignoreCase = true)) "ru" else "en"
    }

    private fun systemDefaultThemeMode(): String {
        val uiMode = appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (uiMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
    }

    companion object {
        private const val PREF_NAME = "secret_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_LOCALE_CODE = "locale_code"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CHAT_ORDER_JSON = "chat_order_json"
        private const val KEY_UNREAD_BY_CHAT_JSON = "unread_by_chat_json"
    }
}
