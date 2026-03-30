# schedulify

This repository now includes a working Android project scaffold for the **Schedulify** app, aligned with the implementation blueprint.

## Modules
- `app` — Jetpack Compose Android app (prompt input, schedule generation, day-plan timeline)
- `core/scheduler` — deterministic scheduling engine and unit tests

## Build and test
- Build debug APK:
  - `./gradlew :app:assembleDebug`
- Run scheduler tests:
  - `./gradlew :core:scheduler:test`
- Run parser tests:
  - `./gradlew :app:testDebugUnitTest`

## Implementation Blueprint
- [`AI_SCHEDULE_GENERATOR_IMPLEMENTATION_PLAN.md`](./AI_SCHEDULE_GENERATOR_IMPLEMENTATION_PLAN.md)
