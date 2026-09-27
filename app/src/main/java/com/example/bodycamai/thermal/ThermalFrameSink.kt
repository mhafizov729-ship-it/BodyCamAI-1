package com.example.bodycamai.thermal

/** Callback contract used by USB/UVC or manufacturer SDK adapters. */
fun interface ThermalFrameSink {
    fun onFrame(frame: ThermalFrame)
}
