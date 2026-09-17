You are working in the Away-Assist repo. Before writing any code, read PRD.md and
AGENTS.md in the repo root fully — they are the source of truth for scope, tech
stack, design system, and hard constraints. Do not deviate from them. If any
instruction I give you below conflicts with those files, the files win — flag the
conflict instead of silently picking one.

Build this in stages, in order. After each stage, run `./gradlew assembleDebug`
and fix any compile errors before moving to the next stage. Do not write all files
in one shot.

Stage 1 — Project skeleton
- Set up build.gradle.kts (project + app level), settings.gradle.kts, and
  AndroidManifest.xml exactly per AGENTS.md (permissions, min/target SDK, package
  name com.awayassist.app).
- No app logic yet. Just confirm the empty project builds.

Stage 2 — Theme system
- Create ui/theme/Color.kt, Type.kt, Shape.kt, Theme.kt using the "Calm Indigo"
  tokens from PRD.md Section 10 / AGENTS.md design system section, light + dark.
- Bundle Inter as a variable font asset for typography.
- No screens yet, just the theme compiling with a placeholder preview.

Stage 3 — Core background logic (headless, no UI)
- Implement util/RingerModeController.kt (wraps AudioManager, checks
  isNotificationPolicyAccessGranted before any ringer mode change, logs clearly if
  permission is missing rather than failing silently).
- Implement service/ScreenStateReceiver.kt (ACTION_SCREEN_OFF, ACTION_USER_PRESENT)
  and service/RingerService.kt (foreground service, registers the receiver in code).
- Implement service/NotificationHelper.kt per the minimal notification spec in
  AGENTS.md (IMPORTANCE_LOW/MIN, setShowWhen(false), silent, state text, Force
  Ring/Force Silent/Pause 1h actions).
- Add Log.d() at every state transition so behavior can be verified via adb logcat
  before any UI exists.
- Verify by installing on a connected device/emulator and manually locking/
  unlocking while watching logcat — confirm ringer mode actually changes on a real
  device if one is available.

Stage 4 — Persistence
- Implement data/AwayAssistPreferences.kt using Jetpack DataStore (Preferences):
  master enabled flag, last known mode, last-changed timestamp, pause-until
  timestamp. No Room, no SQLite.

Stage 5 — Main screen UI
- Implement ui/MainScreen.kt and MainActivity.kt: permission onboarding flow
  (deep-link to Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, re-check on
  resume), master enable/disable toggle, live status card, permission status
  indicator.
- Implement the hand-rolled components: ui/components/AppleStyleSwitch.kt,
  AppleStyleButton.kt, GroupedListRow.kt — no third-party iOS UI kit libraries.
- Wire the screen to RingerService and AwayAssistPreferences.

Stage 6 — Notification actions
- Wire Force Ring / Force Silent / Pause 1h notification actions to actually pause/
  override automation per PRD.md Section 7.4, and reflect that state back in both
  the notification and the main screen.

After Stage 6, go through the Behavioral Acceptance Criteria checklist in
AGENTS.md one by one and report status on each — don't mark the build done until
every item is checked or explicitly called out as blocked with a reason.

Start with Stage 1 now.
