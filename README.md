# IPDial — Android VoIP App

A clean, modern SIP softphone for Android built with:
- **PJSIP 2.13** — Opus/G.722/G.711 codecs, AEC, NS, AGC
- **Jetpack Compose + Material 3** — sage/forest-green theme matching your screenshots
- **Multi-account SIP** — register multiple SIP/VoIP accounts, switch with one tap
- **Minimal APK** — ABI splits (arm64 + armv7), R8 shrinking, no unused libs

---

## Project Structure

```
app/src/main/java/com/ipdial/
├── MainActivity.kt              # Nav + call overlay routing
├── IPDialApplication.kt        # App entry, auto-starts SipService
├── data/
│   ├── model/SipModels.kt       # SipAccount, CallSession, enums
│   └── repository/AccountRepository.kt  # DataStore persistence
├── service/
│   ├── SipEngine.kt             # PJSIP wrapper (accounts, calls, codecs)
│   ├── SipService.kt            # Foreground service, notifications
│   └── BootReceiver.kt          # Auto-restart after reboot
└── ui/
    ├── SipViewModel.kt          # State + actions
    ├── theme/                   # Material 3 colors, type, shapes
    └── screens/
        ├── HomeScreen.kt        # Account status cards
        ├── DialpadScreen.kt     # Keypad with account selector
        ├── CallScreen.kt        # Active / outgoing call
        ├── IncomingCallScreen.kt # Incoming call with Answer/Decline
        └── SettingsScreen.kt    # Account management + audio settings
```

---

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps

1. **Open** the project in Android Studio:
   ```
   File → Open → select /IPDial folder
   ```

2. **Sync Gradle** — it will download PJSIP AAR from JitPack automatically.

3. **Build APK** (minimal, split by ABI):
   ```
   Build → Generate Signed Bundle/APK → APK → release
   ```
   Or via terminal:
   ```bash
   ./gradlew assembleRelease
   ```

4. Install the `arm64-v8a` variant on your device:
   ```bash
   adb install app/build/outputs/apk/release/app-arm64-v8a-release.apk
   ```

### Expected APK sizes (after R8 shrinking + splits)
 ABI | Size |
-----|------|
 arm64-v8a | ~4.5 MB |
 armeabi-v7a | ~3.8 MB |

---

## Features

### Screens (matching your reference UI)
- **Home** — Account registration status with colored status dots
- **Keypad** — Dial pad with account chip selector, matches Google Dialer layout
- **Active Call** — Timer, mute/speaker/hold/keypad controls, end button
- **Incoming Call** — Full-screen with Decline / [phone icon] / Answer
- **Settings** — SIP account CRUD, audio processing toggles, network options

### Excluded (per your markup on screenshots)
- ~~Assisted dialing~~
- ~~Incoming call gesture~~
- ~~Quick responses~~
- ~~Voicemail~~

### SIP Settings included
- Username, password, domain, proxy, port
- Transport: UDP / TCP / TLS
- Preferred codec: Opus · G.722 · G.711u · G.711a
- Echo Cancellation (AEC) toggle
- Noise Suppression (NS) toggle
- Auto Gain Control (AGC) toggle
- STUN/ICE for NAT traversal
- SRTP for TLS accounts
- Multiple accounts with default selection

---

## Permissions
 Permission | Reason |
-----------|--------|
 RECORD_AUDIO | Microphone during calls |
 READ_PHONE_STATE | Interrupt calls on GSM activity |
 FOREGROUND_SERVICE | Keep SIP registered in background |
 POST_NOTIFICATIONS | Incoming call heads-up |
 RECEIVE_BOOT_COMPLETED | Reconnect after reboot |
 INTERNET | SIP signaling and media |

---

## Logo
Modern vector phone + signal waves, forest green (`#1E6B3C`) background.
Located at `res/drawable/ic_launcher_foreground.xml` + `ic_launcher_background.xml`.
