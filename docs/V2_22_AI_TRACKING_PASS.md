# BodyCam AI v2.22 — AI tracking pass

- Added temporal smoothing for ML Kit object/face bounding boxes.
- Tracking IDs are preserved when ML Kit supplies them; fallback keys use label/index.
- HUD boxes now move smoothly between frames instead of jumping directly to every detection.
- Added resettable smoother component for future camera/profile switches.
- This is visual tracking stabilization, not autonomous targeting or aiming.
