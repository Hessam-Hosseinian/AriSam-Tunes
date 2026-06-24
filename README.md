# AriSam Tunes

AriSam Tunes is a full-stack music streaming prototype with a Ktor backend and a native Android app written in Kotlin and Jetpack Compose. It includes authentication, catalog/search APIs, playlists, offline/local storage, Media3 playback, realtime FFT visualizer support, social profile flows, chat, bilingual English/Persian resources, and release-oriented Android build configuration.

## Stack

- Backend: Kotlin, Ktor, PostgreSQL, Flyway, JWT access/refresh tokens, WebSockets, Docker Compose.
- Android: Kotlin, Jetpack Compose, Hilt, Ktor client, Room, DataStore, Paging 3, WorkManager, Coil, Media3/ExoPlayer.
- Music import: local `music_data/` scan, ffprobe/ffmpeg metadata extraction, MP3 tag fallback, cover extraction, generated demo tracks when the catalog is too small.

## Repository layout

```text
backend/        Ktor API, database migrations, seed pipeline, tests
android/        Android application
music_data/     Local music files and extracted covers
docs/           Setup helper files
```

Progress is tracked in `.codex-progress.txt`; use it to resume milestone work without guessing which commits are already complete.

## Backend setup

The quickest path is Docker Compose:

```bash
cp .env.example .env
docker compose up --build
```

The backend listens on `http://localhost:8080` by default and exposes versioned API routes under `/api/v1/`.

Useful checks:

```bash
curl http://localhost:8080/api/v1/health
cd backend && ./gradlew test
```

To run locally without Docker, start PostgreSQL and then:

```bash
cd backend
./gradlew run
```

Important environment variables:

- `PORT` defaults to `8080`.
- `PUBLIC_BASE_URL` controls generated audio/cover URLs.
- `DB_ENABLED`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` configure PostgreSQL.
- `JWT_SECRET` must be changed for any shared or production-like environment.
- `MUSIC_DATA_FOLDER` defaults to `music_data`.

Docker Compose also accepts `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, and `BACKEND_PORT`.

## Database and seed data

Flyway migrations create the schema automatically when the backend starts. The default local database is:

- database: `arisam_tunes_db`
- user: `arisam`
- password: `arisam_dev_password`

The backend scans `music_data/` for audio files. Metadata is extracted from embedded tags and ffprobe; sidecar/folder naming fallbacks are used when tags are missing. If fewer than 50 tracks are available, the seed pipeline generates short demo MP3 files under `music_data/demo/` so the UI still has enough catalog data.

Run the seed task manually when needed:

```bash
cd backend
./gradlew seedMusic
```

No default login account is seeded. Create an account from the Android register screen or call `POST /api/v1/auth/register`. Login uses JWT access tokens plus refresh tokens.

## Android setup on a physical phone over Wi‑Fi

Put the backend LAN URL in the repository-level `local.properties` file, not inside `android/`:

```properties
API_BASE_URL=http://192.168.1.23:8080
```

Find the PC LAN IP with one of these:

```bash
hostname -I
ip route get 1.1.1.1
```

On Windows use `ipconfig`; on macOS use `ifconfig` or `ipconfig getifaddr en0`.

Make sure the phone and PC are on the same Wi‑Fi network, the backend is listening on `0.0.0.0`, and the firewall allows inbound traffic to the backend port.

Build and install:

```bash
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For an emulator, omit `API_BASE_URL`; the app defaults to `http://10.0.2.2:8080`.

## Myket Maven mirror

The Android Gradle settings already include the Myket Maven mirror before Google and Maven Central. If your local Maven/Gradle environment needs a global mirror, copy or adapt:

```bash
docs/maven-settings-example.xml
```

## Release notes

The release build uses ProGuard/R8 rules for Kotlin serialization, Ktor/OkHttp, Hilt, Room, Coil, Media3, WorkManager, and app DTO/entity classes. Release signing is intentionally not configured in this repository; add a private keystore configuration outside version control before publishing.

Known limitations for this prototype:

- Release signing and store metadata are not included.
- There is no seeded auth user; testers should register their own account.
- Some UI and playback edge cases still need device QA after milestone completion.
