# Secret Chat

Android chat application + separate Node.js backend + Nuxt web client.

## Implemented features

- Create chat with any name longer than 3 chars.
- Optional chat password.
- Join chat by chat name.
- User profile with avatar and nickname.
- Default random nickname for new users.
- Send text messages.
- Send files.
- Record and send audio messages.
- Record and send video messages.
- Real-time message updates via WebSocket.

## Project structure

- `app/` - Android client (Jetpack Compose)
- `backend/` - Node.js backend (Express + WebSocket)
- `web-client/` - Browser client (Nuxt 3)

## Backend setup

```bash
cd /Users/mac/AndroidStudioProjects/Secret/backend
npm install
cp .env.example .env
npm start
```

Default backend URL: `http://localhost:3100`

Backend now uses MySQL (instead of `store.json` as primary storage).

Required MySQL env vars are in:
- `/Users/mac/AndroidStudioProjects/Secret/backend/.env.example`

Minimal MySQL setup example:

```sql
CREATE DATABASE IF NOT EXISTS secret_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

If your MySQL has password for `root`, set it in `DB_PASSWORD` inside `.env`.

## Android setup

1. Start backend first.
2. Open project in Android Studio.
3. Run app on emulator.

API base URL is configured in:
- `/Users/mac/AndroidStudioProjects/Secret/app/src/main/java/com/appswebnetkz/secret/data/network/NetworkModule.kt`

For another network/IP, replace host in `BASE_API_URL` and `BASE_WS_URL`.

## Web client setup (Nuxt)

```bash
cd /Users/mac/AndroidStudioProjects/Secret/web-client
npm install
npm run dev
```

Default URL: `http://localhost:5173`

If backend runs on LAN host, set env vars:

```bash
NUXT_PUBLIC_API_BASE=http://192.168.0.17:3100/api \
NUXT_PUBLIC_WS_BASE=ws://192.168.0.17:3100/ws \
npm run dev
```

## Local checks

- Android Kotlin compile check:

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:compileDebugKotlin
```

- Backend syntax check:

```bash
node --check /Users/mac/AndroidStudioProjects/Secret/backend/src/server.js
```
