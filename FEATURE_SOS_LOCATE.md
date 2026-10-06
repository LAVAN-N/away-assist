# FEATURE_SOS_LOCATE.md — Away Assist: SOS Locate Addendum

This is an addendum to PRD.md, adding a second feature to Away Assist. It follows
the same philosophy as the core ringer feature: **stay off/silent by default, and
only activate briefly in a specific "something's wrong" moment.** Read PRD.md and
AGENTS.md first — this file does not repeat constraints that still apply (no
backend, no analytics, no DI framework, etc.) unless explicitly changed below.

## 1. Summary
SOS Locate adds a way to find the phone if it's lost or stolen, without keeping
location turned on at any other time. Location is enabled only when one of two
specific triggers fires, a single fix is captured, sent via SMS to a pre-configured
trusted number, and location is disabled again immediately after.

## 2. Problem Statement
The user keeps location permanently off by preference (privacy, battery), but this
means standard "find my phone" tooling can't help if the phone is lost or stolen,
since there's nothing to report with location disabled. The user wants the same
pattern already used for ringer mode — off by default, active only in the moment
it's actually needed — applied to location, triggered by specific signals that
indicate the phone may be lost/stolen rather than by continuous background
tracking.

## 3. Goals
- Location stays off during all normal use — no change to the user's baseline
  privacy/battery posture.
- Three independent triggers can request location action:
  1. SIM card removed → single fix
  2. Phone powered off (shutdown detected) → last-known location, best-effort
     (see 7.4a — true fix acquisition is not reliable in the shutdown window)
  3. SMS commands received from a pre-configured trusted number — three distinct
     commands with different behavior (see 7.4b):
     - `FIND` → single location fix, sent via SMS, then location off again
     - `TRACK` → turns location services on and leaves them on (no periodic SMS);
       lets the user pull location themselves via Google Maps/Find My Device;
       stays on until `STOP` or the auto-timeout
     - `TRACE` → periodic location fixes (every 5–10 min) sent via SMS
       automatically, until `STOP` or the auto-timeout
     - `STOP` → cancels `TRACK` or `TRACE` mode, returns location to disabled
- Passive triggers (SIM removed, shutdown) always behave like `FIND` — single
  fix only, never escalate to `TRACK`/`TRACE` on their own. Continuous location
  (`TRACK`/`TRACE`) is only ever entered by deliberate SMS command, never
  automatically — this is a deliberate safety boundary, see Section 11.
- No location history beyond what's needed for the active mode (single fix
  discarded after sending; `TRACE` keeps only the most recent fix, not a trail).
- No server/backend — SMS only, consistent with the rest of the app's no-network
  design.
- Auto-timeout safety net: `TRACK` and `TRACE` both auto-stop after a maximum
  duration (default 6 hours, configurable) even if `STOP` is never received —
  prevents indefinite battery drain and indefinite location exposure if the stop
  command can't be sent for any reason.

## 4. Non-Goals (v1)
- No "failed unlock attempts" trigger — requires Device Admin API, which is a
  heavyweight, alarming permission (full-screen system prompt, historically
  associated with MDM/malware-adjacent use) for a trigger that's less reliable
  than the other two. Explicitly deferred, not planned for a later version unless
  reconsidered.
- No *automatic* escalation into continuous tracking — `TRACK`/`TRACE` only ever
  start from an explicit SMS command, never from a passive trigger. This is a
  deliberate boundary, not an oversight (see Section 11).
- No location history/trail storage for `TRACE` — only the most recent fix is
  kept at any time, old fixes are not retained after the next one is sent.
- No map UI inside the app itself — the SMS response contains a Google Maps link;
  the user opens that on whatever device they're using to search.
- No cloud relay, no push notifications via a backend, no third-party tracking
  service integration.
- No support for multiple trusted numbers in v1 — a single configured number.

## 5. Target User
Same as core app: solo user, personal utility, values privacy (location off by
default) but wants a safety net if the phone goes missing.

## 6. Core User Stories
1. As a user, if my SIM card is removed (e.g. by a thief swapping it out), I want
   the phone to grab its location and text it to my other number, so I have a
   last-known location before the SIM is gone and it goes fully dark.
2. As a user, if my phone is powered off, I want it to attempt a best-effort
   location send in the brief shutdown window, so I have a last data point even
   if a full GPS fix isn't possible in time.
3. As a user, if I still have another way to reach the phone's number (e.g. from
   a friend's phone), I want to send `AWAYASSIST FIND` and have my phone text
   back a single location fix, so I can locate it without needing an app,
   account, or internet connection on either end.
4. As a user, if a single fix isn't enough — the phone is moving, or I want to
   actively watch it in Maps/Find My Device myself — I want to send
   `AWAYASSIST TRACK` to just turn location on and leave it on, so I can use my
   own tools to follow it.
5. As a user, if I want the phone to actively keep me updated without me having
   to check anything myself, I want to send `AWAYASSIST TRACE` to get a location
   text every 5-10 minutes, so I have a real trail while I try to recover it.
