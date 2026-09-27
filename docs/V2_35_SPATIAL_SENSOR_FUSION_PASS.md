# BodyCam AI v2.35 — Spatial Sensor Fusion

Completed foundation for the unified fusion layer.

- Converts observed AI detections into spatial events.
- Uses GPS and heading when actually available.
- Keeps source and tracking ID attached to events.
- Produces an estimated bearing from the image position.
- Projects an approximate map coordinate only when GPS + heading are present.
- Exposes current SensorFusionPipeline health for HUD/diagnostics.
- Does not claim wall penetration, exact ranging, or targeting.
- Distance is explicitly an estimate and must not be treated as measurement.

This stage is a foundation; accurate world coordinates require device calibration, depth/stereo/range sensors or external source metadata.
