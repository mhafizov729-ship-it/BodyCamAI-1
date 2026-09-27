package com.example.bodycamai.arxr

class SpatialHudProjector(private val horizontalFovDegrees: Float = 70f, private val verticalFovDegrees: Float = 50f) {
    data class Point(val x: Float, val y: Float)
    fun project(bearingDegrees: Float, elevationDegrees: Float = 0f): Point? {
        if (!bearingDegrees.isFinite() || !elevationDegrees.isFinite()) return null
        if (bearingDegrees !in -horizontalFovDegrees / 2f..horizontalFovDegrees / 2f) return null
        if (elevationDegrees !in -verticalFovDegrees / 2f..verticalFovDegrees / 2f) return null
        return Point((0.5f + bearingDegrees / horizontalFovDegrees).coerceIn(0f, 1f), (0.5f - elevationDegrees / verticalFovDegrees).coerceIn(0f, 1f))
    }
}
