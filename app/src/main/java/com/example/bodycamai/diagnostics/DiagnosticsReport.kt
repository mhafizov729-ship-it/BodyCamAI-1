package com.example.bodycamai.diagnostics

import android.content.Context
import com.example.bodycamai.core.DiagnosticsResult
import com.example.bodycamai.core.SensorHealth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creates a local, human-readable diagnostics snapshot without uploading data. */
class DiagnosticsReport(private val context: Context) {
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun build(result: DiagnosticsResult, sensors: List<SensorHealth> = emptyList()): String = buildString {
        appendLine("BodyCam AI diagnostics")
        appendLine("Generated: ${format.format(Date())}")
        appendLine("Overall: ${result.overall}")
        appendLine("Camera permission: ${result.camera}")
        appendLine("Microphone permission: ${result.microphone}")
        appendLine("GPS/location: ${result.gps}")
        appendLine("Compass: ${result.compass}")
        appendLine("Storage: ${result.storage}")
        appendLine("AI: ${result.ai}")
        appendLine("Network available: ${result.network}")
        appendLine()
        appendLine("Sensor health:")
        if (sensors.isEmpty()) appendLine("  none")
        sensors.forEach { s ->
            appendLine("  ${s.source}: connected=${s.connected}, ageMs=${s.lastFrameAgeMs}, fps=${s.fps}, confidence=${s.confidence}, latencyMs=${s.latencyMs}")
        }
    }

    fun save(text: String): File {
        val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
        val file = File(dir, "diagnostics-${System.currentTimeMillis()}.txt")
        file.writeText(text)
        return file
    }
}
