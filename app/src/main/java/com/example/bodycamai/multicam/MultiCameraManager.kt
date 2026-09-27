package com.example.bodycamai.multicam

import android.hardware.camera2.CameraCharacteristics

/**
 * Keeps the multi-camera policy independent from a vendor-specific implementation.
 * Android may expose multiple cameras, but simultaneous streaming depends on the device
 * hardware and Camera2 concurrent-camera support.
 */
data class CameraSourceState(
    val id: String,
    val label: String,
    val active: Boolean = false,
    val streaming: Boolean = false,
    val facing: Int? = null
)

data class MultiCameraState(
    val sources: List<CameraSourceState> = emptyList(),
    val simultaneousRequested: Boolean = false,
    val simultaneousSupported: Boolean = false
)

class MultiCameraManager {
    fun buildState(
        cameraIds: List<Pair<String, String>>,
        activeIds: Set<String> = emptySet(),
        streamingIds: Set<String> = emptySet(),
        simultaneousSupported: Boolean = false
    ): MultiCameraState = MultiCameraState(
        sources = cameraIds.map { (id, label) ->
            CameraSourceState(id, label, id in activeIds, id in streamingIds)
        },
        simultaneousRequested = activeIds.size > 1,
        simultaneousSupported = simultaneousSupported
    )

    fun canUseSimultaneously(state: MultiCameraState): Boolean =
        state.simultaneousRequested && state.simultaneousSupported
}
