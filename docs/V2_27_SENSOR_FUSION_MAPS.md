# BodyCam AI v2.27 — Sensor Fusion + GPS/Map integration

## Completed in this pass
- Sensor Fusion state now carries the current GPS position, heading and location accuracy into the unified fusion model.
- Fusion source health continues to be based on actual timestamped frames rather than UI toggles.
- Map UI now has a real selected map mode instead of displaying non-interactive labels.
- Offline map registry supports explicit progress updates and pause/cancel state without pretending that tiles were downloaded.
- Existing 120 ms frame-pair synchronization remains the default fusion threshold.

## Scope boundary
This pass does not claim a real map-tile downloader, satellite provider, thermal SDK, or vendor-specific sensor integration. Those require an actual data source/API.
