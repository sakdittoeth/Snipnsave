# HANDOVER — Snips for Android

Read this first, then `web-prototype/README.md` and `web-prototype/index.html`. That prototype is the reference implementation: it defines the visual language, the data model, and the deep-link behaviour, and it runs — open it in a browser to see what you're matching. This document says what changes when it becomes a native app.

The Android app goes in `android/` at the repo root. Nothing in `web-prototype/` should be edited as part of this work; it's frozen reference.

---

## 1. What we're building and why

Substack has no reader-side highlighting. You can restack a quote, but that publishes it to Notes — there's no private "save this passage." Snips is the private version: capture a passage while reading, keep it in a library that looks like the app it came from, and get back to the exact sentence later.

The user reads on an Android phone. The Substack app is the main reading surface.

**Success looks like:** two taps from reading a sentence to having it saved, and one tap from a saved card back to that sentence in context.

## 2. What already exists

A working PWA in `web-prototype/`. Ship-quality for the parts that matter:

| Carries over unchanged | Notes |
|---|---|
| Card design | Passage is the hero; title/author/cover are attribution in the footer. Tokens in §6. |
| Data model | See §5. Keep the field names so exported JSON stays compatible with the prototype. |
| URL hygiene | The `JUNK` list and `cleanUrl()` in `web-prototype/index.html`. Port them literally. |
| Text-fragment construction | `deepLink()` — start/end form for quotes over 12 words. |

**Deliberately dropped on native:** `web-prototype/metadata-worker.js`. It only existed to dodge browser CORS. A native app fetches article HTML directly — do the Open Graph parse on-device and delete the worker from the plan entirely.

## 3. Stack

- Kotlin, Jetpack Compose, Material 3
- Room for persistence, Flow-backed queries
- OkHttp + Jsoup for metadata, Coil for images
- Chrome Custom Tabs for opening articles (Custom Tabs honour text fragments; a raw `ACTION_VIEW` may open the Substack app instead, which does not)
- minSdk 26, targetSdk current
- Single-module app, no DI framework. This is small; don't over-scaffold it.

## 4. Capture — the part that needs the most care

Three entry points, in priority order. All converge on one `CaptureSheet` that shows the parsed result before saving, so a bad parse is always visible and fixable.

### 4a. Chrome "Link to highlight" (best case, build first)

Chrome Android's share menu offers a highlight link when text is selected. It arrives as `ACTION_SEND` `text/plain` containing a URL that already has the fragment:

```
https://pub.substack.com/p/the-post#:~:text=the%20selected%20passage
```

Detect `#:~:text=` in an incoming URL and **decode the passage out of it**. You get the quote and a verified working deep link in one shot. Handle both fragment forms:

- `#:~:text=quote`
- `#:~:text=start,end` (join with an ellipsis, flag it as truncated so `deepLink()` isn't rebuilt from a partial quote)
- Optional `prefix-,quote,-suffix` context syntax — strip the `-` delimited parts

### 4b. `ACTION_PROCESS_TEXT` (best UX, build second)

```xml
<intent-filter>
    <action android:name="android.intent.action.PROCESS_TEXT" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

Puts Snips in the floating selection toolbar system-wide, including inside the Substack app. Register it read-only (`PROCESS_TEXT_READONLY` handling — don't return edited text).

**The catch:** you get the passage but no URL. Recovery, in order:
1. Scan the text itself for a URL (Substack sometimes appends one)
2. Check the clipboard for a Substack URL
3. Check whether a snip was saved from the same article in the last few minutes and offer that article
4. Fall back to an empty URL field in the sheet — a snip with no link is still worth keeping

### 4c. `ACTION_SEND` from the share sheet (fallback)

```xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

Read `EXTRA_TEXT` and `EXTRA_SUBJECT`. Substack's own share often bundles the link inside `EXTRA_TEXT` — extract it with the same regex the PWA uses.

### Verify early

Before building the library UI, install a stub that dumps every incoming intent's extras to the screen, then test the real path: Substack app → select text → share, and Chrome → select text → share. **What those two actually deliver decides the rest of the app.** Don't trust this document over what the intents contain.

## 5. Data model

```kotlin
@Entity(tableName = "snips")
data class Snip(
    @PrimaryKey val id: String,        // UUID
    val text: String,                  // the passage
    val url: String,                   // cleaned, no fragment, no tracking params
    val note: String = "",             // user's own note
    val publication: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val logoUrl: String = "",
    val savedAt: Long,                 // epoch millis
    val enriched: Boolean = false,     // metadata fetch completed
    val fragmentTruncated: Boolean = false  // quote came from a start,end link
)
```

Enrichment runs as a WorkManager job, not inline: the snip saves instantly with `publication` derived from the hostname, and the card fills in when the fetch returns. Metadata failure is never a save failure.

Metadata sources, in order — same logic as `web-prototype/metadata-worker.js`, which is a working reference implementation of this parse:
1. `{origin}/api/v1/posts/by-slug/{slug}` for `/p/` URLs — clean title, `cover_image`, `publishedBylines`. Undocumented, so treat any failure as normal.
2. Open Graph tags via Jsoup: `og:site_name`, `og:title`, `og:image`, `author`
3. Hostname-derived publication name

Cache cover images to disk with Coil. Hotlinking Substack's CDN forever is rude and breaks if a post is deleted.

## 6. Design tokens

Match the prototype. Light / dark:

```
paper   #FFFFFF / #111110    surface
tint    #FAF9F8 / #1C1B1A    input backgrounds
ink     #1A1A1A / #F0EFED    primary text
muted   #6B6B6B / #9A9793    metadata
hair    #E7E5E4 / #2E2C2A    dividers
mark    #FF6719 / #FF6719    Substack orange — the highlight rule, and nothing else
```

Type: Spectral (bundle the variable font) at 18sp/1.6 for passages; system sans for all chrome. The orange appears in exactly two places — the rule beside the passage and the primary button. Resist spreading it.

Card anatomy, top to bottom: publication logo + name + relative timestamp / passage with orange left rule / optional note in italic muted / post title + byline with cover thumbnail right-aligned / actions row.

Cards are separated by hairlines, not elevated surfaces. No Material cards, no shadows — Substack's feed is flat and this should read as continuous with it.

## 7. Build order

1. Intent dump stub — confirm what Substack and Chrome actually send (§4)
2. Room + capture sheet + save. No metadata, no styling. Prove capture works end to end.
3. Library list with the real card design
4. "Read in context" via Custom Tabs — verify the fragment actually scrolls Substack posts
5. Metadata enrichment worker
6. Search, delete, undo
7. JSON export/import via Storage Access Framework, matching the PWA's shape

## 8. Decisions still open

- **Sync.** Currently none, by design. If it's wanted later, the Room DAO is the only seam that changes.
- **Editing a passage after saving.** Trimming a quote breaks the text fragment. Either keep the original for linking and show the edited version, or disallow edits.
- ~~**Non-Substack sites.**~~ **Decided: any article is supported.** The Open Graph parse in `data/ArticleMetadata.kt` is site-agnostic; `by-slug` is an optional seed for Substack posts only. Settled before the metadata layer was written, as this entry advised.
- **Paywalled posts.** Links work when signed in on the device. Don't fetch or store article body text; only the passage the user selected.

## 9. Constraints

- Never scrape or store full article content. Only user-selected passages, stored locally, for personal use.
- No analytics, no accounts, no network calls beyond metadata for URLs the user explicitly saved.
- The Substack `api/v1` endpoint is undocumented and can disappear. It must never be load-bearing — OG tags are the real path.
