# v2.39 — Stage 2: UVC frame bridge

## Completed
- Added `UvcFrameRouter` to route real decoded UVC JPEG frames into the bundled offline ML Kit pipeline.
- Added `OfflineAiEngine.analyze(Bitmap, rotation, callback)` so non-CameraX sources can use the same AI path.
- External source state is promoted to `STREAMING` only after an actual frame is delivered by a UVC adapter.
- Frame dimensions and timestamps are propagated to the source descriptor.
- Added frame-age heartbeat handling and an AI frame-rate throttle.
- Fixed `StreamState` values used by the runtime controller (`STALE`, `DISCONNECTED`).

## Hardware boundary
`UvcFrameRouter` does not pretend to be a universal UVC driver. A real UVC transport/decoder (vendor SDK, native UVC library, or OS-exposed external camera path) must provide decoded JPEG frame bytes through `UvcFallbackAdapter.frameConsumer()`.

## Next
- real device-specific UVC transport/decoder integrations and testing on actual USB cameras;
- then thermal SDK adapters and multi-camera synchronization.
