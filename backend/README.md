# SmartCalc AI — backend

Small Express API that sits between the Android app and an AI vision model.
Its only job: accept a photo, ask the model to read and solve the maths problem,
and return a strict JSON answer — or admit it could not read the problem.

The AI API key lives **only** in this service's environment. The Android app never
sees it.

```
Android app ──POST /api/solve──▶ this backend ──▶ OpenAI / Gemini / …
```

---

## Requirements

- Node.js **18.17+** (uses the built-in `fetch` and `node:test`)
- An API key for one supported provider

## Quick start

```bash
npm install
cp .env.example .env     # then edit .env
npm run dev              # or: npm start
curl http://localhost:3000/api/health
```

Run the tests:

```bash
npm test
```

---

## Configuration

Everything is read from environment variables (see `.env.example`).

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | `3000` | Listening port |
| `AI_PROVIDER` | `openai` | `openai` or `gemini` |
| `OPENAI_API_KEY` | — | Required when `AI_PROVIDER=openai` |
| `OPENAI_MODEL` | `gpt-4o-mini` | Any vision-capable model |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | For OpenAI-compatible endpoints |
| `GEMINI_API_KEY` | — | Required when `AI_PROVIDER=gemini` |
| `GEMINI_MODEL` | `gemini-1.5-flash` | Any vision-capable model |
| `GEMINI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta` | |
| `MAX_IMAGE_MB` | `8` | Rejects larger uploads with `413` |
| `AI_TIMEOUT_MS` | `45000` | Per-call timeout towards the provider |
| `RATE_LIMIT_MAX` | `60` | Requests per IP per 15 minutes |
| `CORS_ORIGIN` | `*` | Comma-separated origins, or `*` |

Never commit `.env`. Only `.env.example` belongs in git.

---

## API

### `GET /api/health`

```json
{
  "status": "ok",
  "provider": "openai",
  "providers": [
    { "name": "openai", "configured": true },
    { "name": "gemini", "configured": false }
  ]
}
```

### `POST /api/solve`

Request:

```json
{
  "imageBase64": "<base64 JPEG/PNG/WebP, no data: prefix needed>",
  "mimeType": "image/jpeg",
  "language": "sr"
}
```

`language` is `sr` (Serbian, Latin script) or `en`. A `data:image/...;base64,` prefix is
accepted and stripped.

Recognised — HTTP `200`:

```json
{
  "status": "ok",
  "problem": "2x + 5 = 15",
  "steps": ["2x = 15 - 5", "2x = 10", "x = 5"],
  "solution": "x = 5",
  "confidence": 0.95
}
```

Not recognised reliably — HTTP `200`. No answer is ever invented:

```json
{ "status": "unclear", "message": "Problem could not be recognised reliably." }
```

Error — HTTP `4xx`/`5xx`:

```json
{ "status": "error", "code": "AI_TIMEOUT", "message": "AI request timed out." }
```

| HTTP | `code` | Cause |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | Malformed JSON body |
| 413 | `IMAGE_TOO_LARGE` | Above `MAX_IMAGE_MB` |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Not JPEG/PNG/WebP |
| 422 | `INVALID_IMAGE` | Missing, too small or non-base64 payload |
| 429 | `RATE_LIMITED` | Rate limit hit |
| 502 | `AI_ERROR`, `INVALID_AI_RESPONSE` | Provider failed or returned unusable output |
| 503 | `PROVIDER_NOT_CONFIGURED`, `AI_UNAVAILABLE`, `AI_RATE_LIMITED` | Missing key, provider unreachable or throttled |
| 504 | `AI_TIMEOUT` | Provider exceeded `AI_TIMEOUT_MS` |

Try it:

```bash
curl -X POST http://localhost:3000/api/solve \
  -H 'Content-Type: application/json' \
  -d "{\"imageBase64\":\"$(base64 -w0 problem.jpg)\",\"mimeType\":\"image/jpeg\",\"language\":\"sr\"}"
```

---

## How "never guess" is enforced

Two layers, both in this service:

1. **Prompt** (`src/utils/prompt.js`): the model is told to answer `status: "unclear"`
   for a blurry, cropped, incomplete or non-mathematical image, and to lower
   `confidence` whenever a character is uncertain.
2. **Parsing** (`parseAiAnswer` in `src/services/aiService.js`): anything that is not a
   clean `status: "ok"` object with a non-empty problem and solution becomes `unclear` —
   including prose, markdown fences, malformed JSON, and any answer with
   `confidence < 0.55`. The app then shows the "photograph it again" message.

The Android client applies the same confidence floor a second time, so a misbehaving
backend still cannot produce a made-up answer on screen.

---

## Files

```
src/
├── server.js                       app setup, middleware, listener
├── config.js                       environment parsing
├── routes/solve.js                 /api/health and /api/solve, input validation
├── services/
│   ├── aiService.js                provider registry + strict answer parsing
│   ├── aiService.test.js           tests for the parser
│   └── providers/
│       ├── openaiProvider.js
│       └── geminiProvider.js
├── middleware/errorHandler.js      error → JSON code mapping, 404 handler
└── utils/
    ├── prompt.js                   shared system prompt
    └── http.js                     fetch with timeout and uniform error codes
```

---

## Adding a provider

Create `src/services/providers/myProvider.js`:

```js
import { config } from '../../config.js';
import { SYSTEM_PROMPT, userPrompt } from '../../utils/prompt.js';
import { fetchJson } from '../../utils/http.js';

export const myProvider = {
  name: 'myprovider',
  isConfigured() {
    return Boolean(config.myprovider.apiKey);
  },
  async solve({ imageBase64, mimeType, language }) {
    const json = await fetchJson('https://api.example.com/v1/vision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${config.myprovider.apiKey}`
      },
      body: JSON.stringify({ /* provider-specific shape */ })
    });
    return json.output_text; // raw model text; the service parses it
  }
};
```

Then register it in the `providers` map in `aiService.js`, add its keys to `config.js`
and `.env.example`, and set `AI_PROVIDER=myprovider`. The prompt, the JSON contract,
the timeout handling and the "never guess" rule are inherited. Nothing changes in the
Android app.

---

## Deployment

**Docker**

```bash
docker build -t smartcalc-backend .
docker run -p 3000:3000 -e AI_PROVIDER=openai -e OPENAI_API_KEY=sk-... smartcalc-backend
```

**PaaS (Render, Railway, Fly.io, Heroku)** — root directory `backend`, build
`npm install`, start `npm start`, and add the keys as secret environment variables.
The platform's `PORT` is picked up automatically.

**VPS** — run behind nginx or Caddy with HTTPS and keep the process alive with
`systemd` or `pm2`. Serve the app over HTTPS in production; the Android client permits
cleartext HTTP only for `10.0.2.2`, `localhost` and `127.0.0.1`.

Provider error bodies are logged server-side and never returned to the client.
