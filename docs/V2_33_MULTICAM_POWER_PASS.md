# BodyCam AI v2.33 — Multi-camera + Power/thermal optimization

Implemented foundation:
- runtime battery/charging/power-save telemetry;
- Android thermal status monitoring on API 29+;
- adaptive policy combines memory, battery, power-save and thermal state;
- conservative AI interval/overlay limits under thermal or power pressure;
- vendor-neutral multi-camera state model;
- explicit distinction between active camera and actually streaming camera;
- simultaneous multi-camera is reported only when the device/API says it is supported.

Not claimed as complete:
- vendor-specific concurrent camera pipelines;
- synchronized multi-camera capture;
- arbitrary UVC multi-camera streaming;
- background monitoring service.
These require device-specific Camera2/UVC capabilities and further integration.
