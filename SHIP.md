# SHIP.md — Finalize Away Assist for GitHub Releases

Instructions for the coding agent (Antigravity CLI or similar) to take the app
from "builds and runs" to "shippable via GitHub Releases." Read PRD.md and
AGENTS.md first if not already loaded in context — this file assumes the app
from Stages 1-6 is functionally complete and behavioral acceptance criteria in
AGENTS.md have passed.

Do this in order. Stop and report back after each stage if something can't be
completed automatically (e.g. anything requiring my manual input like keystore
passwords or GitHub secrets).

---

## Stage 1 — Versioning setup

- In `app/build.gradle.kts`, confirm `versionCode` (integer, incremented each
  release) and `versionName` (semantic, e.g. `"1.0.0"`) are explicit fields, not
  left at defaults.
- Set initial release to `versionCode = 1`, `versionName = "1.0.0"`.
- Do not hardcode version numbers anywhere else in the app (UI "About" text, if
  any, should read from `BuildConfig.VERSION_NAME`).

## Stage 2 — Release signing setup

- Generate a release keystore if one does not already exist:
  ```
  keytool -genkey -v -keystore away-assist-release.keystore \
    -alias away-assist -keyalg RSA -keysize 2048 -validity 10000
  ```
  Do NOT run this with hardcoded passwords in a script — prompt me interactively
  for keystore password, key password, and identity fields (name/org can be
  minimal/personal since this isn't an org-published app).
- Add `signingConfigs` block in `app/build.gradle.kts` for a `release` build type,
  reading keystore path/passwords from a local, gitignored `keystore.properties`
  file (never hardcode credentials in `build.gradle.kts` itself):
  ```properties
  storeFile=../away-assist-release.keystore
  storePassword=***
  keyAlias=away-assist
  keyPassword=***
  ```
- Add both `away-assist-release.keystore` and `keystore.properties` to
  `.gitignore`. Verify neither is already tracked in git history — if either was
  committed at any point, flag this to me immediately (do not attempt to scrub
  git history yourself; that requires my explicit decision).
- Confirm `buildTypes { release { ... minifyEnabled ... proguardFiles ... } }` is
  configured. Enable R8/ProGuard minification and resource shrinking for release
  builds to keep APK size down (aligns with PRD's <5MB target):
  ```kotlin
  release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
  ```
- After enabling minification, rebuild and re-verify all Behavioral Acceptance
  Criteria from AGENTS.md still pass — R8 can break reflection-based code
  (unlikely here given the simple stack, but DataStore/Compose sometimes need
  keep-rules). Add any needed `proguard-rules.pro` keep rules if something breaks.

## Stage 3 — App identity polish

- Confirm final app icon (adaptive icon: foreground + background layers) is in
  place per the palette — not the default Android Studio placeholder
  icon. If no real icon exists yet, flag this to me rather than shipping with a
  placeholder.
- Confirm app label in `AndroidManifest.xml` / `strings.xml` reads "Away Assist",
  not a default project name.
- Double check final package name (`applicationId` in `build.gradle.kts`) matches
  what was decided — this cannot be changed after your first public release
  without breaking updates for anyone who installed it.

## Stage 4 — Build and verify the release artifact

- Run: `./gradlew assembleRelease`
- Confirm output at `app/build/outputs/apk/release/app-release.apk` is signed
  (verify with `jarsigner -verify -verbose -certs app-release.apk` or
  `apksigner verify app-release.apk`).
- Install the actual release APK (not a debug build) on a real device via
  `adb install app-release.apk` and re-run the full Behavioral Acceptance
  Criteria checklist from AGENTS.md against this exact artifact — release builds
  with minification enabled can behave differently from debug builds, so this is
  not redundant with earlier testing.
- Report final APK size — confirm it's under the 5MB PRD target; if not,
  investigate what's contributing (unused resources, unshrunk assets) before
  proceeding.

## Stage 5 — Repo hygiene for public release

- Confirm `.gitignore` covers: `*.keystore`, `keystore.properties`, `/build`,
  `.gradle`, `local.properties`, `.idea` (except shareable files if desired).
- Add a top-level `LICENSE` file — MIT unless I specify otherwise.
- Add a top-level `README.md` (separate from PRD.md/AGENTS.md) titled "Away
  Assist" with: what the app does (short version of the problem statement), a
  screenshot, install instructions (sideload steps: enable "install unknown
  apps," download APK from Releases, install), and a note that Notification
  Policy Access permission is required and why.
- Confirm no secrets, keystores, or personal identifiers are anywhere in tracked
  files before proceeding — do a final `git grep` pass for anything resembling a
  password or key.

## Stage 6 — GitHub Actions: automated release build

Create `.github/workflows/release.yml` that triggers on pushing a version tag
(e.g. `v1.0.0`) and:
1. Checks out the repo
2. Sets up JDK (17) and Android SDK
3. Decodes the release keystore from a GitHub Actions **encrypted secret**
   (`RELEASE_KEYSTORE_BASE64`) — do not attempt to commit the keystore to the
   repo, even privately; it must only exist as a GitHub secret + locally with me
4. Reads signing passwords from GitHub secrets (`KEYSTORE_PASSWORD`,
   `KEY_PASSWORD`, `KEY_ALIAS`)
5. Runs `./gradlew assembleRelease`
6. Attaches the resulting signed `app-release.apk` to a new GitHub Release
   matching the pushed tag, using the tag name as the release title and the
   corresponding `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   (if present) as release notes body — otherwise leave release notes blank for
   me to fill manually.

After creating this workflow, tell me exactly which GitHub repo secrets I need
to add manually (name each one) and how to base64-encode the keystore file for
the `RELEASE_KEYSTORE_BASE64` secret — do not attempt to add secrets yourself,
that requires my GitHub access.

## Stage 7 — First release checklist (report this back to me, do not self-execute)

Once Stages 1-6 are done, present me this checklist rather than acting on it —
tagging and pushing the first release is my call, not something to automate
unprompted:
- [ ] Keystore generated and backed up by me somewhere outside the repo (e.g.
      password manager, encrypted drive) — losing this means no future updates
      to this app under the same identity.
- [ ] GitHub repo secrets added (list them explicitly).
- [ ] `fastlane/metadata/android/en-US/` populated with real title, descriptions,
      icon, at least one screenshot (reusable for F-Droid later per our earlier
      discussion, optional for GitHub-only release now).
- [ ] README.md reviewed and accurate.
- [ ] Ready to tag: `git tag v1.0.0 && git push origin v1.0.0` — this triggers
      the release workflow automatically.

## Constraints carried over from AGENTS.md (do not violate here either)
- No network permission, no analytics SDKs, no DI framework, no Room — none of
  the finalization work above should introduce any of these.
- Do not weaken any hard constraint from AGENTS.md to make signing/minification
  "easier" (e.g. do not disable minification just because R8 threw an error —
  fix the ProGuard rule instead, or report the specific error to me).
