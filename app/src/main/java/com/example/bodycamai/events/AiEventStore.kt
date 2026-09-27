package com.example.bodycamai.events

import android.content.Context
import com.example.bodycamai.core.AiFrameResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Local event journal. No network is required. */
class AiEventStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, "ai_events.json")

    fun add(result: AiFrameResult, timestamp: Long = System.currentTimeMillis(), latitude: Double? = null, longitude: Double? = null, sourceUri: String? = null, captureId: String? = null) {
        val all = loadRaw()
        val objects = JSONArray()
        result.objects.forEach { d ->
            objects.put(JSONObject().apply {
                put("label", d.label)
                put("confidence", d.confidence.toDouble())
                put("trackingId", d.trackingId ?: JSONObject.NULL)
                put("safetyLabel", d.safetyLabel ?: JSONObject.NULL)
            })
        }
        all.put(JSONObject().apply {
            put("timestamp", timestamp)
            put("latitude", latitude ?: JSONObject.NULL)
            put("longitude", longitude ?: JSONObject.NULL)
            put("sourceUri", sourceUri ?: JSONObject.NULL)
            put("captureId", captureId ?: JSONObject.NULL)
            put("faceCount", result.faceCount)
            put("ocr", result.text.take(1000))
            put("objects", objects)
        })
        file.writeText(all.toString())
    }

    fun list(limit: Int = 200): List<AiEvent> {
        val raw = loadRaw()
        val out = mutableListOf<AiEvent>()
        for (i in raw.length() - 1 downTo maxOf(0, raw.length() - limit)) {
            val o = raw.optJSONObject(i) ?: continue
            out += AiEvent(
                o.optLong("timestamp"),
                o.optDoubleOrNull("latitude"), o.optDoubleOrNull("longitude"),
                o.optString("sourceUri", null), o.optString("captureId", null), o.optInt("faceCount"),
                o.optString("ocr", ""), o.optJSONArray("objects")?.length() ?: 0,
                o.optJSONArray("objects")?.maxConfidence() ?: 0f
            )
        }
        return out
    }


    fun forCapture(captureId: String): List<AiEvent> = list(1000).filter { it.captureId == captureId }

    fun clear() { if (file.exists()) file.delete() }

    fun exportJson(): String = loadRaw().toString(2)

    fun countForCapture(captureId: String): Int = forCapture(captureId).size

    private fun loadRaw(): JSONArray = if (file.exists()) runCatching { JSONArray(file.readText()) }.getOrDefault(JSONArray()) else JSONArray()

    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key)) null else optDouble(key)
    private fun JSONArray.maxConfidence(): Float {
        var max = 0.0
        for (i in 0 until length()) max = maxOf(max, optJSONObject(i)?.optDouble("confidence", 0.0) ?: 0.0)
        return max.toFloat()
    }
}

data class AiEvent(
    val timestamp: Long,
    val latitude: Double?, val longitude: Double?, val sourceUri: String?,
    val captureId: String?, val faceCount: Int, val ocr: String, val objectCount: Int, val maxConfidence: Float
)
