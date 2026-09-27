# BodyCam AI v2.15 — Two-stage roadmap

This version groups the project into two implementation stages while keeping both stages in the source tree.

**Stage 1 / Foundation:** camera + external camera discovery, sensor fusion, sensor center, localization architecture, language-independent device commands, vendor-neutral drone/PTZ control contracts, safety defaults.

**Stage 2 / Add-ons:** UVC fallback, vendor SDK adapters, real drone follow control, remote video transports, thermal SDKs, advanced spatial audio, downloadable language/voice packs, AR/XR adapters, offline map packages and system integrations.

Important: adapter contracts do not claim hardware support. A device becomes controllable/streamable only after a compatible implementation is installed and the device/API is actually connected.
