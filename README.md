# APEX

A lightweight, native Android race companion built with Kotlin and Jetpack Compose. The default app runs offline with a clearly labeled, deterministic Monaco exhibition. No account, API key, backend, map SDK, downloaded images or voice service is needed to try it.

Phone APK: [APEX-0.4.0.apk](artifacts/APEX-0.4.0.apk). Open **Explore → Race 3D** to race the supplied liveries on the 2027 tracks. The [experience guide](docs/EXPERIENCES.md) and [QA report](docs/QA.md) cover the earlier 0.3 experiences.

### Phone installation troubleshooting

If Android reports **App not installed**, that message alone does not identify the cause. The original APK passes `apksigner verify` and `zipalign -c -P 16 4` checks, contains ARM64 and ARMv7 native libraries, targets API 36, and has no `testOnly` flag. A phone-side package-manager error is still needed to distinguish a signing conflict, insufficient storage, damaged transfer, or installation policy.

The separate **APEX Demo** phone variant uses package `com.aistudio.f1live.vnmqpz.demo`, so it can coexist with a differently signed original APEX installation. It is non-debuggable, excludes Compose preview/test tooling, and retains the offline demo. It uses the local development signing certificate, not a production distribution key. This variant rules out a conflict with the original package; it cannot bypass Android installation policies or storage limits.

```powershell
# Build serially with the existing low-memory settings and copy to artifacts/APEX-phone.apk:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build-local.ps1 -PhoneApk
# Connect an unlocked phone, enable USB debugging, and authorize this computer:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/install-phone.ps1
# Diagnose the original APK instead:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/install-phone.ps1 -ApkPath artifacts/APEX-demo-debug.apk
```

The installer preserves existing app data, does not uninstall anything, and does not bypass package verification. It writes the exact install response, APK checksum, device compatibility details, and launch command response to ignored `.tools/install-phone.log`. If more than one device is connected, supply `-DeviceSerial`. Do not uninstall an existing APEX just to test the separate phone variant.

## Run

