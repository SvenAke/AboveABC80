package com.aboveware.aboveabc80

import java.util.Locale

private val defaultSystemLocale: Locale = Locale.getDefault()

fun getAppLanguage(): String = getPersistedString("app_language", "system")

fun setAppLanguage(language: String) {
    setPersistedString("app_language", language)
    applyAppLanguage(language)
}

fun applyAppLanguage(language: String = getAppLanguage()) {
    val targetLocale = when (language) {
        "sv" -> Locale.forLanguageTag("sv")
        "en" -> Locale.forLanguageTag("en")
        else -> defaultSystemLocale
    }
    Locale.setDefault(targetLocale)
}
