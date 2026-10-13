# Pentas

The Yasvara game and video sidebar. It opens over a running game or video app and keeps every per-game control in one place.

## What it does

- **Sidebar:** live FPS, CPU, GPU and RAM tiles; a thermal profile switcher; media controls; brightness; quick toggles and tools; a rail with session time, ping, screen recording and app shortcuts that open in a Layang window.
- **Tuner:** per-game clocks, render resolution and supersampling, display colour, GLES driver, GPU composition, touch report rate with edge and grip filtering, and Dolby profiles. The same editors appear on each game's page in the app.
- **On-screen widgets:** Classic, Mini, Processes and Temperature. Each one reads data only while it is shown.
- **Thermal and charging:** profile selection for mi_thermal_engine, custom throttle curves, charging curves and bypass charging.
- **Sessions:** frame-rate and thermal recordings with charts.

## Architecture

- The persistent main process (`android.uid.system`) runs only the thermal and clock control plane.
- Activities and the sidebar run in `:ui`.
- system_server's PentasService binds `session.SessionService` once per game or video session, so an idle device runs no sidebar code.

| Package | Contents |
|---|---|
| `session` | session lifecycle, call policy, the system settings a session borrows |
| `overlay` | sidebar windows, pages, tiles, widgets, capture |
| `ui.kit`, `ui.tuning` | shared M3 Expressive components and per-game editors |
| `app` | library, game page, global profile, settings |
| `thermal`, `charging`, `touch`, `telemetry`, `audio` | backends |

## Build

Pentas is built with the ROM:

```bash
./build.sh onyx Pentas
```

SELinux policy lives in `sepolicy/`; include `sepolicy/sepolicy.mk` from the device BoardConfig.

## Credits

Pentas began as a fork of [chaldeaprjkt's GameSpace](https://github.com/chaldeaprjkt/packages_apps_GameSpace).

## License

Apache License 2.0. See [LICENSE.md](LICENSE.md).
