package com.example.bodycamai.drone

/** Vendor-neutral control contract. Real movement requires a manufacturer SDK/API adapter. */
interface DroneControlAdapter {
    val name: String
    val connected: Boolean
    fun execute(command: DroneCommand): Result<Unit>
    fun emergencyStop(): Result<Unit>
}

enum class DroneCommand { FOLLOW_OPERATOR, GPS_FOLLOW, HOLD_POSITION, RETURN_HOME, STOP_FOLLOW, CAMERA_FOLLOW, ORBIT }

enum class FollowMode { OFF, FOLLOW_OPERATOR, CAMERA_FOLLOW, GPS_FOLLOW, ORBIT }

data class DroneSafetyLimits(
    val maxDistanceMeters: Float = 30f,
    val maxHeightMeters: Float = 20f,
    val stopOnLinkLoss: Boolean = true,
    val requireConfirmationForMovement: Boolean = true
)

data class DroneControlState(
    val connected: Boolean = false,
    val followMode: FollowMode = FollowMode.OFF,
    val distanceMeters: Float? = null,
    val heightMeters: Float? = null,
    val batteryPercent: Int? = null,
    val linkQualityPercent: Int? = null,
    val safetyLimits: DroneSafetyLimits = DroneSafetyLimits()
)

class DroneControlManager(private val adapterProvider: () -> DroneControlAdapter?) {
    /** Movement commands must pass through adapter-specific safety checks. */
    fun canExecute(command: DroneCommand): Boolean =
        command == DroneCommand.STOP_FOLLOW || adapterProvider()?.connected == true

    fun send(command: DroneCommand): Result<Unit> = adapterProvider()?.execute(command)
        ?: Result.failure(IllegalStateException("No compatible drone control adapter connected"))
    fun emergencyStop(): Result<Unit> = adapterProvider()?.emergencyStop()
        ?: Result.failure(IllegalStateException("No compatible drone control adapter connected"))
}
