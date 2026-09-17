# PRD — Away-Assist

## 1. Summary
Away-Assist is a lightweight, native Android utility app that automatically switches
the phone's ringer mode based on lock state:

- **Screen locked** → Ringer mode set to **Ring** (Normal)
- **Screen unlocked** → Ringer mode set to **Vibrate**

Goal: never miss a call while the phone is put away/locked, while keeping the phone
silent/vibrate during active use — without any manual toggling.

## 2. Problem Statement
The user keeps their phone permanently in vibrate mode by preference (to avoid
disruption while actively using the device), but this causes missed calls when the
phone is out of hand/pocket/bag and locked — e.g. left on a desk, in a bag, or during
driving. The user wants the ringer to automatically become audible only when the
phone is not in active use (locked), and return to vibrate the moment it's unlocked.

## 3. Goals
- Automatically flip ringer mode on lock/unlock with no manual action needed.
- Be reliable: never miss the lock/unlock transition.
- Be lightweight: minimal APK size, no unnecessary background cost, no polling.
- Respect the user's attention: no intrusive notification, no popups, no sound on
  its own state changes.
- Give the user a fast manual override for edge cases (e.g. expecting a call while
  unlocked, or wanting quiet while locked temporarily).

## 4. Non-Goals (v1)
- No iOS version (platform does not expose the required APIs — see decision log).
- No driving/Bluetooth-based detection — lock/unlock is the only trigger.
- No call-log tracking, no analytics, no cloud sync, no accounts.
- No support for multiple custom "modes" beyond Ring ↔ Vibrate.
- No smartwatch / cross-device companion.

## 5. Target User
Solo user (personal utility), Android phone kept in vibrate mode by default,
wants automatic ring restoration only when the phone is not being actively handled.

## 6. Core User Stories
1. As a user, when I lock my phone, I want it to switch to Ring mode automatically,
   so I don't miss calls while it's put away.
2. As a user, when I unlock my phone, I want it to switch back to Vibrate
   automatically, so it doesn't disrupt me while I'm using it.
3. As a user, I want to see the app's current state (Ring/Silent) at a glance from
   the notification, without an intrusive card.
4. As a user, I want a one-tap override (Force Ring / Force Silent / Pause 1h) for
   exceptions, without having to open the app.
5. As a user, I want to grant the one required permission (Notification Policy
   Access) via a simple guided step on first launch.
6. As a user, I want to enable/disable the whole feature with a single toggle in
   the app.

## 7. Functional Requirements

