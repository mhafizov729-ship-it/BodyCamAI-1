package com.example.bodycamai.fusion

import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.core.DetectionBox
import com.example.bodycamai.core.SensorSource
import com.example.bodycamai.core.SensorFusionPipeline
import kotlin.math.cos
import kotlin.math.sin

/** Combines only observed AI/GPS/heading/source data into spatial events. */
class SpatialFusionEngine(
    private val pipeline: SensorFusionPipeline,
    private val maxProjectionDistanceMeters: Float = 100f
) {
    data class SpatialEvent(
        val label: String,
        val confidence: Float,
        val source: SensorSource,
        val trackingId: Int?,
        val latitude: Double?,
        val longitude: Double?,
        val bearingDegrees: Float?,
        val distanceMeters: Float?,
        val timestampMs: Long = System.currentTimeMillis()
    )

    fun buildEvents(
        result: AiFrameResult,
        latitude: Double?,
        longitude: Double?,
        headingDegrees: Float?,
        source: SensorSource = SensorSource.PHONE_CAMERA
    ): List<SpatialEvent> {
        val frameWidth = result.frameWidth.coerceAtLeast(1)
        return result.objects.map { box ->
            val centerX = (box.bounds.left + box.bounds.right) / 2f
            val normalized = (centerX / frameWidth - 0.5f).coerceIn(-0.5f, 0.5f)
            val relativeBearing = normalized * 70f
            val absoluteBearing = headingDegrees?.let { normalize(it + relativeBearing) }
            val distance = estimateDistance(box, result.frameHeight)
            val projected = project(latitude, longitude, absoluteBearing, distance)
            SpatialEvent(box.label, box.confidence, source, box.trackingId,
                projected?.first ?: latitude, projected?.second ?: longitude,
                absoluteBearing, distance)
        }
    }

    fun healthSnapshot() = pipeline.state.value.health

    private fun estimateDistance(box: DetectionBox, frameHeight: Int): Float? {
        val h = box.bounds.height().toFloat()
        if (h <= 0f) return null
        val normalized = (h / frameHeight).coerceIn(0.01f, 1f)
        return (maxProjectionDistanceMeters * (1f - normalized)).coerceIn(1f, maxProjectionDistanceMeters)
    }

    private fun project(lat: Double?, lon: Double?, bearing: Float?, distanceMeters: Float): Pair<Double, Double>? {
        if (lat == null || lon == null || bearing == null) return null
        val earth = 6_371_000.0
        val br = Math.toRadians(bearing.toDouble())
        val dLat = distanceMeters * cos(br) / earth
        val dLon = distanceMeters * sin(br) / (earth * cos(Math.toRadians(lat)).coerceAtLeast(0.01))
        return lat + Math.toDegrees(dLat) to lon + Math.toDegrees(dLon)
    }

    private fun normalize(value: Float): Float {
        var v = value % 360f
        if (v < 0f) v += 360f
        return v
    }
}
