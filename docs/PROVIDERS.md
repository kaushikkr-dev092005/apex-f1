# Integration contracts

## Current boundary

Version 0.3 adds `PublicF1Repository` for on-demand Jolpica published standings/results/qualifying and a bounded OpenF1 historical lap. These are separate from the still-simulated live `RaceProvider`. Provider JSON is normalized before presentation, cached in Room (migration 1→2), bounded to 32 records, and labeled with source/fetch time/cache freshness. See [the experience contracts](EXPERIENCES.md) and [optional AI gateway](../backend/README.md). The gateway keeps model keys server-side; its output is always an unverified interpretation, never a new observation.

`RaceProvider.packets(): Flow<RacePacket>` owns the transport, not the UI. `RaceStore` owns validation, publication, reconnect and freshness. `DemoRaceProvider` is the default implementation. `PollingRaceProvider` is an optional, dependency-free HTTPS adapter for a normalization backend. Add a WebSocket/SSE provider behind the same interface if the licensed provider supports streaming. Do not scrape protected official timing or reuse an account's unauthorized feed.

The polling adapter enforces HTTPS, 8-second connect/read timeouts, at least 250ms between requests and a 1 MiB response limit. It runs network/parsing work on `Dispatchers.IO`, disconnects the HTTP connection in `finally`, and checks coroutine cancellation between reads. A blocking read can take up to its timeout to unwind. Its optional bearer-token callback is for a short-lived authenticated user session, never a permanent upstream API secret. HTTP errors close the flow and let RaceStore reconnect with exponential backoff and jitter.

Inject a different provider into `RaceStore` in the composition root after configuring verified metadata, circuit geometry, matching session identity and provider permissions. The shipping UI intentionally uses the offline exhibition; simply replacing the provider without replacing sample metadata and event-specific labels is not a production integration.

## Normalized snapshot schema

```json
{
  "sequence": 1042,
  "eventTimeMs": 1716732300000,
  "session": {
    "name": "Example Grand Prix",
    "circuit": "Example Circuit",
    "country": "Example",
    "sessionType": "Race",
    "lap": 24,
    "totalLaps": 78,
    "status": "Green"
  },
  "timing": [{
    "driverId": "leclerc", "position": 1, "lastLapMs": 73611,
    "gapMs": 0, "intervalMs": 0, "sectorsMs": [18672,34581,20358],
    "tyre": "M", "tyreAge": 12, "stops": 1,
    "personalBest": true, "fastest": true
  }],
  "locations": [{"driverId":"leclerc","x":0.25,"y":0.6,"approximate":false}],
  "telemetry": [{
    "driverId":"leclerc", "eventTimeMs":1716732300000,
    "speedKph":280, "throttle":1.0, "brake":0.0,
    "gear":7, "rpm":11700, "drs":false
  }],
  "events": [{
    "id":"session-event-42", "kind":"RedFlag", "title":"Session suspended",
    "detail":"Provider race-control message", "lap":24,
    "eventTimeMs":1716732300000, "driverId":null
  }]
}
```

Only sequence and eventTimeMs are required. Omit unsupported sections instead of fabricating data; an absent location list remains absent. The initial/resumed subscription should provide a complete current snapshot. In-session sequences must increase monotonically across reconnects. Use a new RaceStore for a different session or a provider whose sequence namespace resets. Heartbeats should carry new sequence/timestamp values. Duplicate events must keep the same ID; duplicate packets do not refresh data freshness.

Location coordinates must be normalized against the provided circuit geometry, not derived from gaps. The shipped map is a schematic and must be replaced with corresponding permitted geometry before using real coordinates. The sample metadata currently covers ten drivers; expand it from a verified metadata provider before integrating a full field. Unsupported telemetry fields should remain unavailable; the initial telemetry model requires a complete validated sample and adapters must not invent required values.

## State and memory

- States: Connecting, Live (transport receiving, including a separately labeled demo), Reconnecting, Stale after 3 seconds without an accepted packet, Offline after 8 seconds or background cancellation. Reconnecting remains explicit while retrying; last receipt age remains visible.
- Backoff: 1, 2, 4, 8, 16, 30 seconds, with ±20% jitter; success resets attempts.
- Independent StateFlows: session, timing, locations, events, health, per-driver latest sample and per-driver history.
- History: 240 samples per driver, 120 events, 1024 event dedup IDs. Community history is trimmed to 100 messages per channel.
- Event time and device receipt time are separate. Do not derive transport latency from fixture timestamps. This store does not claim cross-provider clock synchronization or lossless packet recovery.

## Other services

**News:** `F1Repository.getNews()` serves Room-backed `EditorialFixtures`. A production repository should map a licensed source into a separate article schema including source URL, publisher, publication time, image attribution, pagination cursors and error/cache metadata. Current fixtures are original educational text, explicitly labeled.

**Fantasy:** `FantasyEngine` defines APEX's internal, independent rules. A backend must store authenticated rosters, immutable deadline snapshots, invitations, scoring versions/corrections and cross-device memberships. Local league codes and sample rivals do not constitute online membership. No official fantasy API is assumed.

**Voice:** `VoiceService` defines join, microphone state and leave. `UnconfiguredVoiceService` returns explicit configuration errors. An actual implementation requires an authorized SFU/WebRTC provider, backend-issued room tokens, explicit runtime microphone permission, reconnect/mute/device routing, participant events, moderation and consent. The demo never requests the microphone and accurately says no audio is connected.

**Push:** `NotificationService` and `NotificationPolicy` separate delivery from priorities/preferences. `AndroidNotificationService` supplies real, opt-in local notifications only. Remote delivery needs FCM registration, backend device-token storage, category subscriptions, event-id deduplication and priority/rate-limit queues. No persistent polling service is installed. Quiet hours silence all categories; digest retains routine events in the timeline. Manual test alerts explicitly bypass these preferences.

Deep links: `apex://race/event/{id}`, `apex://driver/{id}`, `apex://article/{id}`, `apex://fantasy/team`, `apex://fantasy/league/{code}`. Single-top intents handle an open app; `onCreate` handles cold start. Event details may no longer be cached after restart; display the retained timeline or an unavailable state rather than inventing an event.

**Backend environment:** optional `TELEMETRY_PROVIDER_API_KEY`, `TELEMETRY_PROVIDER_BASE_URL`, `NEWS_PROVIDER_API_KEY`, `FCM_SERVICE_ACCOUNT_PATH`, `VOICE_PROVIDER_API_KEY`, `VOICE_PROVIDER_API_SECRET`, `DATABASE_URL`. These are integration placeholders, not implemented backend services and not client build requirements.
