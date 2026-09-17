# SmartCalc AI

Android calculator with an AI photo solver. Type a calculation, or photograph a maths
problem and get the recognised problem, the steps and the final answer.

The app UI is in Serbian (Latin script). All text lives in string resources, so another
language is a matter of adding one `values-xx/strings.xml` file — English is already
included in `values-en/`.

---

## Features

**Calculator (works fully offline)**
- Digits 0–9, decimal point, `+`, `−`, `×`, `÷`, `%`, parentheses, `+/-`, `C`, backspace, `=`
- Correct operator precedence: `12 + 5 × 3 = 27`
- Decimals, negative numbers, percentages (`200 + 10% = 220`, `200 × 10% = 20`), powers
- Implicit multiplication: `2(3+4) = 14`; unclosed parentheses are closed automatically
- Division by zero and invalid input are reported as messages, never as a crash
- Live preview of the result while typing

**AI photo solver (needs internet)**
- `Slikaj zadatak` opens the camera (CameraX)
- `Izaberi sliku` opens the system photo picker
- Preview the photo, retake it, or send it for solving
- Result screen shows: photo → *Prepoznat zadatak* → *Postupak* → *REŠENJE*
- Handles arithmetic, fractions, percentages, powers, roots, equations, algebra,
  systems of equations, geometry and word problems, handwritten or printed
- **Never guesses.** If recognition is unreliable the app shows:
  *"Nisam dovoljno jasno prepoznao zadatak. Molimo fotografišite zadatak ponovo."*

**History (Room, local)**
- Calculator calculations and AI-solved problems, kept apart by a filter
- Expression / recognised problem, result, date and time
- AI entries also store the steps and a copy of the photo
- Tap a calculator entry to reuse it; delete one entry or all of them

**Security**
- The AI API key exists only in the backend environment. It is not in the Kotlin code,
  the manifest, `strings.xml`, `BuildConfig`, SharedPreferences, the APK or this repo.

---

## Project layout

```
SmartCalcAI/
├── android/                  Android Studio project (Kotlin, Compose, Material 3)
│   ├── app/
│   │   └── src/main/java/com/smartcalc/ai/
│   │       ├── calculator/   offline expression engine + key handling
│   │       ├── data/         Room database, Retrofit client, repositories
│   │       ├── navigation/   Compose navigation graph
│   │       ├── ui/           calculator, history, solver screens (MVVM)
│   │       └── util/         image processing, connectivity, formatting
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
├── backend/                  Node.js + Express API that talks to the AI provider
│   ├── src/
│   ├── package.json
│   ├── .env.example
│   └── README.md
├── README.md
├── .gitignore
└── LICENSE
```

Request flow:

```
Android app  ──POST /api/solve (base64 image)──▶  Backend  ──▶  AI vision model
                                                     ▲
                                              API key lives here
```

---

## 1. Open the Android project

1. Install **Android Studio Hedgehog (2023.1.1) or newer** and a JDK 17
   (Android Studio ships one).
2. `File ▸ Open` and select the **`android`** folder (not the repository root).
3. Let Gradle sync. The wrapper downloads Gradle 8.2 on first run.
4. Install SDK Platform 34 if Android Studio asks for it.

Minimum supported Android version: 7.0 (API 24).

---

## 2. Set up the backend

```bash
cd backend
npm install
cp .env.example .env
```

Edit `.env` and set your provider and key:

```env
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```

Run it:

```bash
npm run dev      # auto restart on change
npm start        # production
```

Check it is alive:

```bash
curl http://localhost:3000/api/health
```

Run the backend tests:

```bash
npm test
```

---

## 3. Configure the AI provider

The backend never hardcodes a vendor. `src/services/aiService.js` holds a registry and
each provider is one small file implementing the same three functions.

| Provider | `AI_PROVIDER` | Required variables |
| --- | --- | --- |
| OpenAI (and OpenAI-compatible endpoints) | `openai` | `OPENAI_API_KEY`, optional `OPENAI_MODEL`, `OPENAI_BASE_URL` |
| Google Gemini | `gemini` | `GEMINI_API_KEY`, optional `GEMINI_MODEL`, `GEMINI_BASE_URL` |

Switching providers is an environment change and a restart. The Android app is untouched.

**Adding a third provider**

1. Create `backend/src/services/providers/myProvider.js` exporting:
   ```js
   export const myProvider = {
     name: 'myprovider',
     isConfigured() { return Boolean(config.myprovider.apiKey); },
     async solve({ imageBase64, mimeType, language }) { /* return raw model text */ }
   };
   ```
2. Register it in the `providers` map in `aiService.js`.
3. Add its keys to `config.js` and `.env.example`.

