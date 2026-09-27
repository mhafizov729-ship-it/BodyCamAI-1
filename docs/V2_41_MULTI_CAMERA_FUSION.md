# BodyCam AI v2.41 — Multi-camera fusion

## Stage 2 / block 5

Adds timestamp-coherent coordination for real sensor frames from phone, USB/UVC, thermal and remote sources.

### Guarantees
- No synthetic frames or fake connected state.
- A source enters the synchronized set only after a real `SensorFrame` is submitted.
- Pairing uses timestamps with a configurable 120 ms default skew budget.
- Sources outside the skew window are excluded from the current synchronized snapshot.
- The coordinator is transport-neutral; CameraX/UVC/Media3/WebRTC/thermal SDK adapters remain responsible for delivering actual frames.

### API
- `MultiSourceSynchronizer` — timestamp snapshot selection.
- `MultiCameraFusionSession` — runtime state for active/synchronized sources.
