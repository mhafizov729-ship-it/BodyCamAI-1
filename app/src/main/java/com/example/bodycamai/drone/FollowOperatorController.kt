package com.example.bodycamai.drone

/**
 * Safety-gated follow controller. It never generates navigation coordinates itself;
 * a vendor adapter is responsible for translating an approved high-level command.
 */
class FollowOperatorController(private val manager: DroneControlManager, private val gate: FollowSafetyGate = FollowSafetyGate()) {
    fun requestFollow(input: FollowSafetyGate.Input): Result<Unit> {
        if (!gate.allow(input)) return Result.failure(IllegalStateException("Follow safety conditions not met"))
        return manager.send(DroneCommand.FOLLOW_OPERATOR)
    }

    fun requestGpsFollow(input: FollowSafetyGate.Input): Result<Unit> {
        if (!gate.allow(input)) return Result.failure(IllegalStateException("GPS follow safety conditions not met"))
        return manager.send(DroneCommand.GPS_FOLLOW)
    }

    fun stop(): Result<Unit> = manager.emergencyStop()
}
