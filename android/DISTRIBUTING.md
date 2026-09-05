# Sharing a build

Snips isn't on the Play Store, so friends install it from a `.apk` file. This
is the one-time setup that lets you publish one from a git tag, and what to
tell people once you have.

## 1. Make a signing key — once, and keep it forever

Android identifies an app by *who signed it*, not by its name. The same key
must sign every future version, or phones will refuse the update and treat it
as a different app.

```bash
keytool -genkeypair -v \
  -keystore snips-release.keystore \
  -alias snips \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

It asks for a password and some identity details; only the password matters.

**Back this file and its password up somewhere you won't lose them** — a
password manager is ideal. Lose them and you cannot ship an update to anyone
who installed the app: they'd have to uninstall, losing their snips, and
install a fresh one. There is no recovery path; this is Android's design, not
an oversight.

Keep it out of the repository. `.gitignore` already covers `*.keystore`,
`*.jks` and `keystore.properties`, but the habit matters more than the rule.

## 2. Tell GitHub about it

The keystore is a binary, so it travels as base64:

```bash
base64 -w0 snips-release.keystore > keystore.b64     # Linux
base64 -i snips-release.keystore | tr -d '\n' > keystore.b64   # macOS
certutil -encode snips-release.keystore keystore.b64  # Windows, then strip the header lines
```

In the repository: **Settings → Secrets and variables → Actions → New
repository secret**. Add four:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the contents of `keystore.b64` |
| `KEYSTORE_PASSWORD` | the password you chose |
| `KEY_ALIAS` | `snips` |
| `KEY_PASSWORD` | the same password, unless you set a separate key password |

Delete `keystore.b64` afterwards. The keystore itself stays on your machine.

## 3. Publish

```bash
git tag v0.2
git push origin v0.2
```

`.github/workflows/release.yml` runs the unit tests, builds a signed APK,
names it after the version and attaches it to a GitHub Release. The link on
that release page is what you send people.

`versionCode` comes from the run number, so it always climbs and Android
always sees an upgrade. `versionName` comes from the tag with the `v` removed
— `v0.2` becomes `0.2`.

You can also run the workflow by hand from the **Actions** tab for a build you
don't want to tag yet; it uploads the APK as a workflow artifact instead of
publishing a release.

## Building one locally instead

Put a `keystore.properties` next to `android/`:

```properties
storeFile=/absolute/path/to/snips-release.keystore
storePassword=…
keyAlias=snips
keyPassword=…
```

Then:

```bash
cd android
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

Without that file the release build still runs, but comes out unsigned and no
phone will install it.

## What to tell whoever you send it to

- It needs **Android 8.0 or newer**.
- Their phone will warn about installing from an unknown source. That warning
  appears for every app not from the Play Store; they allow it once, for
  whichever app opened the file — usually Chrome or their file manager.
- Capture works two ways: in Chrome, select text → **Share** → **Link to
  highlight** → **Save snip**. In the Substack app, select text → **Copy**,
  then the post's share menu → **Save snip**, and Snips pairs the two.
- **Read the post on its own page, not in a feed.** A passage shared from
  substack.com's feed produces a link to the feed rather than the post, and
  "read in context" then has nowhere to return to. The app says so when it
  happens.

## Before your own first release

The build on your phone right now is **debug-signed**, and a release APK is
signed with the key above — different signature, so Android will refuse to
install it over the top.

**Export your snips first** (⋮ → Export snips), then uninstall the debug
build, install the release, and import the file back. Do this once; every
release after it upgrades in place.
