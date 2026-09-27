package com.example.bodycamai.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class DeviceTelemetry(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val headingDegrees: Float? = null,
    val speedMps: Float? = null,
    val gpsReady: Boolean = false,
    val compassReady: Boolean = false
) {
    val locationText: String get() = if (latitude != null && longitude != null) "%.5f, %.5f".format(latitude, longitude) else "GPS —"
    val headingText: String get() = headingDegrees?.let { "${it.roundToInt()}°" } ?: "—"
    val speedKmh: Float? get() = speedMps?.times(3.6f)
}

class DeviceTelemetryManager(context: Context) : SensorEventListener, LocationListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _state = MutableStateFlow(DeviceTelemetry())
    val state: StateFlow<DeviceTelemetry> = _state.asStateFlow()

    fun start() {
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotation != null) {
            sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME)
            _state.value = _state.value.copy(compassReady = true)
        } else {
            _state.value = _state.value.copy(compassReady = false)
        }
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }
                if (provider != null) {
                    locationManager.requestLocationUpdates(provider, 1000L, 1f, this)
                    locationManager.getLastKnownLocation(provider)?.let { onLocationChanged(it) }
                }
            } catch (_: SecurityException) { }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        try { locationManager.removeUpdates(this) } catch (_: SecurityException) { }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotation = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotation, orientation)
        var degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (degrees < 0f) degrees += 360f
        _state.value = _state.value.copy(headingDegrees = degrees)
    }

    override fun onLocationChanged(location: Location) {
        _state.value = _state.value.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            speedMps = if (location.hasSpeed()) location.speed else null,
            gpsReady = true
        )
    }
    override fun onProviderEnabled(provider: String) { }
    override fun onProviderDisabled(provider: String) { _state.value = _state.value.copy(gpsReady = false) }
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) { }
}
