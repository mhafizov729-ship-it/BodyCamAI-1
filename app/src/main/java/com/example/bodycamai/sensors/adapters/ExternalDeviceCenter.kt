package com.example.bodycamai.sensors.adapters

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Single entry point for external-device discovery. It separates:
 * discovered -> permission granted -> adapter streaming.
 */
class ExternalDeviceCenter(context: Context) {
    private val registry = UsbDeviceRegistry(context)
    private val discovery = ExternalSensorDiscovery(context)

    val usbDevices: StateFlow<List<UsbDeviceInfo>> = registry.devices
    val sources = discovery.sources

    fun refresh(): DeviceSnapshot = DeviceSnapshot(
        usb = registry.scan(),
        sources = discovery.scan()
    )

    fun requestPermission(deviceName: String): Boolean {
        val device = registry.find(deviceName) ?: return false
        return registry.requestPermission(device)
    }

    data class DeviceSnapshot(
        val usb: List<UsbDeviceInfo>,
        val sources: Map<com.example.bodycamai.core.SensorSource, com.example.bodycamai.core.FusionSource>
    )
}
