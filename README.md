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
