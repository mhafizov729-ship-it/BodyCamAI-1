package com.example.bodycamai.sensors.adapters

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives the Android USB permission result; adapters re-scan their capabilities afterwards. */
class UsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == UsbDeviceRegistry.ACTION_USB_PERMISSION) {
            // No device is marked live here. A concrete adapter must open and stream it first.
        }
    }
}
