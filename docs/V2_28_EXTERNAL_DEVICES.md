# BodyCam AI v2.28 — External Devices

## Completed in this pass
- Central `ExternalDeviceCenter` for USB/external-source discovery.
- USB entries now expose stable Android `deviceId` in addition to name/vendor/product.
- Permission requests remain explicit and user-mediated.
- External CameraX path continues to require Android Camera2 `LENS_FACING_EXTERNAL`.
- Thermal USB name matching is only a capability hint; it never means a thermal stream is connected.
- Remote video endpoints no longer become `connected=true` merely because an endpoint string was entered.
- A remote source becomes connected only after a concrete decoder/SDK calls `markStreaming()` with real frame dimensions/FPS.
- Remote errors explicitly drop the connected state.

## Not claimed as complete
- Generic UVC decoding on every Android device.
- Vendor thermal SDKs (FLIR/Seek/Infiray/etc.).
- RTSP/WebRTC decoder implementation.
- Manufacturer drone SDKs.

These remain adapter-specific integrations and require real hardware/SDK dependencies.
