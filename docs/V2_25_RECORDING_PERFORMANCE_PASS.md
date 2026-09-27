# BodyCam AI v2.25 — Recording & Performance Pass

Completed in this batch:
- bounded local BodyCamAI video retention;
- lightweight recording-interruption recovery marker;
- runtime memory/low-memory snapshot API;
- recording policy model including circular/pre-event configuration;
- cleanup runs only after a successfully finalized recording.

Important: true pre-event video capture requires a rolling encoded-frame buffer integrated with the recorder; this pass exposes the policy and recovery foundation but does not claim a real pre-event buffer yet.
