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
| 1 | Intent dump stub | **Done** — see `CAPTURE-FINDINGS.md` |
| 2 | Room + capture sheet + save | **Done** |
| 3 | Library list with the real card | **Done** — Spectral bundled |
| 4 | Read in context via Custom Tabs | Not started |
| 5 | Metadata enrichment worker | Not started |
| 6 | Search, delete, undo | Not started |
| 7 | JSON export/import | Not started |

Step 1 is done, and it changed the design. `CAPTURE-FINDINGS.md` records what
the intents actually contain — read it before touching capture, because it
contradicts §4 on two points that matter and documents a product problem that
no code fixes. In short: Chrome sends the whole passage rather than just the
fragment, and the Substack app suppresses `PROCESS_TEXT` entirely.

The dump screen stays in `src/debug` for now (share sheet entry: **Snips:
dump intent**) since the open questions at the end of that file still need
answering on a device.

The environment this code was written in can't reach `dl.google.com`, so it has
no Android SDK and most of it has never met a compiler. Two parts have: the
pure-Kotlin capture logic runs against its tests on a JVM-only project, and
Room's annotation processor demonstrably ran — `app/schemas` holds the schema
it generated on a real build.

```bash
./gradlew test              # the 59 that are known to pass
./gradlew assembleDebug     # first real compile
```

## Layout

```
app/src/main/java/app/snips/
  MainActivity.kt              Library host, and the copy action.
  capture/                     The sheet, the parser, URL and fragment logic.
  (src/debug) IntentDumpActivity.kt  Step 1's dump. Test builds only.
  ui/SnipCard.kt               The card. §6's anatomy, passage as hero.
  ui/LibraryScreen.kt          The list and its empty state.
  ui/RelativeTime.kt           "3d ago", ported from the prototype.
  ui/theme/                    §6 design tokens: Color, Type, Theme.
  res/font/                    Spectral, bundled as three static faces.
  assets/licenses/             The OFL licence — res/ only accepts font files.
```

## Notes on the scaffold

- **Single module, no DI**, per §3. The version catalog declares Room,
  OkHttp, Jsoup, Coil, WorkManager and Custom Tabs already, but `app/build.gradle.kts`
  only wires what the current step needs — each gets added as its step lands.
- **Material 3 is present but held at arm's length.** `SnipTheme.colors` carries the
  §6 tokens; the Material scheme is fed the same palette so dialogs and ripples
  don't drift. No dynamic colour — the app should look like Substack, not like
  the wallpaper.
- **Spectral is bundled** in `res/font`, as three static faces rather than the
  variable font §6 assumed — that is how Google Fonts publishes it. Its OFL
  licence ships at `assets/licenses/Spectral-OFL.txt`; `res/font` accepts only
  `.xml`, `.ttf`, `.ttc` and `.otf`, and aapt2 fails the build on anything else.
