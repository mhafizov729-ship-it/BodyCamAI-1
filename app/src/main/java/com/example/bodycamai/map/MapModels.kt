package com.example.bodycamai.map

enum class MapMode(val title: String) {
    TOP_DOWN("2D"), SATELLITE("Спутник"), HYBRID("Гибрид"), DARK("Тёмная"), NAVIGATION("Навигация"), AR("AR")
}

data class OfflineRegion(
    val id: String,
    val name: String,
    val sizeMb: Int,
    val downloaded: Boolean = false,
    val progress: Int = 0,
    val paused: Boolean = false
)
