package com.example.bodycamai.arxr

interface ArXrAdapter {
    fun capabilities(): ArXrCapabilities
    fun start(): Result<Unit>
    fun stop()
    fun submitFusionFrame(frame: ArXrFrame)
    fun trackingState(): ArXrTrackingState
}

class UnavailableArXrAdapter : ArXrAdapter {
    override fun capabilities() = ArXrCapabilities(false, false, false, false, false)
    override fun start(): Result<Unit> = Result.failure(UnsupportedOperationException("AR/XR adapter is not installed"))
    override fun stop() = Unit
    override fun submitFusionFrame(frame: ArXrFrame) = Unit
    override fun trackingState() = ArXrTrackingState.UNAVAILABLE
}
