# Snips — Android

The native app. Spec is `../HANDOVER.md`; the reference implementation is
`../web-prototype/`.

## Build

```bash
cd android
./gradlew installDebug
```

Needs the Android SDK (platform 35). Android Studio supplies it; otherwise
point `local.properties` at your own:

```properties
sdk.dir=/path/to/Android/sdk
```

## Where the build order is

Tracked against `HANDOVER.md` §7.

| # | Step | State |
|---|---|---|
| 1 | Intent dump stub | **Scaffolded — not yet run on a phone** |
| 2 | Room + capture sheet + save | Not started |
| 3 | Library list with the real card | Not started |
| 4 | Read in context via Custom Tabs | Not started |
| 5 | Metadata enrichment worker | Not started |
| 6 | Search, delete, undo | Not started |
| 7 | JSON export/import | Not started |

Step 1 is the one that gates everything else, and it can only be finished on
a real device: install the app, then from the Substack app select text →
**Share** → **Save snip**, and from Chrome select text → **Share** →
**Link to highlight** → **Save snip**. The screen dumps every field of the
incoming intent and has a **Copy dump** button. What those two dumps contain
decides the shape of the capture layer — see §4.

## Layout

```
app/src/main/java/app/snips/
  MainActivity.kt              Library host. Placeholder until step 3.
  capture/IntentDumpActivity.kt  Step 1. Throwaway — delete once CaptureActivity exists.
  ui/theme/                    §6 design tokens: Color, Type, Theme.
```

## Notes on the scaffold

- **Single module, no DI**, per §3. The version catalog declares Room,
  OkHttp, Jsoup, Coil, WorkManager and Custom Tabs already, but `app/build.gradle.kts`
  only wires what the current step needs — each gets added as its step lands.
- **Material 3 is present but held at arm's length.** `SnipTheme.colors` carries the
  §6 tokens; the Material scheme is fed the same palette so dialogs and ripples
  don't drift. No dynamic colour — the app should look like Substack, not like
  the wallpaper.
- **Spectral isn't bundled yet.** `PassageStyle` uses `FontFamily.Serif` as a
  stand-in; the real font drops into `res/font` in step 3.
