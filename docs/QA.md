# APEX validation

## Version 0.3 — 22 November 2026

The current installable build is [APEX-0.3.0.apk](../artifacts/APEX-0.3.0.apk), **7,232,940 bytes / 6.90 MiB**, version `0.3.0-demo`, version code 4, package `com.aistudio.f1live.vnmqpz.demo`. SHA-256: `9988f4cb7095758420fcd0527ad7eb3dfcdbd820b77772644996260be055bcb9`. The `APEX-phone.apk` alias is identical. Signature verification succeeds with v2 signing and the same certificate as 0.2.1, so this is an update to that APEX Demo package. ZIP alignment verifies with 16 KB native-library alignment. The merged APK is neither debuggable nor test-only; its circuit data and MIT license are present. Minimum Android API is 24 and target API is 36.

`assemblePhone` completed successfully in 2m30s in the final incremental build. `lintDebug` completed with **0 errors / 40 warnings**: 24 Kotlin extension-style suggestions, 11 unused scaffold resources, 2 scaffold icon-density issues, 2 dependency-update notices and 1 redundant manifest label. No error baseline was used. Retained tool versions still emit non-fatal SDK XML/KSP AWT cleanup and compiler-metaspace diagnostics; final build/test/lint tasks completed successfully. APK, tests and lint were run sequentially to keep RAM down.

The final regression run reports **67 passing Android tests**, **0 failures/errors**, and **1 explicitly skipped opt-in network smoke test**. That network test passed separately with actual HTTPS requests through `PublicF1Repository`: 2026 standings after provider round 21, 25 season participants, 21 Leclerc race/qualifying records, and 610 recorded coordinate samples over the 80.775-second Monaco 2024 lap-10 window. Cache reads were verified. These counts describe the provider response at retrieval time, not a live grid or an end-to-end latency measurement. The optional Python gateway's **7 tests pass** using fake upstream/model responses and no paid AI calls.

New native UI journeys cover circuit search/detail, small 360×640 dp scrolling, light/dark appearance, helmet visor/layers/rotation, garage explosion/airflow, hypothesis validation, replay scrubbing, Pals racing, evidence labels, return-to-Live navigation, full scenario deep-link parameters and replay source/time restoration. Share cards and native chooser intents preserve Demo, Recorded and Hypothetical classifications. The earlier complete core journeys and real launcher tests remain green. The Room 1→2 migration preserves fantasy teams/bookmarks while adding the bounded data cache.

Actual Compose/native captures were inspected in `docs/screenshots`, including `circuit-detail.png`, `circuit-small-phone.png`, `circuit-gallery-light.png`, `helmet-vault.png`, `digital-garage.png`, `paddock-pals.png`, `time-machine.png`, `rivalry-lab.png` and typed share cards. Review corrected visor/shell overlap, dark car shading, source labels, small-phone detail overflow and share-link context. The reference video was inspected locally; its images/models/logos are not included in the app.

No Android dependency was added. The low-memory settings remain a 768 MiB compiler heap, one worker, no parallel builds or persistent daemon, and one 384 MiB test worker at a time. A two-second sample during the earlier complete 0.3 test/public-provider run observed **1,588.9 MiB summed Java working set** (~1.55 GiB), 1,883.8 MiB private commit and at most three Java processes. This is a sampled test workload, not a guaranteed build maximum or OS-wide measurement. See [explore-build-memory.json](measurements/explore-build-memory.json).

There was **no connected Android device for this 0.3 build**. The physical Android 16 installation measurements below apply to the older 0.2.1 package only. Current phone FPS, touch latency, extended-session memory stability and cold launch performance have not been measured; host snapshots and the old phone measurements must not be presented as those results.

The real-time race still uses the labeled exhibition. Helmet/car models are original concepts rather than licensed scans. Historical playback currently covers a single recorded lap, not a whole race catalog. Simulation percentiles are uncalibrated scenario sensitivity. AI interpretation requires the optional configured gateway/provider; global accounts/voice/remote push still need external services. [Full feature scope and sources](EXPERIENCES.md), [machine-readable verification](measurements/explore-validation.json), [test inventory](measurements/explore-tests.json), [public-provider smoke](measurements/public-provider-smoke.json).

## Previous phone installation investigation — 28 October 2026

The user reported that the original APK would not install on a Motorola G82 5G running Android 16. Android's visible message was only **App not installed**. No authorized USB device was visible during the initial checks, so the actual package-manager rejection code and root cause are unconfirmed. A host screenshot test does not prove that an APK installs on this phone.

