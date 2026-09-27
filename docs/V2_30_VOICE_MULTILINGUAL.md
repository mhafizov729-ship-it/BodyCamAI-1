# v2.30 — Voice + multilingual foundation

- Expanded multilingual voice command vocabulary for camera and drone intents.
- Added zoom/record/center mappings for non-Russian languages.
- Added stricter normalization while preserving language-independent command intents.
- Language packs remain data-only JSON and are parsed with Android JSON APIs.
- Added validation of language tag and strings object, plus loading custom strings by language.
- No voice command directly executes hardware movement; adapters/safety gates remain responsible for execution.
