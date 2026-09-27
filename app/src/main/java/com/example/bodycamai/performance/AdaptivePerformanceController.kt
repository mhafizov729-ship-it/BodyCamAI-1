package com.example.bodycamai.performance

/** Chooses conservative runtime quality from memory pressure and battery state. */
enum class PerformanceTier { HIGH, BALANCED, SAVER, CRITICAL }

data class AdaptivePerformanceState(
    val tier: PerformanceTier,
    val aiEnabled: Boolean,
    val analysisIntervalMs: Long,
    val maxOverlayObjects: Int,
    val reason: String
)

class AdaptivePerformanceController {
    fun decide(snapshot: PerformanceSnapshot, batteryPercent: Int? = null): AdaptivePerformanceState {
        val memoryRatio = if (snapshot.memoryLimitMb > 0) snapshot.memoryUsedMb.toDouble() / snapshot.memoryLimitMb else 0.0
        val critical = snapshot.lowMemory || memoryRatio >= 0.90 || (batteryPercent != null && batteryPercent <= 8)
        val saver = memoryRatio >= 0.78 || (batteryPercent != null && batteryPercent <= 20)
        val balanced = memoryRatio >= 0.62 || (batteryPercent != null && batteryPercent <= 35)
        return when {
            critical -> AdaptivePerformanceState(PerformanceTier.CRITICAL, false, 1000L, 6, "Высокая нагрузка: AI временно ограничен")
            saver -> AdaptivePerformanceState(PerformanceTier.SAVER, true, 500L, 10, "Экономия энергии/памяти")
            balanced -> AdaptivePerformanceState(PerformanceTier.BALANCED, true, 250L, 20, "Сбалансированная нагрузка")
            else -> AdaptivePerformanceState(PerformanceTier.HIGH, true, 120L, 40, "Ресурсы в норме")
        }
    }
}
