package com.smartnotes.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    const val ENGLISH = "en"
    const val CHINESE = "zh"
    const val PREF_NAME = "smartnotes_prefs"
    const val PREF_KEY_LOCALE = "locale"

    val supportedLanguages = listOf(ENGLISH, CHINESE)

    fun getLanguageLabel(code: String): String = when (code) {
        ENGLISH -> "English"
        CHINESE -> "中文"
        else -> "English"
    }

    /**
     * Wrap the base context with the appropriate locale configuration.
     * Call this from Activity.attachBaseContext().
     */
    fun wrapContext(context: Context): Context {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(PREF_KEY_LOCALE, null)
        if (lang != null && lang != Locale.getDefault().language) {
            return updateContextLocale(context, lang)
        }
        return context
    }

    /**
     * Create a new Context with the given locale applied.
     */
    fun updateContextLocale(context: Context, language: String): Context {
        val locale = when (language) {
            CHINESE -> Locale.SIMPLIFIED_CHINESE
            ENGLISH -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        // Also set layout direction for RTL languages if needed
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context.createConfigurationContext(config)
    }

    /**
     * Save the selected language to SharedPreferences.
     */
    fun saveLocale(context: Context, language: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_LOCALE, language)
            .apply()
    }

    /**
     * Get the current saved locale, or system default.
     */
    fun getSavedLocale(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY_LOCALE, getSystemLanguage())
            ?: getSystemLanguage()
    }

    fun getSystemLanguage(): String {
        return Locale.getDefault().language
    }
}
