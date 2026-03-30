# RUNE-Watch

WearOS client for the RUNE Ember ambient AI ecosystem.

Ember runs on your Galaxy Watch (and any WearOS device) as a lightweight companion that:
- Receives cross-device commands from your Windows/Linux/Android Ember instances
- Sends quick-action commands back to the host device (toggle Desktop, Chat, Pass-Thru, etc.)
- Displays the current Ember expression/state on a round-watch UI
- Exposes an Ember Tile for the WearOS tile carousel
- Provides an Ember Complication for compatible watch faces

## Architecture

```
[ Galaxy Watch ]
       │  WebSocket (wss://api.rune-systems.com)
       ▼
[ RUNE-Backend DeviceBusDO ]
       │  WS / REST
       ▼
[ Windows / Linux / Phone Ember instances ]
```

Device commands flow bidirectionally. The watch can trigger actions on the desktop
(show help, toggle surfaces) and the desktop pushes ring-update graphs to the watch.

## Prerequisites

- Android Studio Meerkat (2024.3+) or later
- WearOS emulator API 35 / physical Galaxy Watch 5+ (WearOS 3.x)
- RUNE-Backend deployed (or running `wrangler dev` locally)
- Auth token from RUNE-Portal sign-in

## First-time setup

1. Open in Android Studio
2. Run on WearOS emulator or physical device
3. Credentials (userId + auth token) are persisted via DataStore —
   set them in `DeviceBusClient.saveCredentials()` from a settings flow (TODO)

## Project structure

```
app/src/main/kotlin/com/rune/watch/
  MainActivity.kt               — Entry point, lifecycle
  EmberTileService.kt           — WearOS Tile (swipe-left pages)
  EmberComplicationService.kt   — Watch face complication data source
  presentation/
    EmberApp.kt                 — Root Composable + navigation
    EmberScreen.kt              — Main watch face UI
    theme/
      Color.kt                  — Ember dark palette
      Type.kt                   — Typography scale
      Theme.kt                  — MaterialTheme wrapper
  bus/
    DeviceBusClient.kt          — WebSocket client + command dispatch
    DeviceBusService.kt         — Foreground service (keeps WS alive)
```

## Related repos

| Repo | Platform | Description |
|---|---|---|
| [RUNE-Client](https://github.com/Jared-N-Hurst/RUNE-Client) | Windows / Linux / macOS / Android | Primary Tauri overlay |
| [RUNE-Backend](https://github.com/Jared-N-Hurst/RUNE-Backend) | Cloudflare Workers | API gateway + Durable Objects |
| [RUNE-Portal](https://github.com/Jared-N-Hurst/RUNE-Portal) | Web | Teacher / admin portal |
| [ghost-core](https://github.com/Jared-N-Hurst/ghost-core) | Windows (localhost) | C# animation + AI engine |
| RUNE-Watch *(this repo)* | WearOS | Galaxy Watch companion |
