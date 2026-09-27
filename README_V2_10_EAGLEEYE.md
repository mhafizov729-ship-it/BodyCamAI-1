# BodyCam AI v2.10 — EagleEye-inspired sensor fusion milestone

This milestone turns the UI concept into a stateful Sensor Fusion layer.

## Implemented
- Sensor Fusion Core now tracks every supported source independently.
- Phone camera is enabled by default.
- External camera, thermal camera, drone, AR/XR and audio source slots can be enabled/disabled from the Sensors screen.
- HUD reflects the active source count and fusion state.
- FUSED mode never invents thermal data: thermal availability is tied to the thermal source state.
- Source confidence is represented in the fusion model.
- The architecture remains adapter-ready for real USB/SDK/network devices.

## Important
The source toggles represent configured/available modules in this prototype. They do not magically create a hardware thermal camera or drone feed. Real hardware requires a corresponding Android USB driver, vendor SDK, or network adapter.

## Safety scope
The project focuses on observation, sensor fusion, mapping, AI perception and alerts. It does not implement weapon targeting, ballistic calculations, strike selection, or autonomous weapon control.

## v2.10 Fusion hardware layer
- `ExternalSensorDiscovery` enumerates Android external cameras and USB devices.
- USB thermal detection is only a capability hint; a vendor driver/SDK is still required for frames and temperature data.
- `ThermalSensorAdapter` is the vendor-neutral contract for real thermal drivers.
- `RemoteVideoSource` is the transport-neutral contract for drone/remote video adapters.
- Fusion state now merges discovered hardware with user-enabled modules and does not claim unavailable sensors are active.


## v2.13 Sensor Center / Fusion UI
- AUTO vision mode selects available thermal fusion when a real thermal adapter is connected; otherwise uses the phone camera.
- Rear camera is represented as a separate sensor slot; simultaneous dual-camera capture still depends on device Camera2 capabilities.
- Sensor Center exposes source online state, confidence, latency and synchronization.
- Remote View and directional audio are adapter-driven; the app does not fabricate unavailable hardware data.
