# BodyCam AI v2.43 — Stage 2 / Offline Maps

- Added local map package repository with MBTiles, PMTiles and ZIP import validation.
- Added persistent custom HTTPS tile-source registry using `{z}/{x}/{y}` templates.
- Added Web-Mercator tile math and ground-resolution helpers.
- Offline packages remain local; no automatic upload is performed.
- ZIP imports reject traversal paths and excessive entry counts.
- Custom tile sources require HTTPS and bounded zoom ranges.
- This block does not claim a universal map renderer or a specific provider's licensing terms.
