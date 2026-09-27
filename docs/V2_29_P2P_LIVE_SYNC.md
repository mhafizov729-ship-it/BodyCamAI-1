# BodyCam AI v2.29 — P2P / Live View / Sync foundation

## Completed in this stage
- LAN peer discovery using Android NSD (`_bodycamai._tcp`)
- resolution to real IP/port before connection
- explicit local peer trust store
- encrypted peer session remains ECDH + AES-GCM from v2.28
- bounded reconnect backoff for LAN connections
- explicit connection states: discovering / connecting / connected / network-blocked / offline
- adaptive media policy retained for low-bandwidth/high-latency links
- existing LiveMedia pipeline remains asynchronous so camera analysis is not blocked by network IO

## Important scope
This stage is **LAN/P2P foundation**, not a claim of universal internet NAT traversal. Internet relay/TURN/WebRTC/vendor transports still require a concrete transport implementation and server or peer-network support.

## Safety/privacy
- discovery does not authorize a peer
- trust is explicit and local
- camera sharing still requires a consented session
- no hidden remote camera access
