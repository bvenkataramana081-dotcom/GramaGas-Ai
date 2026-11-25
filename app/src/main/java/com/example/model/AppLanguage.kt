package com.example.model

import android.content.Context

enum class AppLanguage(
    val code: String,
    val bcp47Locale: String,
    val nativeName: String,
    val englishName: String,
    val scriptName: String,
    val isRtl: Boolean = false
) {
    TELUGU(
        code = "te",
        bcp47Locale = "te-IN",
        nativeName = "తెలుగు",
        englishName = "Telugu",
        scriptName = "Telugu"
    ),
    HINDI(
        code = "hi",
        bcp47Locale = "hi-IN",
        nativeName = "हिन्दी",
        englishName = "Hindi",
        scriptName = "Devanagari"
    ),
    ENGLISH(
        code = "en",
        bcp47Locale = "en-IN",
        nativeName = "English",
        englishName = "English",
        scriptName = "Latin"
    ),
    TAMIL(
        code = "ta",
        bcp47Locale = "ta-IN",
        nativeName = "தமிழ்",
        englishName = "Tamil",
        scriptName = "Tamil"
    ),
    KANNADA(
        code = "kn",
        bcp47Locale = "kn-IN",
        nativeName = "ಕನ್ನಡ",
        englishName = "Kannada",
        scriptName = "Kannada"
    ),
    MALAYALAM(
        code = "ml",
        bcp47Locale = "ml-IN",
        nativeName = "മലയാളം",
        englishName = "Malayalam",
        scriptName = "Malayalam"
    ),
    BENGALI(
        code = "bn",
        bcp47Locale = "bn-IN",
        nativeName = "বাংলা",
        englishName = "Bengali",
        scriptName = "Bengali"
    ),
    MARATHI(
        code = "mr",
        bcp47Locale = "mr-IN",
        nativeName = "मराठी",
        englishName = "Marathi",
        scriptName = "Devanagari"
    ),
    GUJARATI(
        code = "gu",
        bcp47Locale = "gu-IN",
        nativeName = "ગુજરાતી",
        englishName = "Gujarati",
        scriptName = "Gujarati"
    ),
    PUNJABI(
        code = "pa",
        bcp47Locale = "pa-IN",
        nativeName = "ਪੰਜਾਬੀ",
        englishName = "Punjabi",
        scriptName = "Gurmukhi"
    ),
    ODIA(
        code = "or",
        bcp47Locale = "or-IN",
        nativeName = "ଓଡ଼ିଆ",
        englishName = "Odia",
        scriptName = "Odia"
    ),
    ASSAMESE(
        code = "as",
        bcp47Locale = "as-IN",
        nativeName = "অসমীয়া",
        englishName = "Assamese",
        scriptName = "Assamese"
    ),
    URDU(
        code = "ur",
        bcp47Locale = "ur-IN",
        nativeName = "اردو",
        englishName = "Urdu",
        scriptName = "Perso-Arabic (Urdu)",
        isRtl = true
    );

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: TELUGU
        }
    }
}

class LanguagePreferenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadLanguage(): AppLanguage {
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, AppLanguage.TELUGU.code)
        return AppLanguage.fromCode(savedCode)
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE_CODE, language.code).apply()
    }

    companion object {
        private const val PREFS_NAME = "gramagas_language_prefs"
        private const val KEY_LANGUAGE_CODE = "selected_language_code"
    }
}
