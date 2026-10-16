# 🚀 Away-Assist v1.1.1 — Patch Release

> **Choose Your Path UI Contrast Fix & Sideload Installation Guide.**

Away-Assist v1.1.1 is a fast patch following the v1.1.0 SOS Remote Locate & Anti-Theft release, resolving UI legibility and providing clear installation instructions for Android devices running Google Play Protect.

---

### ⚠️ Important Installation Notice: Google Play Protect Warning

Because Away-Assist contains privileged security capabilities (`SEND_SMS`, `RECEIVE_SMS`, device administration for anti-theft, and `WRITE_SECURE_SETTINGS`) and is installed directly outside the Google Play Store (sideloaded APK), **Google Play Protect may block installation** by default.

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

### 🛠️ Fixes & Improvements in v1.1.1

- 🎨 **Choose Your Path Modal Contrast Fix**:
  - Replaced the translucent glassmorphism background of the "Choose Your Path" bottom sheet with a 100% solid, opaque surface matching the active theme (`#141520` in Dark theme, `#F6EADB` in Light theme).
  - Eliminates text overlap and visual bleed-through from the underlying screen.
- 📖 **Documentation & Troubleshooting Updates**:
  - Added comprehensive Google Play Protect sideloading guidance to release notes and README.
  - Linked the complete security architecture whitepaper ([`SECURITY_AND_PERMISSIONS.md`](SECURITY_AND_PERMISSIONS.md)).
  - **📌 Shizuku Wireless Debugging Setup Requirements**:
    1. **Avoid Corporate / Enterprise Wi-Fi**: Do not use office, university, or public networks (they enforce client isolation and block local ADB port discovery). Use private home Wi-Fi or a mobile hotspot.
    2. **Set Device MAC Address**: In **Wi-Fi Settings ➔ [Your Wi-Fi Network] ➔ Privacy**, switch from *"Use randomized MAC"* to **"Use device MAC"** to prevent IP/handshake drops during pairing.

---

### 📋 Technical Specifications
- **Version**: `1.1.1` (Build `3`)
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **APK Footprint**: ~2.2 MB
- **Internet Permission**: None (Zero network access, 100% offline & on-device)

---

### 📦 Store / In-App Short Changelog (under 500 chars)

```text
🎉 Away-Assist v1.1.1:
• Fixed contrast & legibility on the SOS 'Choose Your Path' setup dialog with a solid opaque surface.
• Added prominent Google Play Protect installation guidance for sideloaded installs.
• All v1.1.0 SOS Remote Locate, Anti-Theft, 1-Tap Shizuku, and Always Ring features included.
• 100% on-device & private: Zero internet permission.
```
