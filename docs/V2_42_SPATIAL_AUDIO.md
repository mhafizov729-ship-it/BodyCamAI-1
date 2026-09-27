# BodyCam AI v2.42 — Spatial Audio

Stage 2 / block 6.

Adds a lightweight stereo `AudioRecord` analyzer for Android 10+.
It calculates left/right RMS energy, stereo balance, channel correlation,
and an approximate LEFT/CENTER/RIGHT direction tendency.

This is not acoustic localization: no exact bearing, distance, triangulation,
or gunshot localization is claimed. Accurate spatial localization requires
known microphone geometry/calibration and appropriate DSP/sensor fusion.

The analyzer emits `SpatialAudioResult`, which can be consumed by the event
center and sensor-fusion layer together with timestamps/GPS/compass data.
Permission remains controlled by the existing `RECORD_AUDIO` runtime flow.
