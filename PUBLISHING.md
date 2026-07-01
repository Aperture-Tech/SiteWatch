# Publishing SiteWatch to Google Play

Step-by-step, tailored to publishing under **ApertureTech LLC**. Assets and copy
live in `design/`; the privacy policy is `docs/index.html`.

---

## Phase 0 — Prerequisites (start early)

- [ ] **ApertureTech LLC** is a registered legal entity (name + address that
      match public records — Google verifies this).
- [ ] **D-U-N-S number** for ApertureTech LLC. Free from Dun & Bradstreet, but
      issuance can take **up to ~30 days**. Required for an Organization account.
- [ ] A Google account to own the Play Console (ideally a company account).

## Phase 1 — Create the Play Console account (one-time)

1. Go to <https://play.google.com/console> and sign in.
2. Pay the **one-time $25** registration fee.
3. Account type → **Organization**. Enter the ApertureTech LLC legal name,
   address, website, D-U-N-S number, and `sagar.jathan@aperture-tech.com`.
4. Complete identity/organization **verification** (can take a few days).
   > Organization accounts are exempt from the 14-day / 12-tester closed-test
   > requirement that applies to new personal accounts.

## Phase 2 — Build the signed release bundle (on your machine)

1. **Create the upload keystore** once (keep the `.jks` backed up somewhere safe
   — losing it is a problem):
   ```sh
   keytool -genkeypair -v -keystore sitewatch-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias sitewatch
   ```
2. Copy `keystore.properties.template` → `keystore.properties` and fill in the
   path/passwords/alias. (It's git-ignored.)
3. Confirm `versionCode` / `versionName` in `app/build.gradle.kts` (1 / 0.1.0 for
   the first release; bump `versionCode` for every later upload).
4. Build the App Bundle:
   ```sh
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`.
   > Or in Android Studio: **Build → Generate Signed Bundle / APK → Android App
   > Bundle**, pick the keystore, choose `release`.
5. ⚠️ **Smoke-test this exact release build** (R8/shrinking is on). Install it on
   a device — e.g. `bundletool build-apks --mode=universal` then install, or use
   the Internal testing track (Phase 6) — and confirm a check runs and notifies.

## Phase 3 — Create the app entry

1. Play Console → **All apps → Create app**.
2. App name **SiteWatch**, default language **English (US)**, type **App**,
   **Free**. Accept the declarations.

## Phase 4 — Store listing

Play Console → **Grow → Store presence → Main store listing**. Paste from
`design/play_store_listing.md`:

- [ ] App name, short description, full description
- [ ] **App icon** → `design/play_store_icon_512.png`
- [ ] **Feature graphic** → `design/play_feature_graphic_1024x500.png`
- [ ] **Phone screenshots** (min 2, recommend 4): run the app on a device/
      emulator and capture **Dashboard, Add site, Site detail, Notifications**.
- [ ] App category **Tools**, contact email, privacy policy URL

## Phase 5 — App content / policy declarations

Play Console → **Policy → App content**. Complete each:

- [ ] **Privacy policy** → your hosted `docs/index.html` URL
      (`https://<username>.github.io/<repo>/` via GitHub Pages)
- [ ] **Ads** → No ads
- [ ] **App access** → All functionality available without special access (no login)
- [ ] **Content ratings** → fill the questionnaire (expected Everyone / PEGI 3)
- [ ] **Target audience** → choose age groups (13+ is simplest; not child-directed)
- [ ] **Data safety** → **No data collected, no data shared** (no backend/analytics)
- [ ] Remaining toggles (government, financial, health) → No

## Phase 6 — Release (test first, then production)

1. **Internal testing first** (fast, no review wait): **Testing → Internal
   testing → Create release** → upload `app-release.aab` → add yourself as a
   tester → roll out → install via the opt-in link and verify on a real device.
2. When happy: **Production → Create release** → upload the AAB (or promote the
   internal one) → add release notes → select countries/regions → **Send for
   review**.
3. First production review typically takes a few days (sometimes longer).

## Phase 7 — Updates later

For every update: bump `versionCode` (and usually `versionName`) →
`./gradlew bundleRelease` → upload a new release → roll out.

---

### Quick asset reference

| Asset | Location / source |
|-------|-------------------|
| App icon 512×512 | `design/play_store_icon_512.png` |
| Feature graphic 1024×500 | `design/play_feature_graphic_1024x500.png` |
| Listing copy | `design/play_store_listing.md` |
| Privacy policy (host it) | `docs/index.html` |
| Signed bundle | `app/build/outputs/bundle/release/app-release.aab` |
