# v2.38 — Stage 2: real RTSP media engine

Implemented on top of v2.37.

## Included
- AndroidX Media3 1.11.1 ExoPlayer integration.
- Media3 RTSP module for live/on-demand RTSP playback.
- `RtspMedia3Engine` with a real `PlayerView` output.
- Runtime state is promoted to STREAMING only after ExoPlayer reports a rendered first video frame.
- Video size/runtime metadata is propagated to the external source state.
- Playback errors move the source into ERROR instead of leaving a false connected state.
- WebRTC adapter boundary now has an explicit decoded-frame callback contract.

## RTSP support boundary
Media3 documents RTSP support for H.264 video and AAC/AC3 audio and RTP over UDP unicast or RTSP interleaved TCP. Device/camera-specific codecs and authentication still need testing.

## WebRTC
No fake WebRTC implementation is included. A real WebRTC SDK can implement `BaseWebRtcMediaEngine` and must call the decoded-frame callback for STREAMING state.
