package com.example.bodycamai.power

import com.example.bodycamai.performance.AdaptivePerformanceState
import com.example.bodycamai.performance.AdaptivePerformanceController
import com.example.bodycamai.performance.PerformanceSnapshot

/** Combines battery/thermal limits with the existing memory-aware performance controller. */
class AdaptiveRuntimePolicy(private val controller: AdaptivePerformanceController = AdaptivePerformanceController()) {
    fun decide(snapshot: PerformanceSnapshot, power: DevicePowerState): AdaptivePerformanceState {
        val base = controller.decide(snapshot, power.batteryPercent)
        if (power.thermalThrottling || power.powerSave) {
            return base.copy(
                analysisIntervalMs = maxOf(base.analysisIntervalMs, 500L),
                maxOverlayObjects = minOf(base.maxOverlayObjects, 12),
                reason = if (power.thermalThrottling) "Ограничение из-за температуры" else "Системное энергосбережение"
            )
        }
        return base
    }
}
