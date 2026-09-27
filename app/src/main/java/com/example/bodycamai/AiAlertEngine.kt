package com.example.bodycamai

import com.example.bodycamai.core.AiFrameResult

/** Local, conservative AI event classifier. It reports detections, not intent or guilt. */
class AiAlertEngine(private val cooldownMs: Long = 2500L, private val minConfidence: Float = 0.45f) {
    private var lastSignature = ""
    private var lastAt = 0L

    fun evaluate(result: AiFrameResult, now: Long = System.currentTimeMillis()): AiAlert? {
        val labels = result.objects.filter { it.confidence >= minConfidence }.map { it.label.lowercase() }.filter { it.isNotBlank() }.distinct()
        val signature = buildString {
            if (result.faceCount > 0) append("faces:").append(result.faceCount).append('|')
            labels.sorted().take(4).forEach { append(it).append('|') }
            if (result.text.isNotBlank()) append("ocr")
        }
        if (signature.isBlank()) return null
        if (signature == lastSignature && now - lastAt < cooldownMs) return null
        lastSignature = signature
        lastAt = now

        return when {
            result.faceCount > 0 -> AiAlert("AI • ОБНАРУЖЕНЫ ЛЮДИ", "Лиц: ${result.faceCount}", AlertLevel.INFO)
            labels.isNotEmpty() -> AiAlert("AI • ОБЪЕКТ ОБНАРУЖЕН", labels.take(3).joinToString(", "), AlertLevel.INFO)
            result.text.isNotBlank() -> AiAlert("AI • ТЕКСТ ОБНАРУЖЕН", result.text.replace("\n", " ").take(80), AlertLevel.INFO)
            else -> null
        }
    }
}

data class AiAlert(val title: String, val detail: String, val level: AlertLevel)
enum class AlertLevel { INFO, WARNING }