6. As a user, I want to send `AWAYASSIST STOP` to cancel `TRACK` or `TRACE` and
   return location to off, and I want it to stop on its own after a few hours
   even if I never send `STOP`, so this never silently drains the battery or
   exposes location indefinitely.
7. As a user, I want location to be off by default and only ever switch to
   continuous mode when I've explicitly asked for it via `TRACK`/`TRACE` — never
   automatically from SIM-removal or shutdown — so this feature doesn't
   compromise my normal privacy preference by default.
8. As a user, I want to configure the trusted number and the SMS command prefix
   once, in-app, so the feature works when I actually need it.
9. As a user, I want clear onboarding about the new permissions this feature
   needs, so I understand the trade-off before enabling it (this can be an
   optional feature, off by default, separate from the core ringer feature).
10. As a user, I want to be told plainly that Google Find My Device (or my
    phone's equivalent) already offers ring/lock/erase, and that SOS Locate's
    real job is just keeping location on for that to work — with a genuine
    no-account, no-cloud SMS-only alternative if I'd rather not involve Google
    at all — so I can make an informed choice rather than assuming Away Assist
    reimplements those features.

## 7. Functional Requirements

### 7.1 Purpose & Scope (rescoped)
SOS Locate's actual job is narrow and deliberate: **make sure location is on when
it needs to be**, so that existing, mature "find my phone" tooling (Google's Find
My Device, or equivalent OEM tools) can do the real work of locating, ringing,
locking, or erasing the device remotely. Away Assist does not reimplement ring/
lock/erase — that would duplicate a well-built, widely trusted tool for no benefit.

Two paths exist side by side, and the user can use either or both — they don't
conflict, since both simply depend on location being on:
- **Recommended path**: Google Find My Device (or OEM equivalent) — full featured
  (live map, ring, lock, erase), requires a Google account and Google's own data
  handling. SOS Locate's boot trigger (7.10) and SMS-triggered `TRACK` ensure
  location is on so this actually works when needed.
- **Standalone path**: Away Assist's own `FIND`/`TRACK`/`TRACE`/`STOP` SMS
  commands — no account, no cloud, no data collection, works over SMS only. For
  users who don't want Google involved at all, or as a backup alongside Find My
  Device.

- SOS Locate is **off by default** on fresh install — the core ringer feature
  works independently and does not require this.
- A dedicated section/screen in-app lets the user enable SOS Locate, which shows
  the "Choose Your Path" explanation (7.2a) followed by the permission onboarding
  flow (7.2) before it can be turned on.
- If the user disables SOS Locate later, all related receivers/services stop and
  no further triggers are monitored. This does not affect Find My Device, which
  is independent of our app.

### 7.2a Onboarding — "Choose Your Path"
Shown once, before permission requests, as the first screen of SOS Locate setup.
Plain, non-alarming explanation along these lines:

> "For full remote control — ring, lock, erase, live map — install or enable
> **Google Find My Device** (or your phone maker's equivalent). SOS Locate's job
> is just making sure location is switched on when it's needed, so those tools
> actually work.
>
> Prefer not to involve Google? SOS Locate also works entirely standalone, over
> SMS only — no account, no cloud, no data collection. Use `FIND`/`TRACK`/
> `TRACE`/`STOP` commands to your trusted number directly."

- Include a button/link to open Find My Device's setup (deep link to the Find My
  Device app if installed, or its Play Store / setup page if not — do not
  require installing it, this is informational, not blocking).
- Does not require the user to pick one — both can be enabled, nothing here
  disables or requires disabling the other.
- This screen is purely informational (no permissions requested yet) — proceeds
  to 7.2's permission onboarding regardless of which path(s) the user intends to
  use, since SOS Locate's own permissions are needed for the boot trigger and
  standalone SMS path either way.

### 7.2 Permission Onboarding (additive, separate from core app's DND permission)
On enabling SOS Locate for the first time, show a dedicated explanation screen
before requesting each permission, in this order:
1. **Trusted number setup** — user enters the phone number that will receive
   location texts and can send the SOS command. Stored locally only (DataStore).
2. **SMS command prefix setup** — user sets the exact command prefix (default
   suggestion: `AWAYASSIST`, user can customize). The four commands are this
   prefix + a fixed suffix: `<PREFIX> FIND`, `<PREFIX> TRACK`, `<PREFIX> TRACE`,
   `<PREFIX> STOP`. Explain that anyone who knows/guesses this prefix and has
   access to send SMS as the trusted number could trigger it — encourage a
   non-obvious phrase. Explain plainly what each of the four commands does
   before the user leaves this screen, since `TRACK`/`TRACE` behave very
   differently from `FIND` (continuous vs single fix).
3. Request `RECEIVE_SMS` and `SEND_SMS` — explain why (receive the command,
   send back the location).
4. Request `ACCESS_FINE_LOCATION` — explain it's used only momentarily, only on
   trigger, never continuously.
5. Request `READ_PHONE_STATE` — explain it's used only to detect SIM card
   removal.
- Each permission request has a clear one-line "why" shown before the system
  dialog, consistent with the calm, non-alarming tone of the rest of the app.
- If any permission is denied, that specific trigger is disabled but the rest of
  the app continues working normally (e.g. deny SMS permissions → SIM-removal
  trigger can still work if location + phone state are granted).

### 7.3 Trigger 1 — SIM Card Removed
- Listen for SIM state changes via `TelephonyManager`/`ACTION_SIM_STATE_CHANGED`
  (or the modern equivalent for target SDK).
- On detecting SIM removed/absent state (while the app's SOS Locate feature is
  enabled and this trigger specifically is on):
  - Acquire a single location fix (see 7.5).
  - Send it via SMS to the trusted number (see 7.6).
  - This always behaves as a `FIND` (single fix) — never escalates to
    `TRACK`/`TRACE` automatically.
- Known limitation (documented in-app and in README): this only fires while the
  device is powered on; a SIM pulled while powered off and not turned back on
  will not trigger this.

### 7.4a Trigger 2 — Phone Switched Off
- Listen for `Intent.ACTION_SHUTDOWN`, broadcast by the system on a normal
  power-off (while SOS Locate is enabled and this trigger specifically is on).
- On receiving it: immediately send the **last-known location**
  (`getLastLocation()`) via SMS if available — do not attempt to wait for a
  fresh GPS fix, since `ACTION_SHUTDOWN` gives only a few seconds before the
  process is killed and a fresh fix is not reliably achievable in that window.
- If no last-known location is available (e.g. location was never on before
  this moment), send a best-effort SMS noting the phone is powering off with no
  location available, rather than failing silently.
- Known limitation (documented in-app and in README): this only fires on a
  normal/requested shutdown. A hard power-off (battery pull, forced shutdown by
  holding the power button on some devices) does not reliably trigger
  `ACTION_SHUTDOWN` and will not trigger this.
- This always behaves as a best-effort single send — never escalates to
  `TRACK`/`TRACE` automatically.

### 7.4b Trigger 3 — Remote SMS Commands
- A `BroadcastReceiver` listens for incoming SMS (`SMS_RECEIVED`).
- On receiving a message, check sender number matches the configured trusted
  number exactly. If it doesn't match: ignore completely, do not log, do not
  notify — this must be silent and non-intrusive for all normal incoming SMS.
- If the sender matches, check the message body against the four exact command
  strings (case sensitive, exact match — no fuzzy matching, to avoid accidental
  triggers): `<PREFIX> FIND`, `<PREFIX> TRACK`, `<PREFIX> TRACE`, `<PREFIX> STOP`.
  Any other body from the trusted number is ignored the same as a non-matching
  sender (no reply, no log).
- **`FIND`**: acquire a single location fix (7.5) and send it via SMS reply
  (7.6). Location is disabled again immediately after. No change to any
  ongoing `TRACK`/`TRACE` session if one happens to be active — `FIND` is
  independent and does not cancel or interfere with an active session.
- **`TRACK`**: enable location services and leave them on. Do not poll or send
  periodic SMS. Send a single confirmation SMS ("Away Assist: Location turned
  on. Use Find My Device or Maps to view. Reply STOP to turn off, or it will
  auto-stop after <N> hours."). Start the auto-timeout timer (7.4c). If `TRACE`
  is already active, `TRACK` downgrades it to plain-on mode (stop the periodic
  SMS loop, keep location on, restart the auto-timeout timer).
- **`TRACE`**: enable location services and start sending a location fix via
  SMS every 5-10 minutes (exact interval configurable, default 7 minutes) until
  `STOP` or the auto-timeout fires. Send a single confirmation SMS on start,
  matching the `TRACK` confirmation style but noting the periodic interval and
  that `STOP` cancels it. If `TRACK` is already active (location on, no
  periodic sends), `TRACE` upgrades it to start the periodic SMS loop and
  restarts the auto-timeout timer.
- **`STOP`**: cancel any active `TRACK` or `TRACE` session — stop the periodic
  SMS loop if running, disable location services, cancel the auto-timeout
  timer. Send a single confirmation SMS ("Away Assist: Tracking stopped,
  location turned off."). If no session is active, still reply confirming
  location is off, so the sender gets a clear signal either way.
- This must not intercept/consume the SMS for any other purpose — do not use
  `abortBroadcast()` unless required to prevent the command text itself from
  cluttering the user's normal SMS inbox as a stray message (if implemented,
  document this behavior clearly in-app since suppressing an SMS is a notable
  side effect the user should know about).

### 7.4c Auto-Timeout Safety Net
- Both `TRACK` and `TRACE` sessions carry a maximum duration, default 6 hours,
  user-configurable in-app (reasonable bounds, e.g. 1-24 hours).
- On timeout expiry: behave exactly as `STOP` — stop any periodic SMS loop,
  disable location, send a confirmation SMS noting the session auto-stopped
  after reaching its time limit (distinguish this in the message text from a
  user-requested `STOP`, so the trusted number knows why it stopped).
- Timer resets whenever `TRACK` or `TRACE` is (re-)started or upgraded/
  downgraded into each other, not cumulative across separate sessions.

### 7.5 Location Acquisition
- **Single-fix mode** (`FIND`, SIM-removed, shutdown): use
  `FusedLocationProviderClient`, request a single high-accuracy fix, not
  continuous updates. Set a reasonable timeout (e.g. 30 seconds) — if no fix is
  acquired in time, fall back to last-known location (`getLastLocation()`) if
  available and recent (e.g. under 15 minutes old); otherwise send an SMS
  indicating a fix could not be obtained rather than failing silently.
  Immediately after sending the result, ensure no location listener/callback
  remains registered.
- **Continuous mode** (`TRACK`/`TRACE`): use `FusedLocationProviderClient` with
  a standing location request while the session is active. For `TRACK`, no
  periodic read-back is needed by the app itself — the OS-level location toggle
  being on is the whole point, so the user's own tools (Maps, Find My Device)
  can read it. For `TRACE`, the app itself reads the current location on each
  interval tick and sends it — implement the interval timer in a way that
  survives Doze as well as reasonably possible (e.g. via the same foreground
  service hosting this feature's receivers, not a bare `Handler.postDelayed`
  that Doze can suspend); document any reliability caveat here if perfect
  interval accuracy isn't achievable under Doze.
- All location listeners/requests must be fully torn down on `STOP` or
  auto-timeout — verify nothing is left registered afterward.

### 7.6 SMS Response
- `FIND`/SIM-removed/shutdown format, plain text, e.g.:
  ```
  Away Assist: Location fix at 14:32.
  https://maps.google.com/?q=<LAT>,<LONG>
  Accuracy: ~<ACCURACY>m
  ```
- `TRACE` periodic format: same structure, each send is independent (no
  trail/history references, just the current point each time).
- `TRACK`/`TRACE`/`STOP` confirmation messages per the wording examples in 7.4b.
- Sent via `SmsManager.sendTextMessage()` to the trusted number.
- No other data included — no device info, no additional metadata beyond
  coordinates, accuracy, and timestamp (and session-status text for the
  confirmation messages).

### 7.7 State & Persistence
- Store in DataStore (same mechanism as core app, no new storage system):
  - SOS Locate enabled/disabled (master + per-trigger toggles for SIM-removed,
    shutdown, and SMS-command independently)
  - Trusted number
  - SMS command prefix
  - Auto-timeout duration (default 6 hours)
  - Current session state: none / `TRACK` active / `TRACE` active, plus session
    start time (to compute auto-timeout) and, for `TRACE`, the configured
    interval
  - Timestamp of last trigger fired (for user visibility only, shown in-app)
- Do not store location history — only the single most recent fix value at any
  time, overwritten each time, discarded after sending; a `TRACE` session does
  not accumulate a trail, only the latest point exists in memory/storage at
  once.

### 7.8 UI Additions
- New section on the main screen or a secondary screen (per PRD's "single screen"
  preference, prefer a secondary screen reached via a settings row rather than
  cluttering the primary ringer status screen):
  - Master toggle: SOS Locate enabled/disabled
  - Trusted number field
  - SMS command prefix field, with the four resulting commands shown read-only
    beneath it for clarity (`<PREFIX> FIND`, `TRACK`, `TRACE`, `STOP`)
  - Per-trigger toggles: SIM removed / phone switched off / SMS commands
  - Auto-timeout duration setting (for `TRACK`/`TRACE`)
  - Live session status: none / `TRACK` active since <time> / `TRACE` active
    since <time>, interval <N> min — with a manual "Stop tracking now" button
    that mirrors the `STOP` SMS command, for convenience when the user has the
    phone in hand
  - Permission status indicators (reuse `GroupedListRow` component pattern from
    core app)
  - "Last triggered" timestamp, shown plainly, no alarming styling
- Follow existing Calm Indigo design system — no new color/typography system for
  this feature. Consider using the Ring-state green sparingly to indicate an
  active `TRACK`/`TRACE` session status, consistent with how the core app uses
  color to indicate state, not as a new alarming color.

### 7.9 Hardening Checklist (advisory only, not enforced)
Android gives no API for a third-party app to enforce any of the settings below
— all of this is check + advise + deep-link, never silent enforcement. Shown as
a checklist card on the SOS Locate settings screen, each item with a status
(good/needs attention) and, where possible, a button that deep-links to the
relevant system settings screen:
- **Lock screen set**: check `KeyguardManager.isDeviceSecure()`. If false, show
  a warning — "No lock screen set. Anyone can open Settings and disable SOS
  Locate or turn off location." Button deep-links to
  `Settings.ACTION_SECURITY_SETTINGS`.
- **Notification content hidden on lock screen**: advisory only (relevant more
  broadly than just this feature — applies to not tipping off a thief that
  anti-theft tooling is running at all). Button deep-links to
  `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (exact target screen is
  OEM-dependent; use the closest available intent).
- **Location Quick Settings toggle restricted on lock screen**: strongly worded
  advisory — "If your phone allows turning off location from the lock screen
  (Quick Settings/Control Center) without unlocking, a thief can disable this
  feature in seconds. Restrict this in your phone's lock screen settings." No
  deep-link target is consistent across OEMs, so provide guidance text rather
  than a button, and note this explicitly cannot be enforced by any app —
  worded as required setup, not optional, even though it can't be verified or
  enforced in code.
- **Power-off from lock screen**: static informational text, no action button —
  "Android does not allow any app to block the power button. If the phone is
  switched off, the boot trigger (below) is the fallback." This is documented
  plainly rather than presented as a gap in Away Assist, since no app (including
  Find My Device itself) can prevent this.

### 7.10 Boot Trigger
- Manifest-registered `BroadcastReceiver` for `ACTION_BOOT_COMPLETED` (requires
  `RECEIVE_BOOT_COMPLETED` permission), active only while SOS Locate is enabled.
- On boot:
  1. Turn location services on, unconditionally — this alone makes Find My
     Device (Bluetooth/network-based location) functional even with no SIM or
     no cellular signal present.
  2. Check for an active SIM with signal. If present, send a `FIND`-style SMS
     (single location fix, see 7.5/7.6) to the configured trusted number
     automatically — this is the moment most likely to catch a thief powering
     the phone back on to use or resell it.
  3. If no SIM or no signal is present at boot, skip the SMS step — location
     stays on regardless, so the Find My Device path still works once network/
     Bluetooth is available; retry the SMS send later if/when signal becomes
     available is out of scope for v1 (would need a periodic check, which
     conflicts with the no-polling constraint — note as a possible v2 idea, not
     built now).
- **Explicit limitation, documented in-app**: if the thief swaps in a different
  SIM, all of Away Assist's own SMS-based triggers (`FIND`/`TRACK`/`TRACE`/
  `STOP`, and this boot SMS) go dark, because they depend on outbound SMS from
  whatever SIM is currently in the phone reaching your trusted number — a new
  SIM has no relationship to your trusted number at all. **Location being turned
  on at boot still stands independently of this** — it's what makes Find My
  Device's Bluetooth/network-based location work even without your original SIM,
  which is why the boot trigger turns location on unconditionally rather than
  only when a familiar SIM is detected.
- Trusted number is read from the app's own DataStore (not written to or read
  from the SIM's contact storage) — it works identically regardless of which
  SIM is physically in the phone, since sending SMS only requires an active SIM
  with signal, not any particular SIM.

## 8. Non-Functional Requirements
- No continuous background service required for the *passive* triggers beyond
  what's needed to keep the `BroadcastReceiver`s registered (SIM state,
  shutdown, and SMS receiver can be manifest-registered where the OS allows,
  avoiding a second always-on foreground service if possible — investigate at
  implementation time whether a second foreground service is actually required
  or whether the existing `RingerService`'s foreground presence can also host
  these receivers to avoid a second persistent notification).
- An active `TRACE` session **does** need a reliable timer for its periodic
  interval — this is the one part of the feature that isn't purely
  event-driven, and should live inside a foreground service (reusing
  `RingerService`'s if possible) specifically while a session is active, not
  at all times.
- Battery impact: negligible when idle and for `FIND`/passive triggers
  (broadcast-driven, not polling; brief cost only at trigger time). Real,
  expected battery cost while `TRACK` or `TRACE` is actively running — this is
  a deliberate, user-initiated trade-off, not a background cost the user pays
  unknowingly. The auto-timeout (7.4c) exists specifically to bound this cost.
- Privacy: location data never leaves the device except via SMS to the trusted
  number the user configured themselves, or via the OS location toggle during
  `TRACK` (which the user's own tools read, not this app transmitting it
  elsewhere). No cloud, no analytics, no third party.
- This feature must remain fully optional — the core ringer feature must
  continue to work identically whether SOS Locate is enabled or not.

## 9. New Permissions Required
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
No new permission is required for the shutdown trigger (`ACTION_SHUTDOWN` needs
no special permission) or for `TRACK`/`TRACE`/`STOP` (they reuse the same
location/SMS permissions already listed). These are only requested if/when the
user enables SOS Locate — not requested at first launch alongside the core
app's DND permission.

## 10. Risks / Open Questions
- SMS-based command trigger means anyone who knows the trusted number AND the
  command prefix AND can spoof/send as that number could trigger a location
  disclosure — in practice SMS sender spoofing is non-trivial but not
  impossible on some carriers/regions; document this as a known limitation, not
  a guarantee. This risk is more consequential now that `TRACK`/`TRACE` exist,
  since a malicious trigger doesn't just leak one point but can start an
  extended tracking session — the auto-timeout (7.4c) bounds the damage even in
  that case.
- Some OEMs aggressively kill background SMS/telephony broadcast receivers
  despite correct implementation (same battery-optimization issue noted in the
  core PRD) — may need the same "exempt from battery optimization" prompt
  discussed for the core app, now doubly important since this feature is
  safety-critical, and even more so for keeping a `TRACE` interval timer alive.
- If `RECEIVE_SMS`/`SEND_SMS` permissions later trigger Play Store policy
  friction (SMS permissions are heavily restricted for Play Store distribution
  to apps that are default SMS handlers or explicitly declared "core" use cases)
  — not a blocker for GitHub Releases/sideloading, but worth knowing this feature
  may complicate a future Play Store submission if that's ever revisited.
- `ACTION_SHUTDOWN` is not guaranteed to fire in every shutdown path on every
  OEM (some skip it for fast/forced shutdowns) — treat the shutdown trigger as
  best-effort, not a guarantee, and say so clearly in-app.
- Decide at implementation time: does this feature need its own foreground
  service, or can it share the existing `RingerService`'s persistent presence to
  avoid a second notification, and should that shared/second service only
  actually run continuously while a `TRACK`/`TRACE` session is active (rather
  than for the lifetime of SOS Locate being enabled)? Investigate before
  building Stage C below.

## 11. Decision Log
- Failed-unlock-attempts trigger explicitly rejected for v1 — Device Admin API
  permission is disproportionately alarming/heavyweight relative to its
  reliability, and conflicts with the app's minimal-trust-footprint identity.
- SMS chosen over any server-based relay — keeps the "no backend, nothing leaves
  the device except to a number you control" property consistent with the core
  app's privacy stance.
- Passive triggers (SIM removed, shutdown) always stay single-fix, never
  continuous — preserves the user's stated default-off preference; continuous
  tracking is only ever entered deliberately, by explicit SMS command, never
  inferred automatically from a "something might be wrong" signal alone. This
  is the key boundary that keeps the feature an emergency exception mechanism
  rather than a general tracking capability sitting on the phone.
- `TRACK` (location on, no periodic SMS) and `TRACE` (location on + periodic
  SMS) were split into two separate commands, rather than one "continuous
  mode," because they serve different needs: `TRACK` suits a user who can check
  Find My Device/Maps themselves; `TRACE` suits a user who wants the phone to
  actively push updates without them checking anything. Splitting them avoids
  forcing periodic-SMS battery/data cost on someone who only wanted the OS
  toggle on.
- Auto-timeout (default 6h) added as a mandatory safety net on both `TRACK` and
  `TRACE` — without it, a lost `STOP` command (message lost, phone dead, etc.)
  would leave continuous location running indefinitely, which is both a battery
  risk and a privacy risk the core app's design philosophy (minimal footprint,
  nothing runs longer than it needs to) argues against.
- Shutdown trigger added as best-effort only (last-known location, not a fresh
  fix) — `ACTION_SHUTDOWN`'s few-second window makes a fresh GPS fix unreliable,
  so the honest implementation sends what's already known rather than promising
  a fix it likely can't deliver in time.
- **Scope narrowed to "keep location on," not "reimplement Find My Device."**
  Ring/lock/erase are deliberately not built — Google's Find My Device (and
  OEM equivalents) already do this well, and duplicating it would add
  complexity and permissions for no real benefit over what's already on most
  phones. SOS Locate's actual contribution is the one thing Find My Device
  can't do for a user who keeps location off by default: turn it on, on demand
  or automatically at boot. The standalone SMS `FIND`/`TRACK`/`TRACE`/`STOP`
  path is kept as a genuine no-account, no-cloud alternative for users who
  don't want to rely on Google at all, not as a primary feature we're pushing
  over Find My Device.
- **Power-button long-press restriction: investigated and rejected.** No public
  Android API allows any sideloaded or personal app to intercept or block the
  physical power button's long-press gesture; this is only possible under full
  Device Owner/MDM enrollment (provisioned at factory reset), which is far
  outside this app's scope and identity. Not revisited unless the app's entire
  distribution model changes to enterprise MDM, which is not planned.
- **Lock-screen/Quick-Settings restrictions on location and notification
  content: advisory only, by design, not a gap to keep chasing.** No API lets a
  third-party app enforce these OS-level security settings on the user's
  behalf. The hardening checklist (7.9) presents them as required setup via UX
  weight (warnings, deep-links) rather than pretending to enforce them in code.
- **Boot trigger turns location on unconditionally, regardless of which SIM (if
  any) is present**, specifically because Away Assist's own SMS relay depends
  on the current SIM having a relationship to the trusted number (it doesn't,
  if swapped), while Find My Device's Bluetooth/network-based location does not
  depend on the SIM at all — turning location on at boot is valuable even in
  the worst case where every SMS-based trigger is unusable.

---

# Implementation Plan for Coding Agent

Follow AGENTS.md conventions (file structure, no DI framework, no Room, DataStore
only, manual instantiation) for all new files below. Build in stages; run
`./gradlew assembleDebug` after each stage and fix errors before continuing, same
discipline as the original kickoff.

## Stage A — Manifest & permissions
- Add the four permissions from Section 9 to `AndroidManifest.xml` (note:
  `ACTION_SHUTDOWN` needs no separate permission).
- Add manifest-registered `BroadcastReceiver` entries for SIM state change,
  `ACTION_SHUTDOWN`, and `SMS_RECEIVED`, where the Android version allows
  manifest registration; note in a code comment any receiver that must instead
  be registered dynamically (mirror the reasoning already used for
  `ACTION_SCREEN_OFF` in the core app).

## Stage B — Data layer additions
- Extend `data/AwayAssistPreferences.kt` (do not create a second DataStore
  instance) with new keys: SOS Locate master enabled, per-trigger toggles (SIM
  removed / shutdown / SMS commands), trusted number, SMS command prefix,
  auto-timeout duration, current session state (none/TRACK/TRACE) with start
  time and TRACE interval, last-triggered timestamp.

## Stage C — Core SOS logic (headless, no UI)
- `service/SimStateReceiver.kt` — detects SIM removal, calls into the shared
  controller for a `FIND`-style single fix.
- `service/ShutdownReceiver.kt` — catches `ACTION_SHUTDOWN`, calls into the
  shared controller for the best-effort last-known-location send per 7.4a; keep
  this path as fast/minimal as possible given the shutdown time window.
- `service/SmsCommandReceiver.kt` — validates sender match, then parses the
  message body against the four exact command strings
  (`<PREFIX> FIND/TRACK/TRACE/STOP`) and dispatches to the appropriate
  controller method. Anything else from the trusted number, or any message
  from a non-trusted number, is dropped silently.
- `util/SosLocateController.kt` — shared logic, checks permissions before doing
  anything (mirror the `RingerModeController` pattern of never acting without
  confirming permission first). Implement distinct methods:
  - `handleFind()` — single fix per 7.5, format + send per 7.6, then ensure
    location/listeners are torn down.
  - `handleShutdown()` — last-known-location best-effort send per 7.4a.
  - `handleTrack()` — enable location, send confirmation SMS, start/refresh the
    auto-timeout timer (7.4c), persist session state; if a `TRACE` session is
    active, downgrade it (stop periodic loop, keep location on).
  - `handleTrace()` — enable location, start the periodic interval loop
    (default 7 min) inside the foreground service, send confirmation SMS,
    start/refresh the auto-timeout timer, persist session state; if `TRACK` is
    active, upgrade it (start periodic loop).
  - `handleStop()` — cancel periodic loop if running, disable location, cancel
    auto-timeout timer, clear session state, send confirmation SMS distinguishing
    user-requested stop vs (separately) auto-timeout expiry.
  - `handleAutoTimeout()` — same effect as `handleStop()`, triggered by the
    timer itself rather than an incoming SMS; confirmation SMS text should note
    it stopped due to the time limit, not a user command.
- Add `Log.d()` at every step (trigger received, permission check result,
  location fix acquired/timed out, SMS sent/failed, session state transitions)
  — same verification approach as the core app, testable via `adb logcat`
  before UI exists.
- Decide and implement the foreground-service question from Section 10's open
  question, including whether the service should only run continuously while a
  `TRACK`/`TRACE` session is active rather than for the lifetime of SOS Locate
  being enabled — document the decision in a code comment.
- Implement the `TRACE` interval timer in a way that survives Doze as
  reasonably as possible (foreground service context, not a bare
  `Handler.postDelayed`) — document any known interval-accuracy caveat under
  Doze in a code comment and in the README.

## Stage D — Onboarding: "Choose Your Path" + permissions
- New screen (e.g. `ui/SosLocateChoosePathScreen.kt`) implementing Section 7.2a
  — shown first, before any permission request. Explain Find My Device as the
  recommended full-featured path and the standalone SMS-only path as the
  no-account/no-cloud alternative. Include a button/link to Find My Device's
  setup (deep link to the app if installed, otherwise its Play Store page).
  Purely informational — no permission requests on this screen, and it does not
  block proceeding regardless of which path the user prefers.
- Then implement the sequential permission explanation flow from Section 7.2 —
  one permission explained and requested at a time, not a single bulk request.
  Include a clear explanation of all four commands (`FIND`/`TRACK`/`TRACE`/
  `STOP`) before the user leaves this screen, since `TRACK`/`TRACE` behave
  meaningfully differently from `FIND`.
- Reuse `AppleStyleButton`, `GroupedListRow`, and existing theme tokens — no new
  design system.

## Stage E — Settings UI
- New screen or section (e.g. `ui/SosLocateSettingsScreen.kt`) per Section 7.8:
  master toggle, trusted number field, command prefix field (with the four
  resulting commands shown read-only), per-trigger toggles (SIM removed /
  shutdown / SMS commands / boot), auto-timeout duration setting, live session
  status display with a manual "Stop tracking now" button, permission status,
  last-triggered display.
- Wire to `AwayAssistPreferences` and `SosLocateController`.
- Add navigation entry point from the main screen (a single settings row is
  enough — do not add a bottom nav bar or restructure the app's single-screen-
  first design).

## Stage G — Hardening checklist + boot trigger
- `ui/components/HardeningChecklistCard.kt` (or similar) implementing Section
  7.9: lock-screen-set check (`KeyguardManager.isDeviceSecure()`) with deep-link
  to `Settings.ACTION_SECURITY_SETTINGS`; notification-hiding advisory with
  deep-link to `Settings.ACTION_APP_NOTIFICATION_SETTINGS`; location
  Quick-Settings-on-lock-screen advisory (guidance text only, no deep-link
  target, since none is consistent across OEMs); static power-off informational
  text with no action button. Place this card on the SOS Locate settings screen
  (Stage E).
- `service/BootReceiver.kt` — manifest-registered receiver for
  `ACTION_BOOT_COMPLETED`, active only while SOS Locate is enabled. On boot:
  turn location on unconditionally, then check for SIM + signal and send a
  `FIND`-style SMS to the trusted number if available (reuse
  `SosLocateController.handleFind()` logic, do not duplicate it). Add
  `RECEIVE_BOOT_COMPLETED` to the manifest.
- Add `Log.d()` for boot trigger firing, location-on result, SIM/signal check
  result, and SMS send attempt — same verification approach as other stages.

## Stage F — Verification checklist
Confirm each before considering this feature done:
- [ ] With SOS Locate disabled (default), no new permissions are requested and no
      new receivers are active — core app behavior is byte-for-byte unchanged.
- [ ] The "Choose Your Path" screen appears before any permission request, and
      proceeding works identically regardless of which path the user reads/
      chooses — nothing is blocked by this screen.
- [ ] Enabling SOS Locate walks through permission onboarding one step at a time,
      not a bulk request, and clearly explains all four commands before finishing.
- [ ] Removing the SIM (test device) triggers a location fix + SMS within a
      reasonable time window, and never escalates into `TRACK`/`TRACE`.
- [ ] Powering off the device (normal shutdown) sends a best-effort
      last-known-location SMS, or a "no location available" SMS if none exists,
      and never escalates into `TRACK`/`TRACE`.
- [ ] Booting the device with SOS Locate enabled turns location on
      unconditionally, and sends a `FIND`-style SMS if a SIM with signal is
      present at boot.
- [ ] Booting the device with a different/no SIM inserted still turns location
      on (verify via system settings), and does not crash or hang attempting an
      SMS send with no usable SIM.
- [ ] Sending `<PREFIX> FIND` from the trusted number triggers a single location
      fix + SMS reply, then location returns to off.
- [ ] Sending `<PREFIX> TRACK` from the trusted number turns location on, sends
      one confirmation SMS, and does NOT send periodic location SMS.
- [ ] Sending `<PREFIX> TRACE` from the trusted number turns location on, sends
      one confirmation SMS, and DOES send a location SMS every ~5-10 minutes.
- [ ] Sending `<PREFIX> STOP` while `TRACK` or `TRACE` is active stops it
      immediately (periodic loop stops, location turns off, confirmation SMS
      sent).
- [ ] An active `TRACK`/`TRACE` session auto-stops on its own after the
      configured timeout (test with a short timeout, e.g. 2 minutes, to verify
      without waiting hours) and the confirmation SMS correctly distinguishes
      auto-timeout from a user-requested `STOP`.
- [ ] Sending `TRACK` while `TRACE` is active correctly downgrades it (loop
      stops, location stays on); sending `TRACE` while `TRACK` is active
      correctly upgrades it (loop starts). Both restart the auto-timeout timer.
- [ ] Sending any command from a different number, or any non-matching text
      from the trusted number, does nothing — verified silent, no notification,
      no visible log, no SMS sent.
- [ ] No location listener remains registered after a `FIND`, shutdown, boot
      send, or `STOP`/auto-timeout completes (verify via logcat / a location-
      permission-usage indicator in system UI).
- [ ] Denying SMS permissions still allows the SIM-removal and shutdown triggers
      to work if location + phone-state permissions are granted (and vice
      versa); denying location disables all SOS Locate location behavior
      cleanly with a clear in-app indicator, not a silent failure.
- [ ] The in-app "Stop tracking now" button produces identical behavior to
      sending the `STOP` SMS command.
- [ ] The hardening checklist correctly reflects real device state (test with
      lock screen off/on, and confirm each deep-link opens the right settings
      screen).
- [ ] Disabling SOS Locate fully stops all related receivers/services, including
      the boot receiver — no residual battery or permission activity.
- [ ] README.md updated to document this feature, its permissions, the boot
      trigger, the hardening checklist, and the known limitations from Section
      10 and the Decision Log (SIM-removed-while-off, SMS spoofing caveat,
      power-button/lock-screen enforcement not possible, SIM-swap defeats
      SMS-based triggers).
