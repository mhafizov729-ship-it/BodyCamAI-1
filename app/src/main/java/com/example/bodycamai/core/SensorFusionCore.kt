package com.example.bodycamai.core

/**
 * Central state model for the EagleEye-inspired sensor-fusion layer.
 * It only combines data that is actually supplied by connected sensors/modules.
 */
enum class VisionMode(val title: String) {
    AUTO("Авто"), DAY("День"), LOW_LIGHT("Ночь"), THERMAL("Тепло"), FUSED("Слияние")
}

enum class SensorSource(val title: String) {
    PHONE_CAMERA("Камера телефона"), REAR_CAMERA("Задняя камера"), EXTERNAL_CAMERA("Внешняя камера"), THERMAL_CAMERA("Тепловизор"),
    DRONE("Камера дрона"), XR("AR / XR"), AUDIO("Аудиосенсор")
}

data class FusionSource(
    val source: SensorSource,
    val connected: Boolean = false,
    val latencyMs: Int? = null,
    val confidence: Float = 0f
)

data class FusionState(
    val visionMode: VisionMode = VisionMode.DAY,
    val sources: List<FusionSource> = listOf(FusionSource(SensorSource.PHONE_CAMERA, true)),
    val trackingEnabled: Boolean = true,
    val worldMarkersEnabled: Boolean = true,
    val minimapEnabled: Boolean = true,
    val rearViewEnabled: Boolean = false,
    val audioDirectionEnabled: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val headingDegrees: Float? = null,
    val locationAccuracyMeters: Float? = null,
    val fusionConfidence: Float = 1f,
    val sourceStates: Map<SensorSource, FusionSource> = mapOf(
        SensorSource.PHONE_CAMERA to FusionSource(SensorSource.PHONE_CAMERA, true, confidence = 1f)
    )
) {
    val connectedCount: Int get() = sources.count { it.connected }
    fun source(source: SensorSource): FusionSource = sourceStates[source] ?: FusionSource(source)
    val thermalAvailable: Boolean get() = sources.any { it.source == SensorSource.THERMAL_CAMERA && it.connected }
    val effectiveVisionMode: VisionMode get() = when (visionMode) {
        VisionMode.AUTO -> if (thermalAvailable) VisionMode.FUSED else VisionMode.DAY
        else -> visionMode
    }
    val externalVideoAvailable: Boolean get() = sources.any {
        it.connected && it.source in setOf(SensorSource.EXTERNAL_CAMERA, SensorSource.DRONE, SensorSource.THERMAL_CAMERA)
    }
}
