# APEX 0.3 interactive collection

The supplied recording is 20.27 seconds, 30 FPS, 1106 × 762. Twelve sampled frames were inspected. Its composition is a central rotating car, black backdrop, diagonal red floor and compact surrounding panels. The native Garage adapts that layout for a phone. The recorded Ferrari model, sponsor assets and F1 logo were not extracted. All characters and models are original procedural artwork.

Open **Explore** in the header. Live has circuit/Pals shortcuts; driver profiles link to helmets and career history. The five core tabs remain accessible. Back returns to the collection, then the core screen.

| Experience | Working implementation | Limits |
| --- | --- | --- |
| 2027 circuits | All 24 geographic outlines; dates/Sprints, search/filter, raised reveal, rotate/tilt, pinch zoom, reset and detail panel | Height is artistic, not terrain. Future layouts/dates can change; Türkiye subject to homologation |
| Living Driver Doodles | Original vector Pals in Live, timing, profiles and fantasy; jump, drag, profile hold, idle, run/race, celebrate, disappointment, helmet adjustment, entry/exit; timing-row event reactions | Ten-driver exhibition roster; toy race is not a prediction or real result |
| Helmet Vault | Real projected mesh, camera controls, visor hinge, separated layers, lighting and three color-study seasons | Fictional 2024–26 liveries, not licensed/race-worn historical replicas |
| AI Pit Wall | Local typed-evidence analyst plus optional HTTPS AI gateway, bounded responses and citation-ID validation | No model account configured; AI output is an unverified interpretation, separate from observed values |
| Time Machine | Demo replay plus recorded OpenF1 Monaco 2024 lap 10 for LEC/PIA; scrub, play/pause/rate, available speed samples and sharing | Bounded recorded lap, not a full race catalog or video. Missing/stale coordinates disappear |
| Strategy Simulator | Pit lap/compounds/rain/pit loss/degradation/safety-car assumptions; 80 seeded scenarios, cumulative delta and sharing | Educational generic 58-lap model; sensitivity percentiles are not calibrated prediction confidence |
| Digital Garage | Original car mesh, camera controls, palettes, component isolation, exploded assembly and airflow illustration | Simplified educational geometry; no team CAD, CFD or engineering measurements |
| Rivalry Lab | Demo lap/sectors; published 2026 race results/points/podiums, grid-to-finish gains and matched Q1 median delta | Race points omit Sprints; gains are not overtakes; conditions are not normalized; no invented racecraft score |
| Race Intelligence | Event context, source facts versus possible implications, timestamps and Pit Wall handoff | Event source remains the labeled exhibition |
| Virtual Paddock | Persistent local predictions, simulator handoff, existing local rooms/chat and disconnected voice previews | No fabricated participation, global delivery, authentication or live audio |
| Career Universe | Published driver/season results, race timeline, season wins/podiums and cache/error states | Season totals rather than unsupported whole-career aggregates |

Explore and Profile also link to published 2026 standings. Fractional points, provider round, retrieval time and source URL are preserved. These results are separate from the 2024-inspired exhibition roster. Strategy deep links restore pit lap, compounds, rain, pit loss, degradation and safety-car assumptions. Replay links restore data mode and timestamp. PNG cards and native sharing captions retain Demo, Recorded or Hypothetical classification.

## Sources and rights

- Calendar: [Formula 1 announcement, 16 September 2026](https://corp.formula1.com/2027-calendar-announced-with-10-sprint-events/), checked 22 November 2026.
- Geometry: [Tomislav Bacinger/f1-circuits](https://github.com/bacinger/f1-circuits), MIT. Full license is bundled in `app/src/main/assets/licenses/f1-circuits-MIT.txt`. Reviewed source SHA-256: `7dcd3b9a5fe25b46c7ea276ea513475854a4187cebcf4384f9cd9a3465deee55`. `scripts/prepare-circuits.py` reproduces the 60 KB, 2,992-vertex collection. It is not required for builds.
- Historical timing/locations: [OpenF1](https://openf1.org/docs/), public historical endpoints on demand. Paid live/auth endpoints are not called.
- Results/standings: [Jolpica documentation](https://github.com/jolpica/jolpica-f1/tree/main/docs), custom `APEX/0.3.0 Android` User-Agent, bounded responses, paced requests and cache.
- Aero terminology: [F1 2026 terminology update](https://corp.formula1.com/f1-2026-regulations-terminology-update/). New engineering notes describe active aero; legacy DRS remains explicitly part of the 2024 demonstration.

## Runtime and integration

No Android dependency was added. Compose Canvas projects vertices and depth-sorts faces. Models are generated only when their views open; no texture pack, game engine or model downloader runs. Doodle idle motion follows lifecycle/reduced motion, and replay pauses on backgrounding.

`PublicF1Repository` separates raw schemas from UI. IO has timeouts, cancellation, 4 MB response bounds, paced request admission, validation before cache replacement, provenance and stale fallback. Room stores at most 32 cache records. Migration 1→2 adds the cache while preserving teams, bookmarks and messages. Recorded telemetry downloads one lap rather than an entire race.

The optional [Python gateway](../backend/README.md) uses only the standard library and is never started by Gradle. It caches normalized standings and keeps model credentials server-side. Global community/auth, real voice, remote push, licensed realistic assets, AR and paid live streaming remain separate integrations.

## Checks

Native Robolectric journeys exercise circuit search/detail, model controls, simulation changes, replay scrubbing, toy racing, evidence explanations and navigation. Logic tests cover projection, simulation, absent/stale replay samples, timestamps, fractional points, source labels, gateway validation and token storage. A migration test preserves existing fantasy/bookmark data.

`PublicProviderSmokeTest` makes real public HTTPS requests through the Android repository when `APEX_NETWORK_TEST=1`. It never calls paid live/AI endpoints. Backend tests use fake upstream/model responses. Results are recorded under `docs/measurements`; host captures/math timings are not phone FPS or end-to-end latency measurements. No emulator is required.
