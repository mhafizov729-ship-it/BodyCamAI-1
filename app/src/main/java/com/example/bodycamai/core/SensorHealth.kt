package com.example.bodycamai.core

/** Runtime health snapshot for one sensor source. */
data class SensorHealth(
    val source: SensorSource,
    val connected: Boolean,
    val lastFrameAgeMs: Long? = null,
    val fps: Float? = null,
    val confidence: Float = 0f,
    val latencyMs: Long? = null
)
