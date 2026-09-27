# BodyCam AI v2.18 — completion pass

## Foundation
- External camera selection remains OS/CameraX based; arbitrary USB devices are not falsely reported as cameras.
- Sensor Fusion remains timestamp/confidence based.
- Drone control is vendor-neutral and requires a real manufacturer SDK/API adapter.
- Movement commands now pass through an explicit confirmation gate; emergency stop bypasses confirmation.
- Fixed the drone command connectivity condition so normal commands require a connected adapter while STOP remains available to the manager.
- Language packs have a local data-only registry with size/basic-schema validation.

## Add-ons
Vendor SDKs, UVC fallback drivers, thermal SDKs, RTSP/WebRTC stacks and XR runtimes must be integrated only when their actual APIs/dependencies are available. The project does not claim these integrations are complete merely because an adapter interface exists.
