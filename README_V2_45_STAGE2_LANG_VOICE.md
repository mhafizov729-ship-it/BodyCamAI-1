# BodyCam AI v2.45 — Stage 2 / Languages + Voice

- Added a built-in language registry for 13 languages.
- Added user-controlled voice runtime settings: disabled by default, wake phrase, language selection, confirmation, readiness/confirmation sounds, and DND behavior.
- Added wake-phrase matching as a pure parser layer; it does not start microphones or execute commands.
- Existing data-only language packs remain supported and never execute code.
- Actual speech recognition/TTS availability remains dependent on Android/vendor engines and installed language data.
