# Pulse

A Fabric profiler for Minecraft dedicated servers.

It records CPU stacks, tick times, memory, GC, worlds, network, and errors, then opens the result in a web viewer.

- **Minecraft:** 26.2 (Fabric)
- **Author:** BLOODWOLF
- **License:** MIT
- **Viewer:** https://bloodwoof.github.io/PULSE-Profiler/
- **Downloads:** https://github.com/BLOODWOOF/PULSE-Profiler/releases

## Install

1. Install Fabric Loader and Fabric API on the server.
2. Put `pulse-*.jar` in the server `mods` folder.
3. Restart the server.

## Commands

Operators only.

| Command | What it does |
| --- | --- |
| `/pulse profiler start [seconds] [intervalMs]` | Start CPU sampling |
| `/pulse profiler stop` | Stop and save a report |
| `/pulse health` | Snapshot of TPS, memory, worlds, and similar |
| `/pulse heap` | Class histogram |
| `/pulse errors` | Recent errors |
| `/pulse tps` | Live TPS / MSPT |
| `/pulse reload` | Reload `config/pulse.json` |
| `/pulse help` | Command list |

Reports are written to `config/pulse/reports/`. Drop those files on the viewer.

## Config

`config/pulse.json` is created on first launch. Common options:

| Key | Meaning |
| --- | --- |
| `sampleOnlyServerThread` | Only sample the server thread |
| `includeWaitingThreads` | Include idle/waiting stacks |
| `actionBarHud` | Show TPS / MSPT on the action bar |
| `saveLocal` | Keep a copy under `config/pulse/reports/` |