### 7.1 Permission Onboarding
- On first launch, check `NotificationManager.isNotificationPolicyAccessGranted`.
- If not granted, show a single clear screen explaining why it's needed, with a
  button that deep-links to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`.
- Re-check on `onResume` after returning from Settings.
- App must not attempt any ringer-mode change without this permission.

### 7.2 Core Automation
- On screen lock (`ACTION_SCREEN_OFF`) → set ringer mode to `RINGER_MODE_NORMAL`.
- On unlock (`ACTION_USER_PRESENT`) → set ringer mode to `RINGER_MODE_VIBRATE`.
- Must be handled by a foreground service with a dynamically registered
  `BroadcastReceiver` (manifest-declared receivers cannot catch these on modern
  Android).
- State changes must be event-driven only — no polling, no periodic checks.

### 7.3 Notification (foreground service requirement)
- Persistent, low-importance (`IMPORTANCE_LOW`/`MIN`) notification, silent, no
  heads-up, no timestamp (`setShowWhen(false)`).
- Content line reflects live state, e.g.:
  `🔔 Ring mode — screen locked` or `🔕 Silent — screen unlocked`
- Two inline actions: **Force Ring** / **Force Silent**, and **Pause 1h**.
- No expandable big-text body, no images.

### 7.4 Manual Override
- "Pause" temporarily disables automation for a chosen duration (default 1h);
  automation resumes automatically after the timer, or immediately if the user
  re-enables manually.
- "Force Ring" / "Force Silent" from the notification immediately sets ringer mode
  and marks automation as paused until next natural lock/unlock event or timer
  expiry (avoids fighting the user's explicit choice).

### 7.5 Main Screen (single screen app)
- Master enable/disable toggle for the whole feature.
- Current status card: current ringer mode + reason (locked/unlocked/paused/override).
- Permission status indicator + re-grant entry point if revoked later.
- Settings row: Ring mode on lock (Normal — fixed in v1), Vibrate on unlock (fixed
  in v1) — shown as read-only info for now, editable in a future version.

### 7.6 State Persistence
- Persist: master enabled/disabled flag, last known mode, last-changed timestamp,
  pause-until timestamp — via Jetpack DataStore (Preferences).
- No database, no relational storage needed.

## 8. Non-Functional Requirements
- **APK size target:** under 5MB.
- **Battery:** negligible — no location, no polling, pure broadcast-driven.
- **Reliability:** must survive Doze/App Standby; foreground service ensures this.
- **Privacy:** no data leaves the device. No network permission requested.
- **Min SDK:** API 26 (Android 8.0) — required for notification channels and
  aligns with the implicit-broadcast restrictions this app is built around.
- **Target/Compile SDK:** latest stable at build time.

## 9. Tech Stack (decided)
- Kotlin, native Android (no cross-platform framework).
- UI: Jetpack Compose, Material3 as base scaffolding only — heavily themed to a
  custom Apple-inspired look (see Design section).
- Background: Foreground Service + dynamically registered BroadcastReceiver.
- Storage: Jetpack DataStore (Preferences).
- DI: none — manual instantiation (app is ~4-5 classes).
- No networking, no image loading, no analytics/crash SDK in v1.

## 10. Design Direction — "Calm Indigo" (Apple-inspired)
- Palette:
  - Background: `#FAFAFC` (light) / `#0B0B0F` (dark)
  - Card surface: `#F0F0F5` (light) / `#18181D` (dark)
  - Accent (Silent state): `#5E5CE6`
  - Ring state: `#30D158`
  - Text primary: `#1C1C1E` (light) / `#F2F2F7` (dark)
  - Text secondary: `#6E6E73`
- Typography: Inter (bundled variable font), Apple-like type scale (large title,
  confident hierarchy, generous line height).
- Components: custom rounded-pill switch (not Material switch), continuous-corner
  ("squircle") card shapes, no ripple/elevation — press-opacity feedback instead,
  grouped-list settings rows in iOS Settings style.
- Motion: `spring()`-based transitions to approximate iOS spring easing.
- No bottom nav / FAB — single screen in v1.

## 11. Permissions Required
```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 12. Success Metrics (personal-use app, informal)
- Zero missed calls while phone is locked, over a week of normal use.
- No noticeable battery delta vs. baseline (checked via Android battery stats).
- No accidental "stuck in wrong mode" states after a week of use.

## 13. Risks / Open Questions
- OEM battery optimization (Xiaomi/Samsung/etc.) may still kill the foreground
  service despite best practices — may need to prompt user to exempt the app from
  battery optimization as a secondary onboarding step.
- If Notification Policy Access is revoked later (manually, by user), automation
  silently stops — app must detect this on resume and surface it clearly, not fail
  silently forever.
- Future: should locked-but-charging-on-nightstand be excluded from "Ring" to avoid
  night-time ringing? Out of scope for v1, noted for v2 discussion.

## 14. Decision Log
- iOS excluded: platform provides no API to read background lock state or to
  programmatically change ringer mode (Apple restricts this to the physical
  switch/Focus Modes), so true feature parity is impossible.
- Driving/Bluetooth trigger excluded from v1 by explicit user decision — lock/unlock
  only, to keep scope and battery cost minimal.
- Cross-platform framework (Flutter/RN/KMM) rejected — entire feature surface is
  platform API calls with a single screen of UI; a bridge layer adds size/complexity
  with no benefit here.