Both the original package's signature and 16 KB ZIP alignment verify successfully. Its native library LOAD segments use 16,384-byte alignment on all four bundled ABIs. Its manifest targets API 36, supports API 24+, resolves the actual launcher to `com.example.MainActivity`, and has no `testOnly` flag.

A separate non-debuggable `phone` variant produces [APEX-phone.apk](../artifacts/APEX-phone.apk), version **0.2.1-demo**, package `com.aistudio.f1live.vnmqpz.demo`, label **APEX Demo**. This different package ID avoids a signing/version conflict with the original app without deleting that app or its data. It removes debug-only Compose preview/test dependencies and disables shrinking to keep compilation light. It still uses the local development signing key and remains a demonstration, not a production release.

The phone APK is **7,013,972 bytes / 6.69 MiB**, SHA-256 `38527d8291c8e0f84dda39028b739362e170c179cbd3735983e2266786ec9e31`. `apksigner verify --verbose` succeeds with v2 signing, and `zipalign -c -P 16 4` succeeds. Packaged manifest inspection confirms no `debuggable=true`, `testOnly=true`, or preview activity. All ARM64, ARMv7, x86 and x86_64 libraries are bundled. These are static package checks, not evidence of a successful physical installation.

`scripts/install-phone.ps1` obtains the exact `adb install --no-streaming -r` result and records compatibility details in ignored `.tools/install-phone.log`. It does not uninstall apps, permit downgrades, grant runtime permissions, or bypass verification. On installation success it launches the real activity and checks for the app process after two seconds.

The first combined `assemblePhone` and test invocation completed the APK task but exhausted the 384 MiB compiler metaspace during test compilation. The build helper now releases the compiler between APK generation and tests by running them in separate sequential invocations, preserving the RAM caps. No emulator or Android Studio was started.

The separate targeted `LaunchActivityTest` invocation passes both new tests under Robolectric API 35: the actual `MainActivity` and default ViewModel factory start, survive background/resume, and handle a cold-start notification link. These tests do not use the screen-test wrapper or its injected prefetch strategy. They do not constitute Android 16 physical-device validation. Package verification metadata is in [phone-package.json](measurements/phone-package.json).

After enabling the USB debugging interface, the connected phone identified itself as **moto g37 power**, Android **16 / API 36**, ARM64, 4,096-byte pages (different from the initially reported G82 model). **Both the original APK and APEX-phone.apk installed successfully** using `adb install --no-streaming -r`, with no uninstall or data reset. The original manual installer failure did not reproduce; no specific cause has been established.

The phone variant launched successfully, with `am start -W` reporting a cold launch `TotalTime` of **1,387 ms**. This is one Android activity-launch measurement, not startup-to-usable-screen or a latency percentile. Physical UI checks found the expected headings for all five destinations and the driver telemetry panel. Actual phone screenshots of Live and News were inspected locally; captures remain in ignored `.tools/device` to avoid committing personal status-bar details. The app process remained running and its PID-filtered crash buffer had no entries during the brief checks. No extended soak or device FPS test was performed.

One post-navigation `dumpsys meminfo` sample reported **96,104 KiB total PSS**, **182,888 KiB total RSS** and **12,678 KiB swap PSS**. This is one observation, not a peak or a guarantee of race-session memory stability. The separate final `scripts/build-local.ps1 -PhoneApk` run completed successfully and copied the verified APK and checksum to `artifacts/`. The app was left open on the connected phone.

## Final result

Verified on 28 October 2026: `assembleDebug`, `testDebugUnitTest` and `lintDebug` completed successfully with the checked-in low-memory settings. All **39 tests pass**. Lint reports **0 errors and 68 warnings**; remaining warnings concern dependency update suggestions, unused scaffold resources/icon variants, redundant metadata and Kotlin extension style suggestions. The build also emits non-fatal SDK XML/deprecated Room/KSP cleanup diagnostics with the retained tool versions; no failed build or test is hidden by a baseline.

The installable Android debug APK is [APEX-demo-debug.apk](../artifacts/APEX-demo-debug.apk), **10,694,768 bytes / 10.20 MiB**. Its SHA-256 is `41e51a2feec67121337eddcb033363d01001a6c42e92e51de7dc931b223a0139`. Machine-readable results are in [validation-summary.json](measurements/validation-summary.json).

Native captures of Live in dark/light appearance, News, Fantasy, Community, Profile and an actual generated share card are in [screenshots](screenshots/). Visual review corrected a clipped hero hint and aligned voice-room cards. Native host rendering can retain a tap-ripple texture in sequential screenshots; physical-device rendering and motion still require device QA.

## Scope

