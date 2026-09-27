# v2.44 — Stage 2 AR/XR

Added a vendor-neutral AR/XR integration layer:
- capability reporting (6DoF, passthrough, depth, external display);
- tracking state model;
- spatial marker model for Sensor Fusion events;
- normalized HUD projection using camera field-of-view assumptions;
- adapter boundary for real Android/vendor XR SDKs.

No AR/XR hardware is marked supported without a real adapter. No simulated 6DoF/depth data is generated.
