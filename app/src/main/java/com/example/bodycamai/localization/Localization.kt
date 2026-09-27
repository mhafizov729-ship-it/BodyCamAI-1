package com.example.bodycamai.localization

import android.content.Context
import java.util.Locale
import java.io.File

enum class LanguagePack(val tag: String, val nativeName: String) {
    AUTO("auto", "Auto"), RU("ru", "Русский"), EN("en", "English"), ES("es", "Español"),
    DE("de", "Deutsch"), FR("fr", "Français"), PT("pt", "Português"), IT("it", "Italiano"),
    TR("tr", "Türkçe"), AR("ar", "العربية"), ZH("zh", "中文"), JA("ja", "日本語"), KO("ko", "한국어"), HI("hi", "हिन्दी")
}

class LocalizationManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("bodycam_localization", Context.MODE_PRIVATE)
    private val packs = LanguagePackRepository(context)

    fun selected(): LanguagePack = runCatching { LanguagePack.valueOf(prefs.getString("language", "AUTO") ?: "AUTO") }.getOrDefault(LanguagePack.AUTO)
    fun setLanguage(pack: LanguagePack) { prefs.edit().putString("language", pack.name).apply() }
    fun effectiveLocale(): Locale {
        val selected = selected()
        return if (selected == LanguagePack.AUTO) context.resources.configuration.locales[0] else Locale.forLanguageTag(selected.tag)
    }
    fun text(key: String): String {
        val lang = effectiveLocale().language
        val custom = packs.load(lang)[key]
        return custom ?: BuiltInStrings.value(key, lang)
    }
    fun installedLanguagePacks(): List<FileInfo> = packs.installed().map { FileInfo(it.name, it.length()) }
    data class FileInfo(val name: String, val bytes: Long)
}

private object BuiltInStrings {
    private val data = mapOf(
        "start" to mapOf("ru" to "Начать наблюдение", "en" to "Start monitoring", "es" to "Iniciar monitorización", "de" to "Überwachung starten", "fr" to "Démarrer la surveillance"),
        "camera" to mapOf("ru" to "Камера", "en" to "Camera", "es" to "Cámara", "de" to "Kamera", "fr" to "Caméra"),
        "sensors" to mapOf("ru" to "Сенсоры", "en" to "Sensors", "es" to "Sensores", "de" to "Sensoren", "fr" to "Capteurs"),
        "ai" to mapOf("ru" to "ИИ", "en" to "AI", "es" to "IA", "de" to "KI", "fr" to "IA"),
        "stop" to mapOf("ru" to "Стоп", "en" to "Stop", "es" to "Detener", "de" to "Stopp", "fr" to "Arrêter")
    )
    fun value(key: String, language: String): String = data[key]?.get(language) ?: data[key]?.get("en") ?: key
}
