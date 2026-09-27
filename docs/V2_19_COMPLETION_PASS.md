# BodyCam AI v2.19 — sensor runtime completion pass

This pass moves the Sensor Fusion layer closer to runtime hardware use.

## Added
- per-source runtime health (`connected`, frame age, estimated FPS, confidence, latency);
- stale-frame detection for Fusion;
- reset of timing state when a source is removed;
- common `SensorAdapter` contract for real camera/thermal/drone/XR/audio integrations;
- external Camera2/CameraX selection remains limited to devices that Android actually exposes as `LENS_FACING_EXTERNAL`.

## Boundary
This does not claim arbitrary UVC, thermal, drone, or XR compatibility. Those require a concrete transport/SDK adapter. The adapter contract is intentionally ready for those implementations.
