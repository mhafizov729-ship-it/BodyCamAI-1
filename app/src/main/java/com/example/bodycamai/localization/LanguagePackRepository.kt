package com.example.bodycamai.localization

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Data-only language packs. No executable code is loaded from packs. */
class LanguagePackRepository(private val context: Context) {
    private val dir = File(context.filesDir, "language-packs").apply { mkdirs() }

    fun installed(): List<File> = dir.listFiles { f -> f.isFile && f.extension.equals("json", true) }
        ?.sortedBy { it.name.lowercase() } ?: emptyList()

    fun validate(file: File): Boolean {
        if (!file.isFile || file.length() > 5L * 1024 * 1024) return false
        return runCatching {
            val root = JSONObject(file.readText())
            val language = root.optString("language").trim()
            val strings = root.optJSONObject("strings")
            language.matches(Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{2,8})?$")) && strings != null
        }.getOrDefault(false)
    }

    fun load(tag: String): Map<String, String> {
        val normalized = tag.lowercase().substringBefore('-')
        val file = installed().firstOrNull { it.nameWithoutExtension.lowercase() == normalized || it.nameWithoutExtension.lowercase().startsWith("$normalized-") }
            ?: return emptyMap()
        return runCatching {
            val strings = JSONObject(file.readText()).optJSONObject("strings") ?: return@runCatching emptyMap()
            strings.keys().asSequence().associateWith { strings.optString(it) }
        }.getOrDefault(emptyMap())
    }
}
