package com.example.bodycamai.sensors.adapters

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbManager
import com.example.bodycamai.core.SensorSource
import com.example.bodycamai.core.FusionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hardware discovery layer. It deliberately reports only hardware that Android can
 * actually enumerate; it never fakes a thermal/drone source.
 */
class ExternalSensorDiscovery(private val context: Context) {
    private val _sources = MutableStateFlow<Map<SensorSource, FusionSource>>(emptyMap())
    private val usbRegistry = UsbDeviceRegistry(context)
    val sources: StateFlow<Map<SensorSource, FusionSource>> = _sources.asStateFlow()

    fun scan(): Map<SensorSource, FusionSource> {
        val result = linkedMapOf<SensorSource, FusionSource>()
        result[SensorSource.PHONE_CAMERA] = FusionSource(SensorSource.PHONE_CAMERA, true, confidence = 1f)

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        var externalCamera = false
        if (cameraManager != null) {
            runCatching {
                cameraManager.cameraIdList.forEach { id ->
                    val c = cameraManager.getCameraCharacteristics(id)
                    if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                        externalCamera = true
                    }
                }
            }
        }
        result[SensorSource.REAR_CAMERA] = FusionSource(SensorSource.REAR_CAMERA, true, confidence = .95f)
        result[SensorSource.EXTERNAL_CAMERA] = FusionSource(
            SensorSource.EXTERNAL_CAMERA, externalCamera, confidence = if (externalCamera) .95f else 0f
        )

        val usb = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val usbDevices = usb?.deviceList?.values.orEmpty()
        usbRegistry.scan()
        // USB presence is only an adapter hint. Thermal capability must be confirmed by a driver/SDK; it is not marked active here.
        val thermalHint = usbDevices.any { d ->
            val text = "${d.manufacturerName} ${d.productName} ${d.deviceName}".lowercase()
            listOf("thermal", "flir", "seek", "infiray", "topdon").any(text::contains)
        }
        result[SensorSource.THERMAL_CAMERA] = FusionSource(
            SensorSource.THERMAL_CAMERA, false, confidence = if (thermalHint) .35f else 0f
        )
        result[SensorSource.DRONE] = FusionSource(SensorSource.DRONE, false)
        result[SensorSource.XR] = FusionSource(SensorSource.XR, false)
        result[SensorSource.AUDIO] = FusionSource(SensorSource.AUDIO, true, confidence = .9f)

        _sources.value = result
        return result
    }
}
