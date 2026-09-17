# AGENTS.md — Away-Assist

Instructions for any coding agent (Claude Code, Copilot, etc.) working in this
repository. Read `PRD.md` first — it is the source of truth for scope and
decisions. This file governs *how* to build, not *what* to build.

## Project Identity
- Name: Away-Assist
- Platform: Android only (native), min SDK 26
- Package: `com.awayassist.app` (placeholder — confirm before first commit)
- Type: single-module Android app, no backend, no network

## Hard Constraints (do not violate)
1. **No cross-platform framework.** Kotlin + native Android SDK only. Do not add
   Flutter, React Native, or KMM, even if asked to "make it easier" — this
   contradicts the PRD decision log.
2. **No networking code or permission.** Do not add `INTERNET` permission, Retrofit,
   OkHttp, or any HTTP client. This app is 100% on-device.
3. **No analytics/crash SDKs in v1** (no Firebase, no Sentry, etc.).
4. **No polling.** Any implementation of lock/unlock detection must be broadcast/event
   driven (`ACTION_SCREEN_OFF`, `ACTION_USER_PRESENT`). Do not implement with
   `WorkManager` periodic jobs, alarms, or timers that check state repeatedly.
5. **No ringer-mode change without checking
   `NotificationManager.isNotificationPolicyAccessGranted` first.** Every code path
   that calls `AudioManager.setRingerMode()` must guard on this.
6. **Foreground service notification must stay minimal**: `IMPORTANCE_LOW` or
   `IMPORTANCE_MIN`, `setShowWhen(false)`, `setSilent(true)`, no `BigTextStyle`, no
   images, max two inline actions. Do not "improve" this into a richer card.
7. **No Room/SQLite.** Use Jetpack DataStore (Preferences) only — the data model is
   a handful of key-value flags.
8. **No DI framework** (no Hilt/Koin). Manual constructor injection only — the class
   count does not justify a framework per the PRD.
9. **Keep it one Gradle module** (`app`). Do not split into `core`/`data`/`ui`
   modules.

## Tech Stack (must match)
- Kotlin, Jetpack Compose (Material3 used only as base scaffolding, themed over —
  do not ship default Material look)
- Font: Inter (bundle as variable font asset)
- Storage: Jetpack DataStore (Preferences)
- Background: Foreground service (`RingerService`) + dynamically registered
  `BroadcastReceiver` (`ScreenStateReceiver`) — receiver must be registered in code
  (`Context.registerReceiver`), not in the manifest, for `ACTION_SCREEN_OFF`.
- `ACTION_USER_PRESENT` may additionally be manifest-declared as a backup, but the
  service-registered receiver is the primary path.

## Design System (must match PRD Section 10 — "Calm Indigo")
Define as Compose theme tokens, do not hardcode colors inline in composables.

```kotlin
// Light
Background   = Color(0xFFFAFAFC)
CardSurface  = Color(0xFFF0F0F5)
TextPrimary  = Color(0xFF1C1C1E)
TextSecondary= Color(0xFF6E6E73)

// Dark
BackgroundDark   = Color(0xFF0B0B0F)
CardSurfaceDark  = Color(0xFF18181D)
TextPrimaryDark  = Color(0xFFF2F2F7)

// Shared / state colors (same in light & dark)
Accent      = Color(0xFF5E5CE6) // Silent state
RingState   = Color(0xFF30D158) // Ring state
```

- Shapes: continuous-corner ("squircle") style — implement a custom `Shape`, do not
  use default Material rounded-rect if it looks obviously circular-cornered.
- Components to hand-roll (do not pull in a third-party iOS-style UI kit library):
  - `AppleStyleSwitch` — rounded pill toggle
  - `AppleStyleButton` — press-opacity feedback, no ripple, no elevation
  - `GroupedListRow` — iOS Settings-style row with chevron, dividers between items
    only (not at top/bottom edges)
- Motion: use `spring()` animation specs, not default `tween()`, for state
  transitions (toggle, status card color change).
- No bottom navigation, no FAB. Single screen (`MainScreen`) for v1.

## File/Class Structure (expected)
```
app/
  src/main/java/com/awayassist/app/
    MainActivity.kt
    ui/
      MainScreen.kt
      theme/ (Color.kt, Type.kt, Shape.kt, Theme.kt)
      components/ (AppleStyleSwitch.kt, AppleStyleButton.kt, GroupedListRow.kt)
    service/
      RingerService.kt        // foreground service, owns the receiver + notification
      ScreenStateReceiver.kt  // BroadcastReceiver: ACTION_SCREEN_OFF / ACTION_USER_PRESENT
      NotificationHelper.kt   // builds/updates the low-importance notification
    data/
      AwayAssistPreferences.kt // DataStore wrapper: enabled flag, last mode, pause-until
    util/
      RingerModeController.kt  // wraps AudioManager + permission check
  AndroidManifest.xml
```

## Manifest Requirements
```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
Do not add `INTERNET`, `ACCESS_FINE_LOCATION`, `BLUETOOTH*`, or `READ_PHONE_STATE`
— none of these are needed for lock/unlock-only automation (v1 scope).

## Behavioral Acceptance Criteria (verify before considering a task done)
- [ ] Locking the screen sets ringer to Normal within ~1s, without app in foreground.
- [ ] Unlocking sets ringer to Vibrate within ~1s.
- [ ] No ringer change occurs if Notification Policy Access is not granted; instead
      the main screen clearly shows the permission is missing.
- [ ] Notification shows current state text and updates on every transition.
- [ ] Force Ring / Force Silent / Pause 1h from the notification work without
      opening the app, and correctly suspend automation until expiry.
- [ ] Master toggle in-app fully stops the service and receiver when turned off (no
      residual foreground service running).
- [ ] No network permission present in the final manifest.
- [ ] No polling loops anywhere in the codebase (grep for `Handler.postDelayed` /
      `WorkManager` periodic requests used as pseudo-polling and flag if found).

## Commit/PR Hygiene
- Keep commits scoped to one component at a time (service, UI, theme, etc.).
- Any deviation from PRD.md scope (e.g. adding a feature not listed) must be called
  out explicitly, not silently added.
- If a hard constraint above must be broken for a technical reason, stop and surface
  it rather than working around it silently.
