# BodyCam AI completion status

## Stage 1 — Foundation
- CameraX phone camera and Android external-camera path
- Sensor Fusion contracts and timestamped pipeline
- AI modules, archive/events/diagnostics
- GPS/map/telemetry
- configurable HUD themes
- modular localization and language-independent device-command model
- vendor-neutral drone/PTZ command contracts

## Stage 2 — Add-ons
- UVC fallback contract; actual capture requires a compatible Android UVC driver/SDK
- thermal adapters require vendor SDK/USB protocol
- drone follow/orbit/GPS-follow require manufacturer SDK/API and safety limits
- remote video requires RTSP/WebRTC/vendor transport adapter
- AR/XR requires device SDK

A feature is marked connected only when a real adapter reports a compatible device. The app never fabricates a sensor, drone, thermal stream, or remote camera.
