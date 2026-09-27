package com.example.bodycamai.localization

/** Built-in language registry. Speech engines are selected by Android/vendor availability. */
data class SupportedLanguage(val tag: String, val nativeName: String)

object SupportedLanguages {
    val all: List<SupportedLanguage> = listOf(
        SupportedLanguage("ru", "Русский"), SupportedLanguage("en", "English"),
        SupportedLanguage("es", "Español"), SupportedLanguage("de", "Deutsch"),
        SupportedLanguage("fr", "Français"), SupportedLanguage("pt", "Português"),
        SupportedLanguage("it", "Italiano"), SupportedLanguage("tr", "Türkçe"),
        SupportedLanguage("ar", "العربية"), SupportedLanguage("zh", "中文"),
        SupportedLanguage("ja", "日本語"), SupportedLanguage("ko", "한국어"),
        SupportedLanguage("hi", "हिन्दी")
    )
}
