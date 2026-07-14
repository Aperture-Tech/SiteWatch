# SiteWatch

A self-contained Android app that monitors websites for changes and sends
**local** push notifications. No backend, no accounts, no FCM.

**Stack:** Jetpack Compose · MVVM · WorkManager · OkHttp · Jsoup · Room · Hilt

## Build phases

- [x] **Phase 1** — Room + WorkManager + full-page hash + notifications
- [x] **Phase 2** — Text & CSS-selector monitoring (Jsoup)
- [x] **Phase 3** — Visual / screenshot diff (WebView bitmap hash)
- [x] **Phase 4** — UI polish + manual check trigger *(this build)*

**All four phases complete.**

### Screens

`Dashboard` (site list) → tap a card → `Site Detail` → `Add/Edit`. The toolbar
bell opens `Notification History`.

- **Dashboard** — per-site cards with a monitor-type icon, active toggle,
  relative "checked N ago" status (or error/paused), and an inline **Check now**
  that fires a one-off check and confirms with a snackbar.
- **Site Detail** — full status (last checked/changed, current snapshot hash,
  selector/text, last error), active toggle, an observable **Check now** button
  that shows a live "Checking…" spinner driven off the WorkManager job state,
  this site's change history, and edit/delete (delete behind a confirm dialog).
- **Add/Edit** — validated form with monitor-type-specific fields.
- **Notification History** — all change notifications, newest first, clearable.
  Each entry describes *what* changed (see below), not just that something did.

### Describing changes

When a change is detected, `ChangeDescriber` turns the previous and current
content into a short summary used in both the notification and history:

| Type | Example message |
|------|-----------------|
| Full page | `Added: "New event on Sunday" / "…" (+3 more)` / `Removed: "…"` |
| CSS selector | `Changed from "₹250" to "₹199"` |
| Specific text | `The watched text appeared.` |
| Visual | `The page's appearance changed.` (no readable text from a screenshot) |

This required storing the previous human-readable content (`WatchedSite.lastContent`),
so full-page monitoring now hashes the page's **visible block text** instead of
raw HTML — which also makes it less noisy (ignores script/markup churn).

## What's in Phase 1

| Area | Files |
|------|-------|
| Room DB | `data/local/` — `WatchedSite`, `NotificationRecord`, DAOs, `SiteWatchDatabase` |
| Monitoring | `monitor/` — `PageFetcher` (OkHttp), `SiteMonitor`, `HashUtil` (SHA-256) |
| Background work | `work/` — `SiteCheckWorker` (HiltWorker), `WorkScheduler` (per-site UUID-tagged periodic jobs) |
| Notifications | `notification/NotificationHelper` |
| DI | `di/AppModule`, `SiteWatchApplication` (custom WorkManager config) |
| UI | `ui/dashboard` (site list), `ui/addsite` (add/edit), `ui/notifications` (history) |

### Monitor types

| Type | How it detects change |
|------|-----------------------|
| **Full page** | SHA-256 of the entire fetched HTML body |
| **Specific text** | Tracks whether a target string is present; fires when presence flips |
| **CSS selector** | SHA-256 of the text matched by a Jsoup selector (e.g. a price or status badge) |
| **Visual** | dHash of an offscreen WebView render of the top of the page |

Text/selector extraction runs through Jsoup in `SiteMonitor`. Misconfigurations
(invalid selector, selector matching nothing) are surfaced as **non-retryable**
failures so the worker records the error and waits for the next periodic run
instead of hammering the site with backoff retries.

#### Visual monitoring (`monitor/VisualCapture` + `monitor/PerceptualHash`)

- Renders the URL in an **offscreen** `WebView` on the main thread, forced to a
  **software layer** so `draw(Canvas)` captures pixels without a window.
- Captures a fixed 1080×1920 region (above the fold) after an `onPageFinished`
  settle delay, with a 30s overall timeout.
- Hashes it with a **difference hash (dHash)** rather than a raw pixel hash, so
  antialiasing and minor jitter don't cause false alarms while real layout
  changes still flip the hash. (A Hamming-distance threshold would make this
  even more tolerant — a natural future refinement; today comparison is exact.)
- Known limitation: offscreen software rendering can blank out some
  GPU-composited / `<canvas>`-heavy content. It works well for typical
  document-style pages.

## Running it

The repo intentionally does **not** include the binary `gradle/wrapper/gradle-wrapper.jar`.

**Easiest:** open the project folder in **Android Studio** (Koala or newer). It
will sync, generate the missing wrapper jar, and let you Run on an emulator or
device (API 26+).

