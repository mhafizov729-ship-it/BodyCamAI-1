# v2.24 — Batch progress

This pass groups multiple AI/event features into one stage instead of one feature per version.

Implemented in source:
- persistent AI alert enable/disable setting;
- minimum confidence threshold for alerts;
- persistent label visibility and contour-only preferences;
- AI event journal clear/export primitives;
- alert engine now ignores detections below configured confidence;
- AI settings screen exposes alert, labels, contour and confidence controls;
- all-stage roadmap added.

Next batch: event severity/configuration + recording pre-buffer/circular storage + performance controls + profile persistence expansion.
