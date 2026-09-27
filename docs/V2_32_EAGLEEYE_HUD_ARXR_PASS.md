# v2.32 — EagleEye HUD + AR/XR foundation

Implemented in this pass:
- world-marker rendering layer over the live frame;
- AI detection boxes scaled from source-frame coordinates to display coordinates;
- tracking IDs remain available in the HUD data model;
- fusion confidence indicator;
- marker filtering follows the selected AI overlay mode;
- marker rendering is bounded to a small number of objects to control UI load.

This is an AR/XR-ready HUD foundation, not a vendor-specific headset integration. Real XR displays and thermal streams still require their device APIs/SDKs.
