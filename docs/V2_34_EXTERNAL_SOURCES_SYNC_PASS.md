# BodyCam AI v2.34 — External Sources + Frame Sync

Completed foundation:
- USB device discovery with Android permission state.
- Explicit distinction between discovered and actually streaming sources.
- RTSP/WebRTC source descriptors without fake connection status.
- Vendor-neutral thermal adapter contract; thermal is active only after real frames.
- Timestamp-based multi-source frame pairing with configurable skew.

Not claimed complete:
- Generic UVC decoding for every Android device.
- RTSP/WebRTC playback without a selected media engine/SDK.
- Manufacturer thermal SDK integrations.
- Hardware-specific synchronized multi-camera capture.
