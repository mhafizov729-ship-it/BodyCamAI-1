# BodyCam AI — Android 10+ universal prototype

Universal BodyCam-first Android application architecture. Minimum Android version: API 29 (Android 10).

## Implemented in this source milestone
- Camera-first startup with CameraX.
- Separate movable transparent gear button.
- Recording/photo controls and defensive camera error handling.
- Configurable HUD screen with size/transparency controls.
- Profiles: BodyCam, Civilian, Police, Tactical, Airsoft, Training, Search & Rescue, AR/XR, Custom, All.
- Independent module toggles.
- Device manager UI for phone/external camera/mic/AR-XR/drone integrations.
- Multi-source Live View layout selector.
- Diagnostics panel.
- Offline-first AI foundation remains in OfflineAiEngine.
- Android 10 minimum.

## Planned/architected modules
- GPS/compass and orientation-following map.
- Offline/manual map downloads.
- Stereo audio direction estimate and event zone on map.
- Slow-motion/frame analysis and possible flash/event detection.
- Vehicle/plate OCR with status checks only through permitted public/official sources.
- Face detection and possible-match workflows; no automatic criminal classification.
- Drone/camera adapters through supported SDK/API, with capabilities discovered per device.
- Optional voice control and configurable energy mode.
- Search & Rescue group mode and optional SOS.
- Local encrypted archive and optional cloud sync.
- AR/XR external display/camera/sensor support.

## Safety boundaries
The app may detect and visualize objects/events, approximate directions, and provide training/observation features. It does not implement ballistic calculations, weapon targeting, autonomous weapon control, or automatic decisions to use force.

## Build status
This is a source project. The current execution environment does not contain an Android SDK/Gradle installation, so a real APK build/device test cannot be truthfully claimed here. The project should be opened in Android Studio with a compatible Android SDK/JDK 17 toolchain and built there.


## v0.8 — Command / Coordinator
- Added a dedicated Command / Coordinator profile.
- Added authorized team member model, shared tasks, map points/zones concept, route/task status and permission-ready camera access.
- Added communication UI for group/private messages and voice-message placeholder.
- Added connection-channel model: Internet, 4G/5G, local Wi-Fi and Bluetooth, with offline state.
- Internet and mobile-network channels are intended for long-distance communication when coverage/connectivity exists; Wi-Fi/Bluetooth are local/short-range channels.
- Camera access is consent/capability based.
- This version contains the coordination data/UI layer; it does not claim a working remote server, VoIP backend, or real-time multi-device transport yet.
- Tasks are represented as points/routes/zones, not weapon targets.

## v0.9 Command / Coordinator expansion
- automatic connection-channel selection: mobile > internet > local Wi-Fi > Bluetooth > offline
- group and member messaging model
- voice-message event type
- shared task creation with optional assignee and current map coordinates
- participant permission dialog for location/camera/microphone/live view
- Command center keeps local task/message state ready for a future network transport
- long-range communication is represented by internet/mobile network; Wi-Fi/Bluetooth are local channels

## v1.1 Serverless P2P / Host-Client
- Command device can act as the group host.
- No mandatory central server.
- Local discovery uses Android NSD/TCP.
- Direct Internet P2P is attempted only when the network permits inbound/reachable endpoints.
- CGNAT/carrier restrictions can block direct mobile connections; the UI must report this instead of silently using a server.
- Group state is owned by the current host for the active session.

## v1.3 secure serverless P2P
- Host/Client direct transport remains serverless.
- ECDH P-256 establishes a fresh per-connection key.
- AES-GCM protects framed payloads.
- The room code is used as an additional admission secret.
- Direct internet P2P is attempted only when the network permits inbound/reachable peer connections; CGNAT can still prevent direct connectivity.

## v1.4 — Serverless P2P synchronization
- Encrypted P2P application packets for text messages, presence and GPS coordinates.
- Host/Client can keep synchronized peer state without a central backend.
- Offline queue/network retry remains a separate transport concern.
- Direct Internet P2P still depends on NAT/CGNAT and reachable endpoints.
- Live video and voice media transport are not claimed complete until tested on two physical Android devices.

