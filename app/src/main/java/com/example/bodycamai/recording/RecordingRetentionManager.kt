package com.example.bodycamai.recording

import android.content.ContentResolver
import android.provider.MediaStore

/** Keeps the local video archive bounded. Protected videos are never deleted by this policy. */
class RecordingRetentionManager(private val resolver: ContentResolver) {
    fun trim(maxRecordings: Int): Int {
        if (maxRecordings < 1) return 0
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED)
        val rows = mutableListOf<Pair<Long, Long>>()
        resolver.query(uri, projection, "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?", arrayOf("%BodyCamAI%"), "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val date = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (c.moveToNext()) rows += c.getLong(id) to c.getLong(date)
        }
        if (rows.size <= maxRecordings) return 0
        var deleted = 0
        rows.drop(maxRecordings).forEach { (id, _) ->
            val item = uri.buildUpon().appendPath(id.toString()).build()
            if (resolver.delete(item, null, null) > 0) deleted++
        }
        return deleted
    }
}