**From the command line:** with a local Gradle 8.9+ installed, run once to
materialize the wrapper, then build:

```sh
gradle wrapper
./gradlew assembleDebug      # gradlew.bat on Windows
```

## App icon

The launcher icon is an **adaptive icon** (`mipmap-anydpi-v26/ic_launcher.xml`)
that uses an AI-generated raster (`mipmap-*/ic_launcher_image.png`) as a
full-bleed background layer with a transparent foreground, so Android's mask
renders the gradient artwork cleanly as a circle, squircle, or rounded square.
Because `minSdk` is 26, the adaptive icon covers every device (no legacy PNG
fallback needed).

`design/sitewatch_icon_vectorized.svg` is a high-resolution vector trace of the
same artwork, kept for use as a store-listing / web / splash asset (it is too
path-heavy to ship as an in-app `VectorDrawable`).

## Publishing to Google Play

The release build is configured for a signed **App Bundle** with R8 shrinking
(`app/build.gradle.kts`).

1. **Create a keystore** (once) and keep the `.jks` backed up:
   ```sh
   keytool -genkeypair -v -keystore sitewatch-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias sitewatch
   ```
2. **Provide the signing values**: copy `keystore.properties.template` to
   `keystore.properties` (git-ignored) and fill it in, or set the
   `SITEWATCH_STORE_FILE` / `SITEWATCH_STORE_PASSWORD` / `SITEWATCH_KEY_ALIAS` /
   `SITEWATCH_KEY_PASSWORD` environment variables. Without either, the release
   build stays unsigned (so a fresh clone still builds).
3. **Build the bundle**:
   ```sh
   ./gradlew bundleRelease        # -> app/build/outputs/bundle/release/app-release.aab
   ```
4. **Upload** the `.aab` to the Play Console and enable **Play App Signing**.

Listing assets:
- `design/play_store_icon_512.png` — 512×512 store icon.
- `design/play_store_listing.md` — app name, short/full description, and the
  other listing fields, ready to paste into the Play Console.
- Privacy policy: [`PRIVACY.md`](PRIVACY.md) (source) is also published as a
  ready-to-host page at [`docs/index.html`](docs/index.html).

**Hosting the privacy policy for free (GitHub Pages):** push this repo to GitHub,
then in the repo go to **Settings → Pages → Build and deployment → Source:
"Deploy from a branch"**, pick your default branch and the **`/docs`** folder,
and save. Your policy URL becomes `https://<username>.github.io/<repo>/` — paste
that into the Play Console listing + Data safety section.

Still needed in the Play Console: a 1024×500 feature graphic, phone screenshots,
descriptions, content-rating questionnaire, and the Data safety form (declare
"no data collected/shared" — the app has no backend or analytics).

**Publishing as ApertureTech LLC (organization account):**
- Choose an **Organization** account type in the Play Console. This requires a
  **D-U-N-S number** for the company (free from Dun & Bradstreet; can take up to
  ~30 days to issue, so start early). The developer name shown on the store will
  be "ApertureTech LLC".
- Organization accounts are **not** subject to the 14-day / 12-tester closed
  testing requirement that applies to new *personal* accounts — a practical
  advantage of publishing under the LLC.
- The contact email and privacy-policy URL above are reused across every app you
  publish under the company.

Bump `versionCode` (and `versionName`) in `app/build.gradle.kts` for each upload.

## Notes & limitations

- The interval field accepts any value from **1 minute** up, but WorkManager
  won't run *periodic* work more often than every **15 minutes**
  (`WorkScheduler.PERIODIC_FLOOR_MINUTES`), so a sub-15 setting still polls at
  ~15 min in the background. Use **Check now** for an instant check.
- On Android 13+ the app requests the `POST_NOTIFICATIONS` runtime permission on
  first launch.
- A site's first check only stores a baseline snapshot — notifications fire on
  the *next* check that differs.
- **Change confirmation (false-positive guard):** when a check first differs from
  the baseline, the worker waits `CONFIRM_DELAY_MS` (~4s) and re-reads. It only
  alerts if the second read is *stable* and still different. This filters sites
  that briefly show a loading/splash screen before their real content — the
  transient state won't trigger a false alarm, and it isn't stored as the new
  baseline. Visual monitoring also waits longer for the page to settle before
  capturing (`VisualCapture.SETTLE_DELAY_MS`).
- Persistence is not guaranteed across reinstalls (per spec); the Room DB uses
  `fallbackToDestructiveMigration()`.
