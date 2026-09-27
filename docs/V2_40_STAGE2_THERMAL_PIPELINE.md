# BodyCam AI v2.40 — Stage 2 Thermal Pipeline

## Completed
- Added a real thermal-frame data contract for frames delivered by a USB/UVC or manufacturer SDK adapter.
- Added a generic 16-bit little-endian radiometric decoder with adapter-supplied scale/offset calibration.
- Added ThermalFusionBridge, which feeds timestamped thermal frames into SensorFusionPipeline.
- Thermal is considered streaming only after a real thermal frame is submitted.
- Thermal errors/stop clear the thermal source from fusion state.
- Temperature min/max are exposed as measured values only when the upstream adapter supplies calibrated radiometric data.

## Important hardware boundary
This package does not claim universal thermal hardware support. A specific camera still requires a transport/SDK adapter that supplies real frames and, for radiometric temperatures, the manufacturer's calibration mapping.
