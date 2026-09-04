# Snips

A private highlight library for Substack. Select a passage while reading, share it in, and it lands as a card that looks like the app you took it from — with a link that reopens the article at that exact sentence.

Substack has no reader-side highlighting. Quote-restacking is the closest thing, and it publishes to Notes. This fills the private gap.

## Files

| File | Role |
|---|---|
| `index.html` | The whole app: card library, search, capture sheet, share intake |
| `manifest.webmanifest` | Makes it installable and registers it as an Android share target |
| `sw.js` | Service worker — installability plus offline access to saved snips |
| `metadata-worker.js` | Cloudflare Worker that turns an article URL into publication, title, author, cover, logo |
| `icon-192.png`, `icon-512.png` | App icons |

## 1. Host it

Any static HTTPS host. Cloudflare Pages, Netlify, or GitHub Pages all work — drag the folder in. HTTPS is required: service workers and share targets don't run over plain HTTP.

## 2. Deploy the metadata worker

Without it the app still works, but cards show no cover image, author, or real post title.

```bash
npm install -g wrangler
wrangler deploy metadata-worker.js --name snips-meta
```

Copy the resulting URL into `index.html`:

```js
const META_ENDPOINT = "https://snips-meta.you.workers.dev";
```

Then tighten `ALLOWED_CALLERS` in the worker to your app's origin, so it isn't a general-purpose open proxy.

## 3. Wire up capture

### Android

Open the hosted page in Chrome, then **Add to Home screen**. Once installed, Snips appears in the system share sheet.

In the Substack app: select text → **Share** → **Snips**. The capture sheet opens prefilled. If Substack sends the text with the link bundled in, the app pulls the URL out on its own.

### iPhone / iPad

iOS won't let a web app register as a share target, so a Shortcut stands in.

Create a shortcut named **Save snip**, turn on *Show in Share Sheet*, and accept **Safari web pages** and **Text**. Then:

1. **Get URLs from** `Shortcut Input`
2. **Get clipboard**
3. **URL Encode** the clipboard text → name it `Quote`
4. **URL Encode** the URL → name it `Link`
5. **Text**: `https://your-app-url/index.html?text=Quote&url=Link` (insert the two variables)
6. **Open URLs** with that text

Reading flow becomes: select the passage → **Copy** → **Share** → **Save snip**. Two taps more than Android, and it always gets the right URL.

## Read in Safari, not the Substack app

This matters more than any of the code. The Substack app hands out text without a dependable article URL, and it can't produce a link into the middle of a post. A browser gives you both — which is what makes "Read in context" land on your sentence instead of the headline.

## How the deep link works

Saved passages become [text fragment](https://developer.mozilla.org/en-US/docs/Web/URI/Fragment/Text_fragments) URLs:

```
https://pub.substack.com/p/the-post#:~:text=first%20six%20words,last%20six%20words
```

Long quotes use the start/end form so the link survives minor edits to the post. Supported in Chrome, Edge, and Safari 16.1+. Firefox ignores the fragment and opens the article normally, which is a soft failure rather than a broken link.

Paywalled posts open fine as long as you're signed in on that device.

## Where your snips live

`localStorage`, on the device that saved them. Deliberate: no accounts, no server, nothing to leak. The tradeoff is no sync and no backup.

To add sync, replace the `store` object in `index.html` with Supabase or Firebase calls — it's the only part of the app that touches persistence, so nothing else has to change.

## Design notes

The card inverts Substack's feed layout. In the app, the post title is the hero and everything else is supporting. Here the passage is the hero — set in a serif with an orange rule beside it — and the title, author, and cover drop to the footer as attribution. You already know why you saved it; you need to see *what* you saved.
