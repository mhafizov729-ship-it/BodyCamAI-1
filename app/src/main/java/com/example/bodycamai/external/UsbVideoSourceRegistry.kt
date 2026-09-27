package com.example.bodycamai.external

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

/** Discovers USB devices only; it does not pretend arbitrary USB cameras are UVC streams. */
class UsbVideoSourceRegistry(private val usbManager: UsbManager) {
    fun discover(): List<ExternalVideoSource> = usbManager.deviceList.values.map { device ->
        ExternalVideoSource(
            id = "usb:${device.vendorId}:${device.productId}:${device.deviceId}",
            name = device.productName ?: "USB device ${device.deviceId}",
            kind = ExternalSourceKind.USB_UVC,
            state = if (usbManager.hasPermission(device)) StreamState.DISCOVERED else StreamState.PERMISSION_REQUIRED
        )
    }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)
}
