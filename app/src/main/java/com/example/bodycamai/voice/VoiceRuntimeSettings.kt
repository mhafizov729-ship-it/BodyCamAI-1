package com.example.bodycamai.voice

/** User-controlled voice layer. Recognition/execution remains opt-in and separate. */
data class VoiceRuntimeSettings(
    val enabled: Boolean = false,
    val alwaysListening: Boolean = false,
    val wakePhraseEnabled: Boolean = true,
    val wakePhrase: String = "BodyCam",
    val recognitionLanguage: String = "auto",
    val confirmationRequired: Boolean = true,
    val readySoundEnabled: Boolean = true,
    val confirmationSoundEnabled: Boolean = true,
    val dndRespect: Boolean = true
)

class WakePhraseMatcher(private val phraseProvider: () -> String) {
    fun matches(text: String): Boolean {
        val phrase = phraseProvider().trim()
        if (phrase.isEmpty()) return false
        return normalize(text).contains(normalize(phrase))
    }

    private fun normalize(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
}
