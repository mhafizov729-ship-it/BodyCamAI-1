# BodyCam AI v2.31 — Drone + PTZ control foundation

Completed as a vendor-neutral control layer.

- Added `FollowOperatorController` with GPS freshness, target visibility, link-quality, distance and height safety gates.
- Follow/GPS Follow only emits a high-level command after safety checks; it does not calculate autonomous navigation or targeting solutions.
- Emergency stop remains available through the drone manager.
- Added PTZ camera control contract with pan/tilt/zoom/center/record commands.
- PTZ movement can require explicit confirmation and is rejected when no compatible adapter is connected.
- Real drone manufacturer SDKs and real PTZ protocols still require device-specific adapters.
