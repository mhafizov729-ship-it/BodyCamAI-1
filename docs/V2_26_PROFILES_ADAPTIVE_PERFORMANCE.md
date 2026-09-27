# v2.26 — Profiles + adaptive performance

Implemented:
- deterministic module defaults for all built-in profiles;
- custom profile remains user-controlled;
- ALL profile enables every declared module;
- adaptive performance tier based on memory pressure and optional battery level;
- conservative analysis interval and overlay object caps under load;
- critical memory mode can temporarily suspend AI analysis rather than pushing the device into higher pressure.

This pass does not claim vendor-specific thermal, GPU or camera FPS controls. Those require device APIs and should be integrated only when available.
