package com.example.bodycamai.sensors.adapters

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enumerates USB devices exposed by Android and requests user permission when needed.
 * It does not claim that a device is a camera/thermal sensor until a real adapter opens it.
 */
data class UsbDeviceInfo(
    val deviceId: Int,
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?,
    val hasPermission: Boolean
)

class UsbDeviceRegistry(private val context: Context) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.example.bodycamai.USB_PERMISSION"
    }

    private val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val _devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val devices: StateFlow<List<UsbDeviceInfo>> = _devices.asStateFlow()

    fun scan(): List<UsbDeviceInfo> {
        val list = manager?.deviceList?.values.orEmpty().map { d ->
            UsbDeviceInfo(
                deviceId = d.deviceId,
                deviceName = d.deviceName,
                vendorId = d.vendorId,
                productId = d.productId,
                manufacturer = d.manufacturerName,
                product = d.productName,
                hasPermission = manager?.hasPermission(d) == true
            )
        }.sortedBy { it.deviceName }
        _devices.value = list
        return list
    }

    fun requestPermission(device: UsbDevice): Boolean {
        val usb = manager ?: return false
        if (usb.hasPermission(device)) return true
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val intent = PendingIntent.getBroadcast(
            context,
            device.deviceId,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            flags
        )
        usb.requestPermission(device, intent)
        return false
    }

    fun find(deviceName: String): UsbDevice? = manager?.deviceList?.get(deviceName)
}
