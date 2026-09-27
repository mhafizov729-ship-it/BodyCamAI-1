# BodyCam AI — план всех функций по этапам

## Этап 1 — Основа и запуск
- Android 10+
- Home / Profiles / Devices / Settings
- CameraX live preview
- фото/видео/аудио
- локальное AI
- GPS/компас
- диагностика
- архив
- базовый HUD

## Этап 2 — AI и восприятие
- object/face/OCR detection
- bounding boxes
- tracking IDs
- smoothing
- режимы обводки
- confidence filter
- AI events
- AI alerts
- история событий
- настраиваемые подписи/контуры
- post-recording analysis foundation

## Этап 3 — Sensor Fusion
- единая временная шкала сенсоров
- confidence/latency
- phone camera
- rear/external/thermal/drone/XR source contracts
- Sensor Center
- DAY / LOW LIGHT / THERMAL / FUSED
- реальные источники вместо имитации

## Этап 4 — Внешние устройства
- Android external camera
- USB discovery/permission
- UVC fallback contract
- thermal SDK adapters
- RTSP/WebRTC adapters
- multi-camera synchronization
- external audio sensors

## Этап 5 — Карта и навигация
- online/offline maps
- downloadable regions
- custom map sources
- compass/GPS
- units/date/time/timezone
- event markers
- local map cache

## Этап 6 — Связь и Live
- local P2P
- internet P2P
- hybrid mode
- QR/code/trusted devices
- sync queue
- live video
- adaptive quality
- connection recovery

## Этап 7 — Голос и языки
- RU/EN base
- language packs
- user packs
- auto/manual language
- multilingual command parser
- configurable wake phrase architecture
- offline/online voice adapters

## Этап 8 — Дроны и PTZ
- vendor-neutral command layer
- follow operator architecture
- GPS follow
- camera follow
- hold/stop/return-home contracts
- PTZ camera commands
- mandatory movement confirmation
- link-loss stop
- hard limits
- real vendor SDK adapters only when available

## Этап 9 — AR/XR и EagleEye-style HUD
- spatial markers
- minimap
- rear/side feeds
- fused sensor layer
- world-space annotations
- XR adapters
- themes/transparency/layout editor
- orientation/split-screen/floating-window support

## Этап 10 — Надёжность и производительность
- crash/reboot recovery
- circular recording
- pre-event buffer
- protected recordings
- auto-clean
- battery/thermal monitoring
- AI FPS controls
- network/data limits
- persistent notification / quick settings / widget

## Этап 11 — Финальная интеграция
- unified settings
- per-profile configuration
- module enable/disable
- permissions audit
- diagnostics all
- export/import settings
- full Android 10–16 test matrix
- release build and APK

### Правило
Реальное оборудование и внешние функции считаются готовыми только после проверки фактического потока/API. Интерфейс не должен выдавать отсутствующий сенсор за подключённый.


## v2.28 progress
External device discovery/permission/stream-state layer completed; vendor-specific drivers remain stage work.


## v2.29 status
- Stage 8 P2P/Live View/Sync foundation: LAN discovery, trust store, reconnect state machine completed.
