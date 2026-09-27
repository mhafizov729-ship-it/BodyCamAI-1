package com.example.bodycamai.sensors.adapters

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice

/**
 * Detects whether an Android-visible USB device exposes a USB Video Class interface.
 * Detection alone never starts a stream; a real UVC decoder/driver must still be attached.
 */
object UvcDeviceProbe {
    const val VIDEO_CLASS = 0x0E
    const val VIDEO_CONTROL_SUBCLASS = 0x01
    const val VIDEO_STREAMING_SUBCLASS = 0x02

    data class Result(
        val isUvc: Boolean,
        val hasControlInterface: Boolean,
        val hasStreamingInterface: Boolean,
        val interfaceCount: Int
    )

    fun inspect(device: UsbDevice): Result {
        var control = false
        var streaming = false
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass != VIDEO_CLASS) continue
            when (intf.interfaceSubclass) {
                VIDEO_CONTROL_SUBCLASS -> control = true
                VIDEO_STREAMING_SUBCLASS -> streaming = true
            }
        }
        return Result(
            isUvc = control || streaming,
            hasControlInterface = control,
            hasStreamingInterface = streaming,
            interfaceCount = device.interfaceCount
        )
    }

    fun looksLikeVideoEndpoint(device: UsbDevice): Boolean =
        (0 until device.interfaceCount).any { index ->
            val intf = device.getInterface(index)
            intf.interfaceClass == VIDEO_CLASS &&
                intf.interfaceSubclass == VIDEO_STREAMING_SUBCLASS &&
                (0 until intf.endpointCount).any { ep ->
                    intf.getEndpoint(ep).type == UsbConstants.USB_ENDPOINT_XFER_ISOC ||
                        intf.getEndpoint(ep).type == UsbConstants.USB_ENDPOINT_XFER_BULK
                }
        }
}
