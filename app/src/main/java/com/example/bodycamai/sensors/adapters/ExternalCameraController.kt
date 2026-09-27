package com.example.bodycamai.sensors.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

/**
 * Selects an Android-recognized removable/external camera.
 * It does not pretend that an arbitrary USB/UVC device is usable: the device
 * must be exposed by the OS through Camera2/CameraX as LENS_FACING_EXTERNAL.
 */
class ExternalCameraController(private val context: Context) {
    fun isSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)

    fun selector(provider: ProcessCameraProvider): CameraSelector? {
        if (!isSupported()) return null
        val external = provider.availableCameraInfos.firstOrNull { info ->
            runCatching {
                Camera2CameraInfo.from(info).getCameraCharacteristic(
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING
                ) == CameraCharacteristics.LENS_FACING_EXTERNAL
            }.getOrDefault(false)
        } ?: return null

        val id = Camera2CameraInfo.from(external).cameraId
        return CameraSelector.Builder().addCameraFilter { infos ->
            infos.filter { Camera2CameraInfo.from(it).cameraId == id }
        }.build()
    }
}
