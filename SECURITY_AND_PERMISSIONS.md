# 🛡️ Away-Assist: Security Architecture & SOS Permissions FAQ

This document outlines the security model, permission isolation, and threat mitigations implemented in **Away-Assist**, specifically addressing how permissions (including privileged settings) are protected from misuse.

---

## 🔒 1. Sandboxing & Permission Isolation

### Can other apps misuse or "borrow" permissions granted to Away-Assist?
**No. Absolutely not.**

* **Android Linux UID Sandboxing**: Android assigns every installed application its own unique Linux User ID (UID). When a permission—including `WRITE_SECURE_SETTINGS`—is granted via ADB or Shizuku, the Android OS assigns it strictly to `com.awayassist.app`'s UID.
* **No Permission Sharing**: A permission granted to Away-Assist cannot be inherited, delegated, or hijacked by any other application installed on your device.
* **Closed Internal Architecture (`exported="false"`)**:
  All background services, controllers, and broadcast receivers handling hardware controls are declared as `android:exported="false"` in `AndroidManifest.xml`. No third-party app can send intents or call into Away-Assist to execute privileged actions.

---

## 🔐 2. Protection of `WRITE_SECURE_SETTINGS`

### What is it used for?
`WRITE_SECURE_SETTINGS` is a system-level permission used exclusively for emergency hardware recovery:
1. **Force-enabling Location Services (`location_mode = 3`)** when an emergency `FIND` or `TRACE` SMS is received with a valid PIN.
2. **Force-enabling Mobile Data (`mobile_data = 1`)** to ensure Google Play Services and GPS satellite ephemeris (A-GPS) can rapidly acquire location fixes.

### What is it NOT able to do?
* It does **not** grant root access.
* It does **not** allow running arbitrary shell commands.
* It does **not** expose terminal interfaces to other applications.

### Is this permission visible to other apps?
* Standard Android applications run in their own sandboxes and **cannot inspect** privileged permissions granted to other packages.
* In standard Android UI Settings (**Settings ➔ Apps ➔ Away Assist ➔ Permissions**), system-level permissions are not displayed alongside regular runtime permissions, keeping configuration clean and discreet.

---

## 🛡️ 3. SOS Remote Locate Threat Model & Defenses

| Potential Threat | How Away-Assist Defends Against It |
| :--- | :--- |
| **Remote Stalking / Unauthorized Tracking** | Location requests require your **secret PIN**. The PIN is never saved in plaintext; only its **salted SHA-256 hash** is verified on-device. |
| **SMS Brute-Force Attacks** | Unknown or invalid PIN commands fail silently without replying, preventing attackers from confirming whether the device has Away-Assist active. |
| **Data Leaks / Remote Cloud Breaches** | Away-Assist has **`ZERO INTERNET PERMISSIONS`** in its manifest. No location coordinates or telemetry are ever sent to servers, analytics platforms, or third parties. Location data is transmitted solely via direct SMS to your authorized sender. |
| **Lock Screen Snooping (Notification Previews)** | The in-app **Device Hardening Audit** prompts users to disable sensitive notification previews on the lock screen so thieves cannot see incoming verification codes. |
| **Thief Disabling Radios from Lock Screen** | The in-app **Control Centre Audit** guides users across OEM skins (MIUI/HyperOS, Samsung OneUI, ColorOS/OxygenOS, Pixel) to prevent pulling down the Quick Settings shade while locked. |
| **SIM Ejection / Swap** | Detects hardware SIM state transitions (`ACTION_SIM_CARD_STATE_CHANGED`) and automatically dispatches alert SMS messages to preconfigured emergency contacts. |
| **Sudden Shutdown / Power Cut** | Intercepts `ACTION_SHUTDOWN` to capture and dispatch the last-known GPS coordinates before power loss occurs. |

---

## 💡 Best Practices for Maximum User Security

1. **Set a Strong, Non-Obvious PIN**:
   * Do not use consecutive digits (`1234`), repeated digits (`0000`), birth years, or your phone's lock screen PIN.
2. **Customize Your Command Prefix**:
   * Change default command prefixes (e.g. from `FIND` to a personal keyword like `XLOC` or `RECOVER`) in **SOS Settings** to make unauthorized command guessing mathematically impossible.
3. **Turn Off Quick Settings on Lock Screen**:
   * Follow the built-in setup guide to ensure thieves cannot toggle Airplane Mode without unlocking your phone.
4. **Hide Lock Screen Notification Contents**:
   * Ensure incoming SMS codes are hidden behind biometric unlock.
