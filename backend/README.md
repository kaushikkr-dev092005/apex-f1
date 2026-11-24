# Optional APEX gateway

This server uses Python 3's standard library: no pip install, Node process, container, GPU or model download. It is optional and is never started by the Android build. The app's public standings/history views also work directly with their documented public providers.

Run from the repository root:

```powershell
python backend/apex_backend.py
python -m unittest discover -s backend -p test_backend.py -v
```

The server binds to `127.0.0.1:8080`. `GET /health` reports service status; `GET /v1/standings?season=2026` fetches, validates and normalizes Jolpica standings with a 15-minute SQLite cache, a 32-entry bound and explicitly labeled stale fallback. Only supported integer seasons reach the fixed provider URL.

For AI, set these **server environment variables** before starting:

| Variable | Purpose |
| --- | --- |
| `APEX_BACKEND_TOKEN` | Gateway access token required for AI POST requests |
| `APEX_LLM_URL` | HTTPS URL of a compatible chat-completions endpoint |
| `APEX_LLM_API_KEY` | Model provider key; never copy this into Android |
| `APEX_LLM_MODEL` | Model identifier supported by that provider |
| `APEX_BACKEND_PORT` | Optional local port, default 8080 |
| `APEX_CACHE_DB` | Optional SQLite path, default `backend/.apex-cache.sqlite3` |

The model adapter expects a chat-completions JSON contract with `messages`, `model`, `temperature`, `max_tokens`, JSON-object response format, and `choices[0].message.content` containing JSON. Adapt that server function for providers using another contract. No model account is configured and the tests use fake upstream responses; running tests makes no paid calls.

Expose the local server through an HTTPS reverse proxy you control. In **Explore → AI Pit Wall → Connect an AI backend**, enter that gateway base URL and its access token. The Android app permits HTTPS only, rejects credentials/query secrets in URLs, and keeps the gateway token in memory. It does not persist the token or bundle upstream API keys. The caller explicitly taps **Ask connected AI** after generating a local evidence explanation; there is no automatic model call.

`POST /v1/pit-wall` accepts a question of up to 400 characters and 1–16 typed evidence items (`id`, `kind`, `text`). It requires the gateway bearer token. The model receives the evidence classifications and instructions to preserve simulated/assumed/unknown distinctions. Returned citation IDs must reference that exact evidence set. The response is always `ai_interpretation_unverified`; native observed values are displayed separately and are not overwritten by model output. Valid citation IDs do not prove semantic correctness of every generated sentence.

The development gateway limits requests to four concurrent handlers, 24 KB AI request bodies, bounded provider/model responses, and 12 model requests/minute. It suppresses payload/token logging. It is **not** a production identity, billing, moderation or multi-tenant system: add per-user authentication, authorization, quotas and your provider's entitlement checks before public use. It is not deployed by this change. Paid live data, voice and remote push remain separate services.
