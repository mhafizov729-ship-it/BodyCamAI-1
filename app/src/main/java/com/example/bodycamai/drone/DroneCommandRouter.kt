package com.example.bodycamai.drone

/**
 * Safe command gate for compatible drone adapters.
 * This class never invents hardware support and never executes movement itself.
 */
class DroneCommandRouter(private val manager: DroneControlManager) {
    private var confirmed = false

    fun confirmNextMovement() { confirmed = true }
    fun cancelConfirmation() { confirmed = false }

    fun send(command: DroneCommand, limits: DroneSafetyLimits): Result<Unit> {
        if (command == DroneCommand.STOP_FOLLOW) return manager.emergencyStop()
        if (!manager.canExecute(command)) return Result.failure(IllegalStateException("Drone is not connected"))
        if (limits.requireConfirmationForMovement && !confirmed) {
            return Result.failure(IllegalStateException("Movement confirmation required"))
        }
        confirmed = false
        return manager.send(command)
    }
}
