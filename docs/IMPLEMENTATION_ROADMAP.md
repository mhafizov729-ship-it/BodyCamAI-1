# BodyCam AI — два этапа

## Этап 1 — ОСНОВА
- CameraX phone/front/back camera
- External Camera / Camera2 external source
- USB discovery and permissions
- Sensor Fusion Core + timestamp synchronization
- Sensor Center: source health, FPS/latency/confidence
- Day / Low Light / Thermal / Fused modes (thermal only when a real compatible source exists)
- GPS/compass/map/HUD
- Local AI pipeline and event/archive foundations
- Universal localization architecture with AUTO language and downloadable/user-pack extension point
- Language-independent voice-command model
- Vendor-neutral drone/PTZ control adapter contracts
- Safety defaults: movement confirmation, stop command, link-loss handling, bounded follow modes

## Этап 2 — ДОПОЛНЕНИЯ
- Real UVC fallback where Android does not expose a camera through Camera2
- Vendor SDK adapters for supported drones/PTZ cameras
- Follow-operator integration through a supported flight-control API
- Real remote video transport (RTSP/WebRTC/vendor SDK) into Fusion
- Real thermal SDK adapters and calibration metadata
- Multi-camera synchronization and PiP/remote view
- Spatial audio with compatible multi-microphone hardware
- Download/install custom language packs and voice models
- AR/XR device adapters
- Offline map package manager and custom map servers
- Advanced battery/thermal/performance policies
- Home widget / Quick Settings / persistent notification controls

## v2.19 runtime sensor pass
- Sensor Fusion now reports live frame age/FPS/confidence/latency per source.
- Hardware adapters use a common runtime contract.
- Unsupported hardware remains explicitly unavailable rather than simulated.
