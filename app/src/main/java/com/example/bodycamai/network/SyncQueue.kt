package com.example.bodycamai.network

import android.content.Context
import com.example.bodycamai.core.GroupMessage
import org.json.JSONObject

/** Small durable outbox. It never pretends that a message was delivered until a backend confirms it. */
class SyncQueue(context: Context) {
    private val prefs = context.getSharedPreferences("bodycam_sync", Context.MODE_PRIVATE)
    private val key = "outbox"

    fun enqueue(message: GroupMessage) {
        val current = JSONArrayCompat.read(prefs.getString(key, null)).toMutableList()
        current += JSONObject().apply {
            put("id", message.id)
            put("senderId", message.senderId)
            put("senderName", message.senderName)
            put("text", message.text)
            put("type", message.type.name)
            put("timestamp", message.timestamp)
            put("channel", message.channel.name)
        }.toString()
        prefs.edit().putString(key, JSONArrayCompat.write(current)).apply()
    }

    fun size(): Int = JSONArrayCompat.read(prefs.getString(key, null)).size
    fun clearAfterServerConfirmation() = prefs.edit().remove(key).apply()
}

private object JSONArrayCompat {
    fun read(raw: String?): List<String> = if (raw.isNullOrBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
    fun write(items: List<String>): String = items.joinToString("\n")
}
