package com.example.bodycamai.sensors.adapters

/**
 * Contract for a real UVC implementation supplied by a compatible driver/SDK.
 * Android CameraX remains the primary path; this contract prevents fake hardware support.
 */
interface UvcFallbackAdapter {
    val deviceName: String
    val connected: Boolean
    val width: Int
    val height: Int
    val fps: Int
    fun start(): Result<Unit>
    fun stop()
    fun frameConsumer(consumer: (timestampNs: Long, data: ByteArray) -> Unit)
}