## v1.5 P2P Live View
- Added consent-based Live View request/response protocol.
- Added camera/microphone capability negotiation models.
- Kept media transport behind a replaceable interface; no fake claim of a working video stream.
- Live View UI now explains request → consent → capability negotiation → media connection.

## v1.6 Live Source Switching
- Phone camera, compatible drone camera, and external camera are separate Live View sources.
- Commander can request an allowed source, then stop it and switch back.
- Source ownership/consent remains required; no automatic camera activation.

## v1.7 — P2P Voice + Adaptive Media
- Added consent-based peer voice channel control model.
- Added adaptive media policy for weak mobile/internet links.
- Media quality can step down to preserve connectivity and return to higher quality when bandwidth improves.
- Voice is a separate channel from Live View.


## v2.1 Command Map Sync
- Shared command map objects: points, zones and routes.
- P2P JSON packets for creation/deletion.
- Point types: checkpoint, event, task, SOS, custom.
- In-memory store keeps synchronized objects on each authorized device.
- This layer does not perform autonomous routing or tactical targeting.

## v2.3 integration audit / fixes
- Fixed Command Collaboration placement inside the Android app source set.
- Connected command message/task packets to P2PSync and deduplicated messages by id.
- Removed duplicate ACCESS_NETWORK_STATE manifest permission.
- Fixed command message/task codec to JSON so tabs/newlines/special text are preserved safely.
- Separated Voice UI from Live View UI; voice UI no longer falsely claims an active media channel.
- Live View layout selector now has real local selection state instead of no-op buttons.
- Static source checks: 27 Kotlin files; balanced braces/parentheses; one TaskStatus declaration.
- Full Android build/runtime verification remains pending until an Android SDK/Gradle toolchain and physical-device test are available.
- Fixed Live View layout selector no-op buttons; selected layout is now retained in UI state.
- Final static audit after fixes: no brace/parenthesis balance issues; no duplicate TaskStatus; no duplicate ACCESS_NETWORK_STATE; no no-op Live View handlers.

## v2.4 Archive ↔ AI event integration
- Added a shared `captureId` for BodyCam video sessions.
- AI events now store the active `captureId` together with timestamp/GPS.
- Archive items expose the parsed capture ID and count linked AI events.
- Archive text search also checks OCR stored in linked AI events.
- Existing recordings remain readable; old events without a capture ID are preserved.
- Runtime media playback/frame-jump is intentionally not claimed complete until Android build/device testing is available.

## v2.5 — Batch integration
- Command Map packets are now routed through the common encrypted P2P sync codec/store.
- CommandMapStore snapshots are synchronized for concurrent P2P updates.
- ArchiveFilter supports `onlyWithAiEvents` in addition to text/type filtering.
- Archive UI exposes an AI-events-only filter and shows captureId for linked recordings.
- LiveViewController provides explicit request/accept/connect/live/end/reset lifecycle without auto-activating remote cameras.
- LiveQualityPolicy provides conservative network/thermal/battery quality profiles for a future real media transport.
- No claim is made that WebRTC/real media transport is complete; that still requires Android runtime/device testing.

## v2.6 — Live Media Transport Foundation
- Added `SecureLiveMediaTransport` for framed H.264 packets over an already-authorized AES-GCM P2P session.
- Added `H264LiveEncoder` and `H264LiveDecoder` helpers using Android MediaCodec.
- Media transport remains separate from consent/control; no remote camera is activated automatically.
- Actual two-device camera-to-camera runtime still requires Android SDK/device testing and wiring the encoder input surface to the camera pipeline.


## v2.8 development status

- LiveMediaSender now uses a bounded FIFO coroutine channel to preserve frame order and avoid one coroutine per video frame.
- Media transport rejects frames after the channel is stopped and permits explicit EOS packets.
- LiveMediaReceiver closes its callback only once when the peer disconnects.
- RuntimeStatus provides local UI state for AI connectivity, latency, request/context counters and protection state; unknown remote values are not presented as healthy.

The next integration step is to connect the existing consent state machine to an actual SecurePeerSession and expose a real sender/receiver test flow between two Android devices.
