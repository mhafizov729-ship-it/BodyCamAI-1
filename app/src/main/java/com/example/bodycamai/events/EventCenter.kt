package com.example.bodycamai.events

import android.content.Context
import com.example.bodycamai.core.SensorHealth
import com.example.bodycamai.fusion.SpatialFusionEngine.SpatialEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Central local event journal. Keeps operational events in one append-only text log. */
class EventCenter(context: Context) {
    private val logFile = File(context.filesDir, "events/runtime-events.log").apply { parentFile?.mkdirs() }
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized fun record(type: String, message: String, timestamp: Long = System.currentTimeMillis()) {
        logFile.appendText("${format.format(Date(timestamp))}\t$type\t${message.replace('\n', ' ')}\n")
    }

    fun recordSpatial(event: SpatialEvent) = record(
        "SPATIAL",
        "label=${event.label};track=${event.trackingId};confidence=${event.confidence};bearing=${event.bearingDegrees};lat=${event.latitude};lon=${event.longitude};source=${event.source}"
    )

    fun recordSensorHealth(health: SensorHealth) = record(
        "SENSOR",
        "source=${health.source};connected=${health.connected};ageMs=${health.lastFrameAgeMs};fps=${health.fps};confidence=${health.confidence};latencyMs=${health.latencyMs}"
    )

    fun readAll(): String = if (logFile.exists()) logFile.readText() else ""
    fun clear() { if (logFile.exists()) logFile.writeText("") }
    fun file(): File = logFile
}
