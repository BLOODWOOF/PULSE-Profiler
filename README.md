# Pulse

A detailed Fabric server profiler with a web viewer.

Pulse samples CPU stacks, tick times, memory, GC, worlds, network, disk, and errors, then uploads a gzip JSON report. The viewer is a static site meant for GitHub Pages. Shareable links need a small upload API you host yourself.

Author: BLOODWOLF  
License: MIT  
Minecraft: 26.2 (Fabric)

## Layout

- `mod/` — Fabric server mod
- `website/` — Vite + React viewer (GitHub Pages)
- `api/` — report upload/storage server
- `schema/report.v1.json` — report contract

## Mod commands

All require operator permission (level 4).

| Command | What it does |
| --- | --- |
| `/pulse profiler start [seconds] [intervalMs]` | Start CPU sampling (default 30s, 10ms) |
| `/pulse profiler stop` | Stop, save, upload, print a viewer link |
| `/pulse health` | Metrics snapshot without a long sampler run |
| `/pulse heap` | Class histogram plus memory snapshot |
| `/pulse errors` | Recent ERROR/FATAL and uncaught exceptions |
| `/pulse tps` / `/pulse status` | Live colored TPS / MSPT line |
| `/pulse reload` | Reload `config/pulse.json` |
| `/pulse help` | Command list |

Reports are written to `config/pulse/reports/` when `saveLocal` is true.

After first launch, `config/pulse.json` includes sampler, HUD, and upload options. Useful knobs:

```json
{
  "uploadUrl": "http://127.0.0.1:8787/reports",
  "viewerBaseUrl": "http://127.0.0.1:5173",
  "defaultDurationSeconds": 30,
  "defaultIntervalMs": 10,
  "sampleOnlyServerThread": false,
  "includeWaitingThreads": true,
  "sampleGroups": [],
  "actionBarHud": false,
  "includeHeapOnProfilerStop": true,
  "scanWorldSize": false,
  "anonymizePlayers": false,
  "pruneBelowPercent": 0.25
}
```

Set `viewerBaseUrl` to your GitHub Pages origin, for example `https://you.github.io/pulse`.

## Website (GitHub Pages)

```bash
cd website
npm install
npm run dev
```

Production build for project pages (`https://<user>.github.io/pulse/`):

```bash
cd website
set GITHUB_PAGES=true
npm run build
```

`VITE_API_BASE` is the origin of the upload API (no trailing slash), e.g. `https://pulse-api.example.com`.

The GitHub Action in `.github/workflows/pages.yml` builds and deploys the viewer.

## Upload API

```bash
cd api
npm install
npm run start
```

Listens on port `8787` by default (`PORT` to change). Stores gzip reports under `api/data/reports`.

## Local end-to-end

1. `cd api && npm install && npm start`
2. `cd website && npm install && npm run dev`
3. Drop `pulse-1.0.0.jar` into a Fabric 26.2 server `mods/` folder
4. `/pulse profiler start 10` then open the chat link
