package com.example.bodycamai.external

import android.view.View

/**
 * Media-engine boundary for external network video.
 * Implementations must report STREAMING only after a decoded/rendered video frame exists.
 */
interface ExternalMediaEngine {
    val source: ExternalVideoSource
    val view: View?

    fun prepare(): Result<Unit>
    fun play(): Result<Unit>
    fun pause()
    fun stop()
    fun release()
}
