# Secret Chat Web Client (Nuxt)

## Run

```bash
cd /Users/mac/AndroidStudioProjects/Secret/web-client
npm install
npm run dev
```

Open: `http://localhost:5173`

## Environment

Use these variables if backend is on another host:

- `NUXT_PUBLIC_API_BASE` (example: `http://192.168.0.17:3100/api`)
- `NUXT_PUBLIC_WS_BASE` (example: `ws://192.168.0.17:3100/ws`)

Example launch:

```bash
NUXT_PUBLIC_API_BASE=http://192.168.0.17:3100/api \
NUXT_PUBLIC_WS_BASE=ws://192.168.0.17:3100/ws \
npm run dev
```
