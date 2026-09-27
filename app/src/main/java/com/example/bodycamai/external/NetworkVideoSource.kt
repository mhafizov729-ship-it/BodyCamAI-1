package com.example.bodycamai.external

/** Vendor-neutral external video source descriptors. A descriptor is not a live stream until frames arrive. */
enum class ExternalSourceKind { USB_UVC, RTSP, WEBRTC, THERMAL, NETWORK_CAMERA }
enum class StreamState { DISCOVERED, PERMISSION_REQUIRED, CONNECTING, STREAMING, STALE, STOPPED, DISCONNECTED, ERROR }

data class ExternalVideoSource(
    val id: String,
    val name: String,
    val kind: ExternalSourceKind,
    val state: StreamState = StreamState.DISCOVERED,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Float? = null,
    val thermal: Boolean = false,
    val lastFrameTimestampNs: Long? = null,
    val error: String? = null,
    val uri: String? = null
) {
    val isActuallyStreaming: Boolean
        get() = state == StreamState.STREAMING && lastFrameTimestampNs != null
}
