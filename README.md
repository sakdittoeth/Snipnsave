# Snips

A private highlight library for Substack. Save a passage while reading, keep it in a library that looks like the app it came from, and get back to the exact sentence later.

Substack has no reader-side highlighting — quote-restacking publishes to Notes, and there's no private equivalent. This fills that gap.

## Repo layout

```
HANDOVER.md         Spec for the Android app. Start here.
web-prototype/      Working PWA. Frozen reference — don't edit.
android/            The native app. Scaffolded; at step 1 of §7.
```

## For a fresh Claude Code session

Read `HANDOVER.md`, then `web-prototype/README.md` and `web-prototype/index.html`. The prototype is not pseudocode — it runs, and `cleanUrl()`, `deepLink()`, the tracking-param list, and the Open Graph parse in `metadata-worker.js` are all working implementations to port rather than reinvent.

Build order and open decisions are in `HANDOVER.md` §7 and §8. Step 1 is a throwaway intent-dump stub; don't skip it, because what Substack's app and Chrome actually put in a share intent decides the shape of everything after it.

## Installing it

Releases are built and signed by `.github/workflows/release.yml` when a `v*`
tag is pushed, and attached to a GitHub Release. `docs/index.html` is the page
to send people — enable GitHub Pages from the `docs/` folder to serve it.
Setup, and what to tell whoever you send it to, is in
`android/DISTRIBUTING.md`.

## Trying the prototype

Open `web-prototype/index.html` in a browser. Tap **+**, paste a passage and a Substack URL. Covers and author names need the metadata worker deployed — see `web-prototype/README.md`.
