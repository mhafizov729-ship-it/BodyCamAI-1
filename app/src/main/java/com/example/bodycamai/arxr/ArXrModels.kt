package com.example.bodycamai.arxr

enum class ArXrMode { OFF, HUD_OVERLAY, SPATIAL_MARKERS, PASSTHROUGH }
enum class ArXrTrackingState { UNAVAILABLE, INITIALIZING, TRACKING, LIMITED }

data class ArXrCapabilities(val supported: Boolean, val sixDofTracking: Boolean, val passthrough: Boolean, val depth: Boolean, val externalDisplay: Boolean)
data class SpatialMarker(val id: String, val label: String, val bearingDegrees: Float? = null, val distanceMeters: Float? = null, val confidence: Float = 0f, val source: String = "fusion")
data class ArXrFrame(val timestampNanos: Long, val trackingState: ArXrTrackingState, val markers: List<SpatialMarker> = emptyList())
