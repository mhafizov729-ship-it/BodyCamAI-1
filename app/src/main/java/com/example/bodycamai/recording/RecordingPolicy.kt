package com.example.bodycamai.recording

data class RecordingPolicy(
    val circularRecording: Boolean = true,
    val maxRecordings: Int = 50,
    val preEventSeconds: Int = 10,
    val autoProtectEvents: Boolean = true
) {
    init {
        require(maxRecordings in 1..500)
        require(preEventSeconds in 0..60)
    }
}
