package com.example.bodycamai.archive

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.bodycamai.events.AiEventStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveManager(private val context: Context) {
    private val eventStore = AiEventStore(context)
    fun query(filter: ArchiveFilter = ArchiveFilter()): List<ArchiveItem> {
        val result = mutableListOf<ArchiveItem>()
        queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/mp4", ArchiveType.VIDEO, result)
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg", ArchiveType.PHOTO, result)
        return result.map { it.copy(aiEventCount = captureIdOf(it.name)?.let(eventStore::countForCapture) ?: 0) }
            .filter { filter.type == null || it.type == filter.type }
            .filter { filter.onlyWithAiEvents == null || (it.aiEventCount > 0) == filter.onlyWithAiEvents }
            .filter { filter.query.isBlank() || it.name.contains(filter.query, true) || it.captureId?.let(eventStore::forCapture)?.any { e -> e.ocr.contains(filter.query, true) } == true }
            .sortedByDescending { it.dateMillis }
    }

    private fun queryCollection(uri: Uri, mime: String, type: ArchiveType, out: MutableList<ArchiveItem>) {
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE)
        context.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val date = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (c.moveToNext()) {
                val displayName = c.getString(name) ?: continue
                if (!displayName.startsWith("BODYCAM_")) continue
                out += ArchiveItem(Uri.withAppendedPath(uri, c.getLong(id).toString()), displayName, type,
                    c.getLong(date) * 1000L, c.getLong(size), c.getString(mimeIndex) ?: mime,
                    captureId = captureIdOf(displayName))
            }
        }
    }

    private fun captureIdOf(name: String): String? = name.substringAfter("BODYCAM_", "").substringBeforeLast(".").takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
}

data class ArchiveItem(val uri: Uri, val name: String, val type: ArchiveType, val dateMillis: Long, val sizeBytes: Long, val mimeType: String, val captureId: String? = null, val aiEventCount: Int = 0) {
    fun formattedDate(): String = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(dateMillis))
}
enum class ArchiveType { VIDEO, PHOTO }
data class ArchiveFilter(
    val query: String = "",
    val type: ArchiveType? = null,
    val onlyWithAiEvents: Boolean? = null
)
