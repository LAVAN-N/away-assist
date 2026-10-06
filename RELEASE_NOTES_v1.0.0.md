# 🚀 Away-Assist v1.0.0 — Initial Release

> **Never miss a call when your phone is put away. Enjoy complete quiet when you're using it.**

**Away-Assist** is a lightweight, zero-permission, 100% on-device Android utility that intelligently manages your phone's ringer mode based on lock state — completely automated with zero battery drain.

---

### ✨ Key Features

* 🔒 **Smart Screen-Lock Automation**: Automatically switches your phone to **Ring (Audible)** the moment the screen locks, ensuring you never miss important calls while your phone is on a desk, in a bag, or charging.
* 🔓 **Instant Mute on Unlock**: Automatically switches back to **Vibrate / Silent** the second you unlock and use your device, preventing sudden loud ringtones during active usage.
* ⚡ **100% Event-Driven & Battery Friendly**: Zero polling or background wake-locks. State transitions are triggered instantly via native Android broadcast events (`ACTION_SCREEN_OFF` / `ACTION_USER_PRESENT`).
* 🛡️ **Complete Privacy (Zero Internet)**: Built with **no `INTERNET` permission** and no third-party analytics/trackers. All logic and preferences remain entirely on your device.
* 🎛️ **Liquid Quick-Controls**:
  * **Auto (Dynamic)**: Automated lock/unlock transitions.
  * **Always Ring**: Keep audible ringer active continuously without automated muting.
  * **Pause**: Temporarily suspend automation for 15m, 30m, 1h, or 2h with automatic resumption countdown.
* 📱 **Minimal Ongoing Notification**: Clean, non-intrusive status notification with quick 1-tap actions (*Ring*, *Silent*, *Pause 1h*) directly from your lock screen or notification shade.
* 🧩 **Home Screen Widget**: 1-tap mode switching and live countdown tracking right from your launcher.
* 🎨 **Calm Indigo Modern Design**: Handcrafted continuous-corner squircle layout with adaptive dark/light ambient theming and smooth spring animations.

---

### 📋 Technical Details

- **Min SDK**: Android 8.0 (Oreo / API 26)
- **Target SDK**: Android 15 (Vanilla Ice Cream / API 35)
- **Architecture**: Kotlin, Jetpack Compose, Jetpack DataStore (Preferences)
- **Permissions Required**:
  - `android.permission.ACCESS_NOTIFICATION_POLICY` (Do Not Disturb access for ringer mode switching)
  - `android.permission.FOREGROUND_SERVICE`
  - `android.permission.POST_NOTIFICATIONS`
- **Network Permission**: None (Zero network access, 100% offline)

---

### 📦 Store / In-App Short Changelog (under 500 chars)

```text
🎉 Welcome to Away-Assist v1.0.0!
• Smart Ringer Automation: Ring when locked, mute when unlocked.
• 100% On-Device & Private: Zero internet permission, no ads, no trackers.
• Low-Power Event Driven: Instant transitions with zero battery drain.
• Quick Controls & Pause: 1-tap override and customizable pause timers.
• Minimal Notification & Home Screen Widget.
```
