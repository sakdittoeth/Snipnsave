# What the intents actually contain

`HANDOVER.md` §4 ends: *"Don't trust this document over what the intents
contain."* This is what they contained, read off a real phone with the step 1
dump. Where this file and §4 disagree, this file is right.

Captured 2026-09-05, Chrome on Android, reading substack.com.

## Chrome "Link to highlight" — works, but not as §4a describes

```
action = android.intent.action.SEND
type   = text/plain

EXTRA_TEXT    "President Biden will be at Ground Zero in NYC on the 25th
               anniversary of 9/11 with every other living President.
               <blank line>
               You know who isn't going? Donald Trump… because he wasn't
               allowed to give a speech. Pathetic."
              https://substack.com/#:~:text=President%20Biden%20will,a%20speech.%20Pathetic.

EXTRA_SUBJECT "Including link: https://substack.com/"
EXTRA_TITLE   "Including link: https://substack.com/"
sourcePackageName            "com.android.chrome"
org.chromium.chrome.extra.TASK_ID   36090
```

Three things follow.

**The passage arrives whole in `EXTRA_TEXT`; the fragment holds only its ends.**
§4a says to decode the quote out of the fragment. Doing that would drop
everything between "President Biden will" and "a speech. Pathetic." — the
entire middle of what the reader selected. The shared text wins, and a quote
that arrived whole is not marked `fragmentTruncated`.

**`EXTRA_SUBJECT` is not the post title.** It is Chrome's own share label,
"Including link: <url>". It is no use as a title fallback, which is what §5
was holding it for. `EXTRA_TITLE` carries the same string.

**A text fragment can end in a full stop.** `…%20Pathetic.` — trimming
trailing punctuation off the URL, which is right for a link pasted mid
sentence, corrupts the fragment and the parse of the text around it. URLs
carrying `#:~:` are now exempt.

## The Substack app: `PROCESS_TEXT` never reaches it

§4b's premise is that `ACTION_PROCESS_TEXT` puts Snips in the floating
selection toolbar system-wide, "including inside the Substack app". It does
not. Selecting text there offers **Copy · Restack quote · Cancel** and *no
overflow control* — a custom menu that replaces the system one, with no way
to add to it.

`PROCESS_TEXT` is still worth registering; it works in Chrome and most other
apps. It just cannot be the route in for the app §1 calls the main reading
surface.

**What works instead:** select the passage → **Copy** → the post's share
menu → **Save snip**. The share carries the post link and no passage; the
clipboard carries the passage; `parseShare` pairs them. The sheet says the
passage came from the clipboard so a stale one is visible rather than
silently saved.

The clipboard is only read when it could supply a missing half — see
`shareNeedsClipboard`. From Android 12 every read raises a system toast, and
one shouldn't be earned for nothing.

## Reading in the feed produces a link to nowhere

The capture above is the strongest finding, and no code fixes it. The URL is
`https://substack.com/` — the feed root, because the reader was in
substack.com's own feed rather than on the post's page. The passage is not at
that address and will not be there tomorrow.

The app can't recover the post URL from a feed share, so it says so:
`isBareSiteUrl` flags a URL whose path is empty or `/`, and the capture sheet
warns that "read in context" won't find its way back. The snip still saves —
half the product is better than none.

**The habit that avoids it:** open the post on its own page, so the address
bar reads `something.substack.com/p/some-slug`, and share from there.

## Still unknown

- Whether the Substack app's **post-level** share (the ⋯ menu on a post,
  rather than on selected text) hands over a proper `/p/` URL. If it does,
  the Copy-then-share flow above is sound. If it hands over a
  `substack.com/…` redirect, capture from that app is limited to passages
  with no usable link.
- What Substack's **Restack quote** puts on the clipboard, if anything.
- Whether other readers — Feedly, Pocket, Reeder — expose `PROCESS_TEXT`.
