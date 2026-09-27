# BodyCam AI v2.36 — Event Center + Diagnostics Export

## Completed
- Central local event journal for runtime and spatial events.
- Sensor-health event records with timestamp, FPS, latency and confidence.
- Human-readable diagnostics snapshot.
- Local diagnostics export under app-private storage.
- No telemetry upload is introduced by this stage.

## Notes
The event center is an append-only operational log and is not a replacement for the media archive. Spatial distance remains an estimate unless a real depth/range sensor supplies measured distance.