Validation uses the native Android project on this Windows workspace, JDK 17 and SDK 36.1. The original Compose journeys and screen captures ran under Robolectric API 35 before a physical device was connected. The later Android 16 installation/navigation checks are described above. An emulator is deliberately not required, to avoid its additional RAM cost.

## Build memory

The checked-in defaults use a 768 MiB Gradle heap, 384 MiB metaspace ceiling, 128 MiB code cache, Serial GC, one worker, in-process Kotlin compilation, no parallel tasks and no persistent Gradle daemon. Unit tests use one 384 MiB worker with a 256 MiB metaspace ceiling and a fresh worker for each class. These are per-process limits; total RAM includes native allocations, JVM metadata, code caches, the launcher and test processes.

The optional lint worker is also capped at 384 MiB through the current Android Gradle plugin's lint heap setting. A two-second sample of the reduced-memory build/validation attempts observed a peak summed Java working set of 1,860.8 MiB (about 1.82 GiB), including the compiler parent, launcher and one test worker. This sampled measurement includes diagnostic attempts and is not a guaranteed limit for every machine or clean dependency-resolution run.

`scripts/build-local.ps1` builds the debug APK only. Tests and screen recording are opt-in. `scripts/measure-build-memory.ps1` samples project Java processes every two seconds and writes [build-memory.json](measurements/build-memory.json). It measures summed working sets and private committed memory, not an OS-wide peak; shared pages can be counted more than once and short peaks can be missed.

## Logic and journeys

The test sources cover deterministic race packets, malformed-message rejection, JSON normalization, event deduplication, sequence order, stale/offline transitions, bounded histories, backoff, subscription cleanup and reconnect. Fantasy tests cover budget/name/duplicate validation, captain multipliers, constructor points and scoring totals. Notification tests cover followed drivers, category filters, quiet hours, digest priority, deduplication and throttling.

Native journeys exercise driver telemetry/profile navigation, all five destinations, article bookmarking, fantasy creation/scoring, disconnected voice preview/leave, preference persistence and a red-flag deep link. Platform tests check denied notification permission, demo notification content/deep links, actual PNG generation and Android share-sheet invocation.

The host share test substitutes only the FileProvider URI boundary with a cache-root-validating shadow because AndroidX's Android '/' path assumptions conflict with Windows canonical paths. The card bitmap, PNG encoding, chooser Intent and URI-grant flag use the production code. FileProvider grants and sharing into other installed apps remain physical-device checks; the production application uses AndroidX's real provider.

The UI harness injects a no-prefetch strategy into the shared list primitives to prevent offscreen prefetch from repeatedly scheduling synthetic frames while Espresso waits for idle. Visible items still compose and scroll normally; the shipping app keeps its default platform prefetch strategy. Test transitions use the app's reduced-motion setting.

## Performance measurements

[demo-ingestion.json](measurements/demo-ingestion.json) is generated by a host JVM benchmark: 300 warm-up packets, then 2,000 sequential packets across ten drivers. It separately times demo generation and validation/state ingestion and checks bounded telemetry history. There are no UI observers or network in this benchmark.

No device FPS, startup-to-usable-screen timing, extended-session device heap profile, client update-to-render p95, upstream/provider delay, server processing or network delivery latency has been measured. The one later activity-launch time and memory sample are documented above. The ingestion results must not be represented as end-to-end real-time performance.

## Service boundaries

| Experience | Available in this build | Requires external configuration |
| --- | --- | --- |
| Race awareness | Deterministic foreground demonstration, interactive circuit and telemetry, race control, reconnect | Licensed live feed, verified metadata and corresponding real circuit geometry |
| News | Original offline editorials, search, saved/read state and sharing | Licensed current news provider |
| Drivers | Sample standings and points, follows, teammate comparison | Verified career history, current standings and results |
| Fantasy | Local roster, scoring, sample rivals, local league codes | Authentication, shared membership, authoritative deadlines and cloud scoring |
| Community | Local chat, replies, session reactions, blocking and locally stored reports | Global delivery, server moderation and abuse prevention |
| Voice | Clearly disconnected room previews and leave controls; microphone never enabled | Authorized SFU/WebRTC provider, room tokens and microphone permission flow |
| Notifications | Opt-in local Android notifications, filters, quiet hours, test alert and deep links | Remote registration, background delivery and scheduled cloud digests |
| Highlights | Real PNG timing/fantasy cards and native sharing | Licensed video highlights |

## Release checks still needed

Before production distribution, configure release signing and real providers, replace all sample metadata and schematic geometry, validate provider entitlements/licensing, add cloud authentication/moderation, verify push and voice on physical devices, and profile a full race on representative hardware. This build accurately identifies its demonstration and unavailable states.