Open this repository in Android Studio, use JDK 17, let it install Android SDK 36.1 and Build Tools 36.0.0, then run `app` on Android 7.0 / API 24 or later. The project includes the Gradle 9.3.1 wrapper. Build compatibility follows the [Android Gradle plugin 9.1 documentation](https://developer.android.com/build/releases/agp-9-1-0-release-notes).

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

On macOS/Linux use `sh ./gradlew` in place of `.\gradlew.bat`. Set `JAVA_HOME` to a JDK 17 installation and either `ANDROID_HOME` or `sdk.dir` in the ignored `local.properties`. Debug signing uses Android's standard automatically created development key. Release builds enable code and resource shrinking; configure your own signing key before distribution.

On the current workspace, project-local tools are available in the ignored `.tools` directory. Use `scripts/build-local.ps1` to reuse them without changing system settings. These development tools are not bundled into the APK.

### Low-memory builds

The default build caps the Gradle heap at 768 MiB, uses one worker, disables parallel builds and persistent daemons, and runs Kotlin compilation in the same JVM. Tests use one 384 MiB worker, with a fresh worker per test class to avoid retaining multiple Robolectric sandboxes. Native allocations and metaspace use additional memory; these limits are not a total-RAM guarantee.

`scripts/build-local.ps1` builds the APK only. Add `-PhoneApk` for the separate phone package, `-RunTests` for validation or `-RecordScreenshots` for native visual captures. Compilation and tests run in separate, sequential Gradle invocations to release compiler metadata between phases. No emulator is required for compilation or host tests. A physical phone avoids the additional RAM cost of an Android emulator.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build-local.ps1
# Optional checks:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build-local.ps1 -RunTests
```

The execution-policy argument applies only to that PowerShell process. To sample the Java processes during a build, run `scripts/measure-build-memory.ps1` in a second terminal; it writes `docs/measurements/build-memory.json`.

## What works

**Race 3D** is available from Explore and each circuit’s detail view. Use **Switch track** to pick any of the 24 supplied circuits. **Watch AI** runs a one-lap race with all 22 liveries. **Drive** lets you choose a car and hold the on-screen steering, throttle and brake controls. Chase, TV and Track change the camera.

The simulator uses the supplied road geometry, grid positions, elevations and asphalt/kerb textures. The ten OBJ files have identical geometry. All 22 cars share the corresponding 5.4 m mesh with the supplied UV-mapped paint, tyre and carbon textures. Only the current circuit is loaded. Races run locally with simulated speeds and positions.

To regenerate the bundled race assets from the original files, run `python scripts/prepare-race-assets.py --downloads <source-folder>` with NumPy and Pillow installed. Android builds use the packed assets directly.

**New in 0.3:** open **Explore** for the 3D circuit collection, Paddock Pals, Helmet Vault, AI Pit Wall, Time Machine, Strategy Simulator, Garage, Rivalry Lab, Intelligence, Virtual Paddock and Career Universe. All 24 outlines are bundled offline. Procedural models avoid a heavy 3D runtime. Published standings/history use Jolpica; a bounded historical lap uses OpenF1 and Room cache. The race feed remains a labeled exhibition. See the [feature scope](docs/EXPERIENCES.md).

- **Live:** persistent session status, last-receipt freshness, driver selection, expandable timing, lap-pace sorting, simulated circuit markers, pinch/pan/zoom/reset, telemetry, same-clock driver comparison, sectors and race-control timeline.
- **News:** original offline demonstration editorials, search, categories, pull-to-refresh of local storage, pagination, reading state, Room-backed bookmarks, article view and Android sharing.
- **Drivers:** sample championship standings, configurable follows, app-owned helmet avatars, sample stats, teammate points comparison and direct telemetry navigation. Unavailable career/results data is explicitly labeled.
- **Fantasy:** validated two-driver/one-constructor roster, 100M budget, captain, persistent team, deterministic rules and scoring breakdown, sample leaderboard, local private-league creation/join codes, invite sharing and the ten most recent roster saves.
- **Community:** fictional channel previews, local Room-backed messages, replies, session reactions, persistent blocking and local report records. Voice rooms offer an explicitly disconnected preview and leave controls; they never request or enable the microphone.
- **Alerts:** category/favorite/quiet-hour/delivery preferences, deduplication, throttling, opt-in Android local notifications, permission-denied handling, test notification and notification deep links.
- **Sharing:** native PNG timing and fantasy cards through Android's share sheet and FileProvider, plus text/article/invite sharing. Cards are labeled exhibition data and contain no video.
- **Appearance:** persistent dark/light/system preference, larger-text option, reduced-motion preference, screen-reader names on map and icon controls, safe-area navigation and keyboard-aware local chat.

## Data integrity

The entire shipping race experience is **DEMO**. Driver/team identities come from a 2024-inspired sample roster; displayed points, ages, race order, gaps, weather, event messages and telemetry are not verified current facts. The circuit is an authored schematic, not surveyed Monaco geometry. Location packets are generated directly by the demo provider; real positions are never inferred from lap timing. Original editorials explain general racing concepts and are not breaking journalism. No Formula 1 logos, licensed driver photos or video assets are bundled.

The producer sends four telemetry/location updates per second and one timing/session update per second while foregrounded. Backgrounding cancels the subscription. Returning restarts one subscription with a complete snapshot. The deterministic event cycle includes a safety car at 60s, a red flag at 70s and resumption at 80s of simulated runtime. Cars stop during the red flag. In Profile, **Drop feed** exercises reconnect state; **Reconnect** restores it.

## Production integrations

No paid live, voice, push or language-model credentials are configured. Public historical/results integrations are implemented with source labels and cache. The [optional Python gateway](backend/README.md) supports server-side AI credentials; configure its HTTPS URL and gateway token in AI Pit Wall to use it. The built-in analyst works locally without an LLM. [Provider interfaces](docs/PROVIDERS.md) document the remaining production interfaces. Live streaming, authenticated cloud leagues/chat, real voice, remote push and licensed news/video still require their respective providers and backend infrastructure.

Never put upstream provider credentials, voice API secrets or FCM service accounts in Android resources, BuildConfig or source code. `.env.example` lists backend-only integration settings; the offline client requires none.

## Architecture and QA

`MainActivity → F1MainApp → screens/components` retains the original Compose architecture. `F1ViewModel` coordinates the Room repository, independent race subscriptions, fantasy rules and preferences. `RaceStore` validates normalized packets, rejects old sequences, deduplicates events, manages connection recovery and bounds history. No chart, map, image loading, icon pack, AI or Firebase runtime is included.

- [Repository audit and plan](docs/IMPLEMENTATION.md)
- [Integration contracts](docs/PROVIDERS.md)
- [QA and measurement report](docs/QA.md)
- Native screen captures are written to `docs/screenshots/` by `GreetingScreenshotTest`.

The tests cover packet validation/normalization, event deduplication, freshness, backoff, cancellation, reconnect, bounded histories, fantasy validation/scoring, notification priorities and primary Compose journeys. The host ingestion benchmark writes `docs/measurements/demo-ingestion.json`. That benchmark is not a device FPS or end-to-end live-feed latency measurement.
