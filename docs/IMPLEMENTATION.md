# APEX implementation

## Repository audit

The application is native Android (Kotlin, Jetpack Compose, Material 3), not React Native. MainActivity hosts F1MainApp and an AndroidViewModel. Room persists news, chat, and a two-driver/one-constructor fantasy roster. Existing maps are Canvas-based; telemetry was randomly generated without a provider contract. The project had no Gradle wrapper, no production data transport, no real voice infrastructure, and no remote push registration.

## Implementation plan

1. Preserve Compose, Room, and the repository boundary. Remove unused Firebase, AI, image/network serialization, and navigation dependencies; use platform sharing and notifications.
2. Centralize APEX color/typography tokens, persistent dark/light/system settings, and five-destination navigation.
3. Add deterministic provider frames, independent session/timing/location/telemetry streams, event deduplication, bounded history, lifecycle cleanup, freshness, and reconnect handling.
4. Prioritize the live overview, expandable timing, selectable circuit markers, telemetry, race control, profiles, and comparison.
5. Connect local news, fantasy selection/scoring/leagues, local community preview, share cards, and notification preferences. Label all fixtures and preview-only voice honestly.
6. Add critical unit and Compose interaction tests; build and capture native screens when the Android toolchain is available. Report measured results and blockers without claiming unmeasured device performance.

## Lightweight choices

No web runtime, bundled video, image loading library, chart library, map SDK, always-on background service, or external service is required for demo use. Circuit, driver avatar, editorial artwork, and graphs are native Canvas/vector work. One foreground demo producer serves all race subscribers. Histories are bounded. Release builds enable R8 and resource shrinking. Room remains to preserve existing user data and persistence.
