# BodyCam AI v2.20 — USB sensor/runtime pass

- Removed the Sensor screen's fake ON/OFF toggles.
- Sensor availability is now distinguished from a live Fusion source.
- Added Android USB inventory with vendor/product IDs and permission state.
- Added a real Android USB permission request helper.
- Added a non-exported permission-result receiver.
- External Camera remains Camera2/CameraX based; an external device is only LIVE after a real camera frame reaches Fusion.
- Thermal USB detection remains a hint only; no thermal stream is fabricated without a vendor/driver adapter.
- Rear camera is no longer counted as an active Fusion stream merely because the phone has a rear camera.
