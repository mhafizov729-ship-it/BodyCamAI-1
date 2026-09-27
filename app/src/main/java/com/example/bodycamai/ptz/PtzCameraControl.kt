package com.example.bodycamai.ptz

/** Vendor-neutral PTZ contract. Real movement requires a compatible camera SDK/API adapter. */
interface PtzCameraAdapter {
    val name: String
    val connected: Boolean
    fun execute(command: PtzCommand): Result<Unit>
    fun stop(): Result<Unit>
}

enum class PtzCommand {
    PAN_LEFT, PAN_RIGHT, TILT_UP, TILT_DOWN,
    ZOOM_IN, ZOOM_OUT, CENTER, START_RECORDING, STOP_RECORDING
}

data class PtzSafetyLimits(
    val maxStepDegrees: Float = 15f,
    val maxZoomStep: Int = 2,
    val requireConfirmationForMovement: Boolean = false
)

class PtzControlManager(private val adapterProvider: () -> PtzCameraAdapter?) {
    fun send(command: PtzCommand, limits: PtzSafetyLimits = PtzSafetyLimits(), confirmed: Boolean = false): Result<Unit> {
        val adapter = adapterProvider() ?: return Result.failure(IllegalStateException("No compatible PTZ camera connected"))
        if (!adapter.connected) return Result.failure(IllegalStateException("PTZ camera is not connected"))
        if (limits.requireConfirmationForMovement && !confirmed && command in setOf(PtzCommand.PAN_LEFT, PtzCommand.PAN_RIGHT, PtzCommand.TILT_UP, PtzCommand.TILT_DOWN)) {
            return Result.failure(IllegalStateException("PTZ movement confirmation required"))
        }
        return adapter.execute(command)
    }

    fun stop(): Result<Unit> = adapterProvider()?.stop()
        ?: Result.failure(IllegalStateException("No compatible PTZ camera connected"))
}
