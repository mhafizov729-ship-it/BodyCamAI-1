package com.example.bodycamai.core

import android.content.Context

/** Small local settings store. No account or cloud sync is required. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("bodycam_ai_settings", Context.MODE_PRIVATE)

    fun aiEnabled(): Boolean = prefs.getBoolean("ai_enabled", true)
    fun mapEnabled(): Boolean = prefs.getBoolean("map_enabled", true)
    fun frontCamera(): Boolean = prefs.getBoolean("front_camera", false)
    fun uiTheme(): UiThemePreset = runCatching { UiThemePreset.valueOf(prefs.getString("ui_theme", UiThemePreset.EAGLEEYE.name) ?: UiThemePreset.EAGLEEYE.name) }.getOrDefault(UiThemePreset.EAGLEEYE)

    fun aiAlertsEnabled(): Boolean = prefs.getBoolean("ai_alerts_enabled", true)
    fun aiMinConfidence(): Float = prefs.getFloat("ai_min_confidence", 0.45f)
    fun aiLabelsEnabled(): Boolean = prefs.getBoolean("ai_labels_enabled", true)
    fun aiContourOnly(): Boolean = prefs.getBoolean("ai_contour_only", false)
    fun aiAlertCooldownMs(): Long = prefs.getLong("ai_alert_cooldown_ms", 2500L)

    fun profile(): AppProfile = runCatching {
        AppProfile.valueOf(prefs.getString("profile", AppProfile.BODYCAM.name) ?: AppProfile.BODYCAM.name)
    }.getOrDefault(AppProfile.BODYCAM)

    fun setAiEnabled(value: Boolean) = prefs.edit().putBoolean("ai_enabled", value).apply()
    fun setMapEnabled(value: Boolean) = prefs.edit().putBoolean("map_enabled", value).apply()
    fun setFrontCamera(value: Boolean) = prefs.edit().putBoolean("front_camera", value).apply()
    fun setUiTheme(value: UiThemePreset) = prefs.edit().putString("ui_theme", value.name).apply()
    fun setProfile(value: AppProfile) = prefs.edit().putString("profile", value.name).apply()
    fun setAiAlertsEnabled(value: Boolean) = prefs.edit().putBoolean("ai_alerts_enabled", value).apply()
    fun setAiMinConfidence(value: Float) = prefs.edit().putFloat("ai_min_confidence", value.coerceIn(0f, 1f)).apply()
    fun setAiLabelsEnabled(value: Boolean) = prefs.edit().putBoolean("ai_labels_enabled", value).apply()
    fun setAiContourOnly(value: Boolean) = prefs.edit().putBoolean("ai_contour_only", value).apply()
    fun setAiAlertCooldownMs(value: Long) = prefs.edit().putLong("ai_alert_cooldown_ms", value.coerceIn(500L, 30000L)).apply()

    fun moduleEnabled(module: Module, defaultValue: Boolean): Boolean =
        prefs.getBoolean("module_${module.name}", defaultValue)

    fun setModuleEnabled(module: Module, value: Boolean) =
        prefs.edit().putBoolean("module_${module.name}", value).apply()
}
