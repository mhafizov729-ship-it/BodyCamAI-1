package com.example.bodycamai.recording

import android.content.Context

/** Stores only lightweight local recovery state; it never fabricates a recovered video. */
class RecordingRecovery(context: Context) {
    private val prefs = context.getSharedPreferences("recording_recovery", Context.MODE_PRIVATE)
    fun markStarted(captureId: String) = prefs.edit().putString("active_capture", captureId).apply()
    fun markFinished() = prefs.edit().remove("active_capture").apply()
    fun interruptedCaptureId(): String? = prefs.getString("active_capture", null)
}
