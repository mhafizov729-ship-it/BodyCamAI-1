package com.example.bodycamai.network

import android.content.Context
import java.util.UUID

object DeviceIdentity {
    fun get(context: Context): String {
        val prefs = context.getSharedPreferences("bodycam_identity", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }
}
