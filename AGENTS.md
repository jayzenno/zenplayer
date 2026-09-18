# ZenPlayer — Development Rules

ZenPlayer is an Android TV / Google TV all-in-one media app (Live TV, VOD, Music).
These rules are durable — read them before making structural changes.

## Build

```bash
./gradlew assembleDebug
```

Kotlin 2.2.10, AGP 8.13.0, compileSdk/targetSdk 36, minSdk 23, Jetpack Compose
(compose-bom 2025.08.01) + `androidx.tv:tv-material`/`tv-foundation`, Media3/ExoPlayer 1.8.0,
optional VLC (`libvlc-all`) playback engine, Coil for images.

## Home Screen Rule (do not violate)

The app root (`ZenPlayerApp` in `ZenPlayerApp.kt`) shows a **four-tile horizontal home gate**
(`ZenHomeGate` in `ZenHomeGate.kt`): **Live TV | VOD | Music | Settings**. This is the primary
navigation for the entire app.

- Do NOT replace it with a sidebar, bottom nav, hamburger menu, vertical list, or poster carousel
  as the *primary* navigation.
- Visual polish (glass, animation, focus states, icons) may be improved freely.
- The four destinations and their horizontal-tile arrangement must stay.

## Architecture

- `ZenPlayerApp` owns top-level navigation between the four tiles and the exit-confirmation dialog.
- `ZenPlayerShellV6` (`ZenPlayerShellV6.kt`) is the Live TV module: its own sidebar (Home/EPG/Search/
  Settings) is *sub-navigation inside Live TV*, not a replacement for the home gate. It is reused,
  unmodified in spirit, for both the `LIVE_TV` tile (`startPage = "home"`) and the `SETTINGS` tile
  (`startPage = "settings"`) via the `embedded`/`onExitToGate` params — back navigation from either
  returns to the home gate instead of showing the exit dialog.
- `ZenVodScreen` / `ZenMusicScreen` (`ZenComingSoon.kt`) are placeholders until Phases 5/6 land.
- Provider abstractions live in `domain/provider/`: `VodProvider` (Nuvio-oriented) and
  `MusicProvider` (`EclipseMusicProvider`, `SpotifyProvider`, `AppleMusicProvider`). New backends
  implement these interfaces — the UI must not hard-code a single provider.
- `SettingsStore` / `AppSettings.kt` hold persisted UI/player/EPG/remote settings, including the
  `ZenTheme` preset enum and `glassIntensity` used by `ZenGlass` (glass alpha helper) and
  `ZenAnimatedBackdrop` (the animated light-field background).

## Android TV / D-pad Rules

- Every interactive element must be reachable and operable with D-pad + OK/Back only. Touch is
  secondary.
- Use the `tvAction` modifier (`TvInteraction.kt`) for OK/Enter activation on custom focusable rows.
- Preserve focus restoration: leaving a detail/sub-screen should return focus to where the user was.
- `FocusableCompat.kt` / `KeyCompat.kt` / `ModifierCompat.kt` are small compatibility shims used
  across the TV UI code — keep using them rather than importing the underlying APIs ad hoc, so key
  handling stays consistent.

## Provider Priority (Music/VOD, when implemented)

1. EclipseMusic — highest priority for a native (non-launcher) integration.
2. Spotify / Apple Music — only via official, documented APIs. Never bypass DRM or auth.
3. VOD — build against the Nuvio plugin/collection ecosystem; don't invent undocumented endpoints.

## Git / Process

- Check `git status` before broad changes.
- Don't rewrite large amounts of working code without reason — this codebase has been iterated on
  heavily (see git log for TV-focus fixes); prefer additive, targeted changes.
- Never commit secrets (API keys, tokens, passwords) for Spotify/Apple Music/Xtream/etc.
