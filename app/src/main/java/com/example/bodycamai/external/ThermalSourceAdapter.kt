package com.example.bodycamai.external

/** Contract for real thermal SDK/UVC integrations. No thermal state is reported until frames arrive. */
interface ThermalSourceAdapter {
    fun connect(): Result<Unit>
    fun disconnect()
    fun onThermalFrame(timestampNs: Long, width: Int, height: Int, fps: Float): ExternalVideoSource
}