Both the prompt and the response parsing are shared, so a new provider inherits the
"never invent an answer" rule automatically.

---

## 4. Deploy the backend

The only hard requirement is that the API key is supplied as an environment variable.

**Docker**

```bash
cd backend
docker build -t smartcalc-backend .
docker run -p 3000:3000 \
  -e AI_PROVIDER=openai \
  -e OPENAI_API_KEY=sk-... \
  smartcalc-backend
```

**Render / Railway / Fly.io / Heroku**

- Root directory: `backend`
- Build command: `npm install`
- Start command: `npm start`
- Add `AI_PROVIDER` and the matching `*_API_KEY` as environment variables (secrets)
- The platform sets `PORT` for you

**Own VPS**

```bash
npm install --omit=dev
AI_PROVIDER=openai OPENAI_API_KEY=sk-... node src/server.js
```

Put it behind nginx or Caddy with HTTPS, and keep it running with `systemd` or `pm2`.

Use HTTPS in production: the app only allows cleartext HTTP for `10.0.2.2`,
`localhost` and `127.0.0.1` (see `res/xml/network_security_config.xml`).

---

## 5. Point the app at the backend

The address is a build-time Gradle property, never a secret.

| Where you run the app | Value |
| --- | --- |
| Emulator + backend on the same machine | `http://10.0.2.2:3000/` (the default) |
| Physical phone + backend on your PC | `http://<your-LAN-IP>:3000/` |
| Deployed backend | `https://your-backend.example.com/` |

Set it in any of these places:

- `android/gradle.properties` → `SMARTCALC_BACKEND_URL=https://...`
- `android/local.properties` (git-ignored; copy from `local.properties.example`)
- command line: `./gradlew assembleDebug -PSMARTCALC_BACKEND_URL=https://...`

For a physical phone on plain HTTP, add your LAN IP to the `domain-config` block in
`android/app/src/main/res/xml/network_security_config.xml`, or use HTTPS.

---

## 6. Build the APK

```bash
cd android
chmod +x gradlew        # first time on macOS/Linux
./gradlew assembleDebug
```

Output:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device:

```bash
./gradlew installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Other useful tasks:

```bash
./gradlew test             # unit tests for the calculator engine
./gradlew assembleRelease  # unsigned release build (configure signing first)
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

---

## 7. Upload to GitHub

```bash
cd SmartCalcAI
git init
git add .
git status            # confirm no .env and no local.properties are staged
git commit -m "SmartCalc AI: calculator + AI photo solver"
git branch -M main
git remote add origin https://github.com/<username>/SmartCalcAI.git
git push -u origin main
```

`.gitignore` already excludes `.env`, `local.properties`, `build/`, `node_modules/`,
keystores and APKs, while keeping the Gradle wrapper (which must be committed).

If a key is ever committed by accident, revoke it at the provider immediately —
rewriting git history is not enough.

---

## 8. Security requirements

- API keys live **only** in backend environment variables.
- Never place a key in Kotlin source, `AndroidManifest.xml`, `strings.xml`,
  `BuildConfig`, SharedPreferences, the APK, or this repository.
- `.env` is git-ignored; only `.env.example` is committed, with placeholder values.
- The backend never echoes provider error payloads back to the client; they go to the
  server log only.
- Rate limiting (`RATE_LIMIT_MAX`), a request size limit (`MAX_IMAGE_MB`) and
  `helmet` headers are enabled by default.
- The app requests the camera permission at runtime and keeps working if it is denied.

---

## Error handling

Every one of these produces a message and a way forward, never a crash:

| Situation | What the user sees |
| --- | --- |
| No internet | *Za rešavanje zadatka sa slike potrebna je internet veza.* |
| Backend down | *Server nije dostupan. Pokušajte ponovo kasnije.* |
| Timeout | *Isteklo je vreme čekanja. Pokušajte ponovo.* |
| AI error / invalid response | *Došlo je do greške pri rešavanju zadatka.* / *Neispravan odgovor servera.* |
| Unreadable photo | *Nisam dovoljno jasno prepoznao zadatak. Molimo fotografišite zadatak ponovo.* |
| Blurry photo (detected on device) | *Slika je nejasna. Molimo fotografišite zadatak ponovo.* |
| Camera permission denied | Explanation + *Dozvoli pristup* / *Otvori podešavanja* |
| Gallery selection cancelled | *Izbor slike je otkazan.* with a retry button |
| Unsupported or damaged image | *Slika nije podržana ili je oštećena.* |
| Division by zero | *Deljenje nulom nije dozvoljeno* |

---

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Kotlin Coroutines & Flow · Navigation
Compose · CameraX · Room · Retrofit + OkHttp · Coil · Node.js + Express

## License

MIT — see [LICENSE](LICENSE).
