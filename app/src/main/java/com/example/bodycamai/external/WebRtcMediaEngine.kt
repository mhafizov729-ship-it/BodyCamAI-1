package com.example.bodycamai.external

import android.view.View

/**
 * WebRTC adapter boundary. A concrete WebRTC SDK implementation can attach here.
 * The app intentionally does not mark a WebRTC source STREAMING until the SDK supplies
 * decoded frames through onDecodedFrame().
 */
interface WebRtcMediaEngine : ExternalMediaEngine {
    fun onDecodedFrame(timestampMs: Long, width: Int, height: Int): ExternalStreamController.Runtime
}

abstract class BaseWebRtcMediaEngine(
    final override val source: ExternalVideoSource,
    private val onRuntime: (ExternalStreamController.Runtime) -> Unit = {}
) : WebRtcMediaEngine {
    private val controller = ExternalStreamController(source)

    protected fun reportDecodedFrame(timestampMs: Long, width: Int, height: Int) {
        val state = controller.onFrame(timestampMs)
        onRuntime(state.copy(source = state.source.copy(width = width, height = height)))
    }

    override fun onDecodedFrame(timestampMs: Long, width: Int, height: Int): ExternalStreamController.Runtime {
        val state = controller.onFrame(timestampMs)
        return state.copy(source = state.source.copy(width = width, height = height))
    }
}
