# 🚀 Away-Assist v1.1.0 — SOS Remote Locate & Anti-Theft Release

> **Emergency GPS Locate, Autonomous Anti-Theft Triggers, 1-Tap Shizuku Privileged Controls & Always Ring Mode.**

---

### ⚠️ Important Installation Notice: Google Play Protect Warning

Because Away-Assist contains privileged security capabilities (e.g. `SEND_SMS`, `RECEIVE_SMS`, device administration for anti-theft, and `WRITE_SECURE_SETTINGS`) and is installed directly outside the Google Play Store (sideloaded APK), **Google Play Protect may block installation** by default.

#### 🔧 How to Install:
* **Option A (Quick Bypass in Installer Dialog)**:
  When prompted with *"Blocked by Play Protect"*, tap **More details** ➔ **Install anyway**.
* **Option B (Temporarily Toggle Off in Play Store)**:
  If installation is completely blocked without an "Install anyway" button:
  1. Open the **Google Play Store** app.
  2. Tap your **Profile icon** (top right) ➔ **Play Protect**.
  3. Tap the **⚙️ Settings (Gear icon)** in the top right corner.
  4. Toggle **OFF** **"Scan apps with Play Protect"**.
  5. Install `app-release.apk`.
  6. *(Recommended)* You may toggle Play Protect back ON after installation.

---

Away-Assist v1.1.0 expands upon the core screen-lock ringer automation with a comprehensive, 100% on-device **SOS Remote Locate & Anti-Theft** suite designed to help you locate and secure your lost or misplaced device via SMS without relying on internet connectivity.

---

### 🚨 What's New in v1.1.0

#### 📍 1. Remote SMS Commands
- **Instant Location Fix (`FIND <PIN>`)**: Triggers high-accuracy GPS tracking with Google Play Services Fused Location + native framework fallback (25s GPS acquisition window, instant lock at $\le 15\text{m}$) and returns an immediate Google Maps coordinate link via SMS.
- **Continuous Route Tracking (`TRACE <PIN> <interval>`)**: Periodically updates designated emergency contacts with live coordinates at custom intervals.
- **Remote Sound Alarm (`RING <PIN>`)**: Forces device volume to maximum audible ring mode even if currently set to silent or Do Not Disturb.
- **Remote Mute (`MUTE <PIN>`)**: Remotely silences audible alarms when no longer needed.
- **Status Diagnostics (`STATUS <PIN>`)**: Inquires live battery level, GPS status, and current ringer mode.

#### 🛡️ 2. Autonomous Anti-Theft Triggers
- **SIM Card Swap Detection**: Automatically alerts emergency contacts if the SIM card is ejected or replaced, providing new carrier and IMSI details.
- **Power Loss / Shutdown Alert**: Dispatches last known coordinates immediately prior to device shutdown or battery depletion.
- **Device Reboot Alert**: Sends an automated status broadcast as soon as the system reboots.
- **Failed Unlock Intrusion Guard**: Optional protection against repeated failed PIN/pattern attempts on the lock screen.

#### ⚡ 3. 1-Tap Shizuku & Privileged Automation
- **Direct Privileged Permission**: Grant `WRITE_SECURE_SETTINGS` with a single tap on-device via Shizuku (no PC or terminal apps needed).
- **Automated GPS & Mobile Data Activation**: Automatically turns on Location Services and persistent Mobile Data when emergency commands are received.
- **Interactive ADB Guide**: Built-in instructions and direct settings shortcuts for users configuring via a computer.
- **📌 Note on Shizuku Wireless Debugging Setup**:
  For reliable pairing and connection over Wireless Debugging:
  1. **Network Environment**: Do **not** use corporate, university, office, or public Wi-Fi networks (they frequently enforce client isolation and block local ADB port discovery). Use your home private Wi-Fi or a temporary mobile hotspot.
  2. **Wi-Fi MAC Address Setting**: In **Wi-Fi Settings ➔ [Your Network] ➔ Privacy**, switch from *"Use randomized MAC"* to **"Use device MAC"**. Randomized MACs can cause the IP/pairing handshake to drop unexpectedly.

#### 🔔 4. Dedicated "Always Ring" Operational Mode
- Easily select **Ring (Always Ring)** in the operational mode selector to disable automated mute-on-unlock transitions while keeping SOS Remote Locate and Anti-Theft fully active.

#### 🎨 5. UI/UX Enhancements & Security Audits
- Vibrant dynamic status colors for **Ring** (Green) and **Pause** (Amber).
- Full Android system back-gesture navigation support across all sheets and dialogs.
- Lock screen notification content audit & Control Centre pull-down security checklists across OEM skins (MIUI/HyperOS, OneUI, ColorOS/OxygenOS, Stock Android).

---

### 📋 Technical Specifications
- **Version**: `1.1.0` (Build `2`)
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **APK Footprint**: ~2.2 MB
- **Internet Permission**: None (Zero network access, 100% offline & on-device)

---

### 📦 Store / In-App Short Changelog (under 500 chars)

```text
🎉 Away-Assist v1.1.0: SOS Remote Locate & Anti-Theft!
• Remote SMS Commands: Locate your phone with FIND <PIN> or trigger continuous TRACE.
• Autonomous Anti-Theft: Emergency SMS alerts on SIM swap, power off, or reboot.
• 1-Tap Shizuku: Effortless privileged setup to auto-enable GPS & Mobile Data during emergencies.
• Dedicated Always Ring Mode: Continuous audible ring while keeping SOS active.
• 100% on-device & private: Zero internet permission.
```
