# Away Assist

<p align="center">
  <strong>Dynamic Lock & Unlock Ringer Automation for Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-blue?logo=android" alt="Android Version" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-green?logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Internet%20Permission-0%25%20(None)-brightgreen" alt="No Internet" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT" />
</p>

---

## 🎯 The Story & Problem

Like many people, you probably prefer keeping your phone on **silent vibration** when actively using it—loud ringtones are disruptive when you are already looking at your screen. 

However, the moment you put your phone down on a desk, slip it into a pocket or backpack, or walk into another room, leaving it on vibrate means **constantly missing important calls from family, friends, or work**.

**Away Assist** solves this problem automatically and deterministically:
- 🔒 **Screen Locked (Phone Put Away)** → Sets ringer mode to **Normal Audible Ring**.
- 🔓 **Screen Unlocked (Phone in Hand)** → Automatically sets ringer mode back to **Vibrate**.

No manual toggling. No complex geofences. No battery drain.

---

## ✨ Key Features

- **Deterministic Hardware Automation**: Driven purely by system broadcast events (`ACTION_SCREEN_OFF` and `ACTION_USER_PRESENT`).
- **Zero Polling & 0% Idle Battery Drain**: No background alarms, no timers, and no battery loops. The app sleeps when idle and executes in fractions of a millisecond during screen transitions.
- **100% On-Device Privacy**: Contains **zero internet permission**, no analytics, no crash reporters, and no remote servers. Everything runs entirely on your device.
- **Customizable Pause System**: Pause automation for `10m`, `15m`, `30m`, `1h`, or choose an exact duration with the digital clock dial picker.
- **Synced Home Screen Widget & Notification**: Control Auto mode, Force Ring, or trigger custom pause durations directly from your home screen widget or notification drawer.
- **Calm Indigo & Warm Vintage Theme**: High-end continuous squircle UI with interactive pull-cord vintage lamp toggle, gyroscope pendulum physics, and full-screen ambient lighting.
- **Ultra Lightweight**: Built with pure Kotlin & Jetpack Compose. Release APK size is only **~1.7 MB**.

---

## 📲 Download & Installation

### Option 1: Download from GitHub Releases
1. Go to the [Releases](https://github.com/LAVAN-N/away-assist/releases) page.
2. Download the latest `app-release.apk`.
3. Open the downloaded file on your Android device and tap **Install** (allow "Install unknown apps" for your browser/file manager if prompted).

### Option 2: Build from Source
```bash
git clone https://github.com/LAVAN-N/away-assist.git
cd away-assist
./gradlew assembleRelease
```
The compiled APK will be located at `app/build/outputs/apk/release/app-release.apk`.

---

## ⚙️ Initial Setup & Permissions

To ensure smooth and reliable background automation on all Android versions and OEM skins:

1. **Do Not Disturb Access (Notification Policy Access)**:
   - *Why it's required*: Android security policy mandates DND access for any application to programmatically modify system ringer modes between Audible and Vibrate.
   - *Setup*: Tap "Grant" inside the app or navigate to **Settings ➔ Apps & notifications ➔ Special app access ➔ Do Not Disturb access ➔ Away Assist (Allow)**.

2. **Battery Optimization (Exclude from Battery Saver)**:
   - *Why it's required*: Prevents aggressive OS power managers from delaying hardware screen broadcast listeners.
   - *Setup*: Tap "Set Unrestricted" in the app's settings section, or go to **App Info ➔ Battery ➔ Unrestricted**.

3. **Autostart & Recent Apps Pinning (Xiaomi / Samsung / Oppo / Vivo / OnePlus)**:
   - *Why it's required*: Protects the service from OEM task cleaners (*Cleaner*, *Device Care*, *Smart Manager*) when clearing recent apps.
   - *Setup*: Enable **Autostart** in your device security manager and lock Away Assist with the padlock icon in your Recent Apps overview.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material3 token base, hand-rolled continuous squircle design system)
- **State & Preferences**: Jetpack DataStore (Preferences)
- **Background Architecture**: Foreground Service (`RingerService`) + dynamic hardware `BroadcastReceiver` (`ScreenStateReceiver`)
- **Module Structure**: Single Gradle module (`app`)
- **Min SDK**: 26 (Android 8.0 Oreo) • **Target SDK**: 35 (Android 15)

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for more details.
