package com.example.bodycamai.map

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/** Local-first map repository. Supports imported tile packages and validated custom tile URLs. */
class OfflineMapRepository(private val context: Context) {
    private val root = File(context.filesDir, "maps").apply { mkdirs() }
    private val packages = File(root, "packages").apply { mkdirs() }
    private val sourcesFile = File(root, "sources.json")

    data class MapPackage(val id: String, val name: String, val format: String, val file: File, val importedAt: Long)
    data class TileSource(val id: String, val name: String, val template: String, val minZoom: Int, val maxZoom: Int)

    fun listPackages(): List<MapPackage> = packages.listFiles()?.filter { it.isFile }
        ?.mapNotNull { file ->
            runCatching {
                val meta = File(file.parentFile, file.nameWithoutExtension + ".json")
                val o = if (meta.isFile) JSONObject(meta.readText()) else JSONObject()
                MapPackage(file.nameWithoutExtension, o.optString("name", file.nameWithoutExtension), o.optString("format", "unknown"), file, o.optLong("importedAt", file.lastModified()))
            }.getOrNull()
        }?.sortedByDescending { it.importedAt } ?: emptyList()

    fun importPackage(source: File): Result<MapPackage> = runCatching {
        require(source.isFile) { "Файл карты не найден" }
        require(source.length() <= 2L * 1024 * 1024 * 1024) { "Пакет карты слишком большой" }
        val ext = source.extension.lowercase()
        require(ext in setOf("mbtiles", "pmtiles", "zip")) { "Поддерживаются MBTiles, PMTiles или ZIP" }
        if (ext == "zip") validateZip(source)
        val safeId = source.nameWithoutExtension.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dest = File(packages, "$safeId.$ext")
        source.copyTo(dest, overwrite = true)
        val meta = File(packages, "$safeId.json")
        meta.writeText(JSONObject().put("name", source.nameWithoutExtension).put("format", ext).put("importedAt", System.currentTimeMillis()).toString())
        MapPackage(safeId, source.nameWithoutExtension, ext, dest, System.currentTimeMillis())
    }

    fun removePackage(id: String) {
        File(packages, "$id.mbtiles").delete()
        File(packages, "$id.pmtiles").delete()
        File(packages, "$id.zip").delete()
        File(packages, "$id.json").delete()
    }

    fun saveSource(source: TileSource) {
        val arr = org.json.JSONArray()
        readSources().filterNot { it.id == source.id }.forEach { arr.put(sourceJson(it)) }
        arr.put(sourceJson(source))
        sourcesFile.writeText(arr.toString())
    }

    fun readSources(): List<TileSource> = if (!sourcesFile.isFile) emptyList() else runCatching {
        val a = org.json.JSONArray(sourcesFile.readText())
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            TileSource(o.getString("id"), o.getString("name"), o.getString("template"), o.getInt("minZoom"), o.getInt("maxZoom"))
        }
    }.getOrDefault(emptyList())

    fun validateSource(source: TileSource): Boolean {
        if (source.name.isBlank() || source.template.isBlank()) return false
        if (source.minZoom !in 0..24 || source.maxZoom !in source.minZoom..24) return false
        val t = source.template.lowercase()
        return t.startsWith("https://") && t.contains("{z}") && t.contains("{x}") && t.contains("{y}")
    }

    fun tileUrl(source: TileSource, z: Int, x: Int, y: Int): String? {
        if (!validateSource(source) || z !in source.minZoom..source.maxZoom) return null
        return source.template.replace("{z}", z.toString()).replace("{x}", x.toString()).replace("{y}", y.toString())
    }

    private fun sourceJson(s: TileSource) = JSONObject().put("id", s.id).put("name", s.name).put("template", s.template).put("minZoom", s.minZoom).put("maxZoom", s.maxZoom)

    private fun validateZip(file: File) {
        ZipFile(file).use { zip ->
            require(zip.size() <= 200_000) { "ZIP содержит слишком много файлов" }
            zip.entries().asSequence().forEach { e ->
                require(!e.name.contains("..")) { "Небезопасный путь в ZIP" }
                require(!e.isDirectory || e.name.length < 512) { "Некорректный ZIP" }
            }
        }
    }
}
