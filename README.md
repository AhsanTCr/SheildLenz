**An open-source Android parental control and digital wellbeing app powered by Local AI (`.tflite` or compatible models).** Detects and blocks explicit content in real time—even in local offline videos—while preventing unauthorized uninstallation.

👉 **[Download Latest APK from GitHub Releases](https://github.com/AhsanTCr/SheildLenz/Releases)**

> **🎁 Built-In AI Model Included:** No manual model setup is required. The app automatically downloads its built-in AI model during initial configuration, though you can still load a custom `.tflite` model manually at any time.

---

## ✨ Key Features

* **🤖 Real-Time Offline AI Detection:** Instantly scans and blocks inappropriate on-screen content—including locally saved videos—using on-device `.tflite` models with no internet connection required.
* **🧠 Smart False-Positive Protection (Built-In):** Accurately distinguishes between explicit content and safe visuals so natural skin tones, medical imagery, and harmless videos are not mistakenly blocked.
* **🔋 Ultra Battery Saving Mode:** Reduce power consumption by enabling **Ultra Saving Mode**, which restricts AI monitoring to high-risk ("red flag") apps while skipping games and everyday utilities.
* **🔒 Guardian PIN & Anti-Uninstall Protection:** Prevents unauthorized uninstallation or force-stopping without the Guardian Security PIN.
* **⚡ Reboot & Safe-Mode Persistence:** Restarting the device, removing the battery, or booting into **Safe Mode** will not reset active lock timers or disable protection.
* **⏳ Strict Temporary Device Lock:** Immediately shields the screen and locks the device whenever restricted content is detected.
* **👨‍👩‍👧 Guardian Control & Live Monitoring:** Link a Parent device to the Child App to monitor activity remotely, check live status, manage settings, and authorize uninstallation.
* **🔐 100% Local-First Privacy (Zero Cloud Leakage):** All AI inference runs strictly on-device. No screen captures or personal data are ever transmitted to external servers.

---

## 🔥 Advanced Capabilities Built Right In

* **👁️ Live See & Remote Lockdown (Parent ↔ Child Link):** Pair Parent and Child devices via QR code or a 6-digit pairing code to preview live screen thumbnails, inspect the active foreground app, view real-time threat logs, sync sensitivity settings remotely, and trigger instant **Lockdown / Unlock**. If you prefer strictly local management without remote linking, all controls can also be managed directly on the Child device.
* **🌐 Browser Safety Guard:** Inspects browser URLs and navigation (including incognito sessions) to block restricted websites before pages finish loading.
* **⚡ 4 Adaptive Power Modes & Eco Telemetry:** Choose between **Adaptive Eco**, **Ultra Power Saving**, **Balanced**, and **Extreme** modes featuring static-frame skipping, adaptive idle backoff, automatic throttling below 20% battery, compact frame scanning, and automatic pause when the screen is off.
* **🌙 Quiet Hours (Scheduled Guard):** Set automated schedules (including overnight windows) for stricter enforcement during bedtime or study hours.
* **✅ App Whitelist & Instant Eviction:** Exempt trusted apps from AI scanning while enabling instant eviction to close restricted apps and return directly to the Home screen.
* **🎛️ Custom Lockdown Penalties & Emergency Dialer:** Configure penalty durations (presets or exact minutes), optional **Double-Time Fallback Escalation**, **Cooldown Before PIN**, and an optional **Emergency Dialer** shortcut on the lock screen.
* **🧪 Hidden Advanced Precision Lab:** Tap the App Version card 7 times to unlock advanced controls—fine-tune Explicit and Suggestive confidence `%` thresholds, multi-frame confirmation counts, and precision timers.
* **📊 Audit Logs & False-Positive Review:** Review blocked apps, confidence scores, and event history locally; flag false positives; and export security audit reports—complete with a **Quick Settings Tile** and **SHA-256 verified in-app APK updates**.
* **✅ Configurable Warning Thresholds:** Define how many warnings are issued before closing an app and triggering a device lockout. Once locked out, access is restricted until the Guardian PIN is entered, recovery verification is completed via email/dialer, or double the timeout duration elapses.

---

> **Governance Policy:** All application settings can only be modified by the Guardian or parents. While a child cannot alter settings, they may regain device access after waiting double the configured timeout duration—with all timeout lengths and block thresholds controlled exclusively by the Guardian.

## 📱 Screenshots

| Home Console | Settings & AI Model | Content Blocked Alert |
| :---: | :---: | :---: |
| ![Home](https://github.com) | ![Settings](https://github.com) | ![Blocked](https://github.com) |

---

## ⚙️ Installation & Setup

1. Download the `.apk` file from **[GitHub Releases](https://github.com/AhsanTCr/SheildLenz/Releases)** (along with an optional custom `.tflite` model if you prefer not to use the built-in download).
2. Open the APK on your Android device and enable **Install from Unknown Sources** if prompted.
3. Launch **SheildLenz** and configure your **Guardian Security PIN** (be sure to store a backup safely).
4. Allow the app to fetch the built-in AI model (or import a custom `.tflite` file in Settings) and choose your preferred **Detection Sensitivity** (`Strict`, `Balanced`, or `Permissive`).
5. Grant **Device Administrator** and **Accessibility Service** permissions to enable anti-uninstall protection and real-time screen shielding.

---

## 🚀 What's New

* **📷 In-App Camera QR Scanner:** Instant one-tap QR scanning on the Parents screen for effortless Parent-Child pairing.
* **👨‍👩‍👧‍👦 Multi-Child Family Dashboard & App Time Budgets:** Manage multiple child devices from a single dashboard with daily per-app screen time limits and bedtime lock schedules.
* **🛡️ System-Wide Safe DNS & Custom Blocklists:** Zero-battery domain and SafeSearch DNS filtering paired with parent-managed custom blocked websites and keywords.
* **🔐 Guided Device Owner Setup:** Step-by-step Device Owner provisioning wizard for maximum tamper-resistant protection.
* **⚡ GPU / NPU AI Acceleration & Category Toggles:** Hardware-accelerated `.tflite` inference with independent toggles for adult content, suggestive visuals, gambling, and graphic media.
* **🌍 Multi-Language Support:** Full localization for families and guardians worldwide.
* **🔑 Forgotten PIN Recovery:** During initial Master Key setup, the app prompts for a Guardian Email. If the PIN is forgotten during a lockdown, the Guardian can contact AhsanTCr to receive a recovery password to enter via the Emergency Dialer during the timeout window.

---

## ⚠️ Important Notice & Emergency Unlock Support

* **Keep Your PIN Safe:** Please store your **Guardian Security PIN** securely. Because SheildLenz uses strict Device Administrator enforcement to prevent tampering, forgetting your PIN may cause temporary inconvenience (such as waiting for email recovery verification).
* **Locked Out by Mistake?** If your device was locked unintentionally, reach out at **[AhsanTCr.lovable.app](https://lovable.app)** or email **AhsanTCr@gmail.com** for recovery hints or a temporary unlock code to enter in the Emergency Dialer during the timeout period.

---

## 🤝 Built By

Created by **[AhsanTCr](https://github.com/AhsanTCr)**. Explore the repository, visit **[AhsanTCr.lovable.app](https://lovable.app)**, or open an Issue to share feedback!
