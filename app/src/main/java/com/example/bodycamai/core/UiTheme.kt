package com.example.bodycamai.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BodyCamColors(
    val background: Color,
    val panel: Color,
    val panel2: Color,
    val primary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val text: Color = Color.White,
    val muted: Color = Color(0xFF8D99A6)
)

enum class UiThemePreset { EAGLEEYE, NIGHT_VISION, AMBER, MONO }

fun uiTheme(preset: UiThemePreset): BodyCamColors = when (preset) {
    UiThemePreset.EAGLEEYE -> BodyCamColors(Color.Black, Color(0xE611171D), Color(0xD91C252D), Color(0xFF00E5FF), Color(0xFF63FF8B), Color(0xFFFFC857), Color(0xFFFF5B62))
    UiThemePreset.NIGHT_VISION -> BodyCamColors(Color.Black, Color(0xE60B160D), Color(0xD9152718), Color(0xFF7CFF6B), Color(0xFFB5FF9D), Color(0xFFE5E05A), Color(0xFFFF5B5B))
    UiThemePreset.AMBER -> BodyCamColors(Color.Black, Color(0xE61A1208), Color(0xD9281C0F), Color(0xFFFFB300), Color(0xFF8CFF9A), Color(0xFFFFD166), Color(0xFFFF5B62))
    UiThemePreset.MONO -> BodyCamColors(Color.Black, Color(0xE6141414), Color(0xD9252525), Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFFE0E0E0), Color(0xFFFF5B62))
}

val LocalBodyCamColors = staticCompositionLocalOf { uiTheme(UiThemePreset.EAGLEEYE) }
