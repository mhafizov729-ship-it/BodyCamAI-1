# BodyCam AI v2.46 — Stage 2 final foundation pass

Implemented:
- Android 10+ compatible home-screen widget provider.
- Quick Settings Tile to open BodyCam AI.
- Foreground runtime service with a low-priority persistent notification.
- Foreground service camera/microphone declarations for Android 14+ compliance when the runtime is explicitly started.
- Notification permission declaration for Android 13+.
- Version bumped to v2.46.

Performance/power controls already present in the source remain the authority for adaptive FPS/AI load decisions.

The foreground service is not started automatically by this patch; the app must explicitly start it after obtaining the required runtime permissions and user action.
