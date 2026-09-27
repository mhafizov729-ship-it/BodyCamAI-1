package com.example.bodycamai.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/** Web-Mercator helpers for offline/online tile addressing. */
object MapTileMath {
    data class Tile(val x: Int, val y: Int, val z: Int)

    fun tileFor(latitude: Double, longitude: Double, zoom: Int): Tile {
        val z = zoom.coerceIn(0, 24)
        val lat = latitude.coerceIn(-85.05112878, 85.05112878)
        val n = 2.0.pow(z)
        val x = floor((longitude + 180.0) / 360.0 * n).toInt().coerceIn(0, n.toInt() - 1)
        val y = floor((1.0 - ln(tan(Math.toRadians(lat)) + 1.0 / cos(Math.toRadians(lat))) / PI) / 2.0 * n).toInt().coerceIn(0, n.toInt() - 1)
        return Tile(x, y, z)
    }

    fun groundResolution(latitude: Double, zoom: Int): Double {
        val earth = 40075016.686
        return earth * cos(Math.toRadians(latitude.coerceIn(-85.05112878, 85.05112878))) / (256.0 * 2.0.pow(zoom.coerceIn(0, 24)))
    }

    private fun Double.pow(exp: Int): Double = java.lang.Math.pow(this, exp.toDouble())
}
