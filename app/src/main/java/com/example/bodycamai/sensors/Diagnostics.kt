package com.example.bodycamai.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.bodycamai.core.DiagnosticsResult

object Diagnostics {
    fun run(context: Context, aiReady: Boolean): DiagnosticsResult {
        val location = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val microphonePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val locationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val gpsProvider = location?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val networkProvider = location?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        val compass = sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null || sensors?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val storage = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val network = connectivity?.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val gps = locationPermission && (gpsProvider || networkProvider)
        val overall = cameraPermission && microphonePermission && gps && compass && storage && aiReady
        return DiagnosticsResult(cameraPermission, microphonePermission, gps, compass, storage, aiReady, network, overall)
    }
}
