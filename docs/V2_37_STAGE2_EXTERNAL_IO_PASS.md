# v2.37 — Stage 2: external I/O foundation

Implemented the first Stage 2 package on top of v2.36.

## Included
- USB Video Class (UVC) interface probing on Android-visible USB devices.
- Detection of video control/streaming interfaces and bulk/isochronous endpoints.
- Explicit separation between **UVC detected** and **video stream active**.
- Runtime state machine for RTSP/WebRTC descriptors with frame-driven health state.
- Stale-stream detection based on last decoded frame timestamp.
- Validated thermal frame model for real SDK/UVC adapters.

## Important boundary
This package does not pretend to be a UVC decoder, RTSP player, WebRTC stack, or thermal vendor SDK. Those components require a compatible media/USB library or manufacturer SDK and device-specific testing. The app now has a clean adapter boundary for attaching them without reporting fake connectivity.
