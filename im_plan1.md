# Multi-Issue Fixes and Improvements — Implementation Tracker

- [x] **Task 1: Fix Double App Icon**
    - [x] Verified `MainActivity` in `AndroidManifest.xml` does not have redundant `LAUNCHER` intent-filter. Only `activity-alias` has it.
- [x] **Task 2: Audio and Proximity Fixes**
    - [x] Updated `SipService.acquireWakeLock` to use both `PROXIMITY_SCREEN_OFF_WAKE_LOCK` and `PARTIAL_WAKE_LOCK`.
    - [x] Refined `restoreAudio` in `SipService` for safer state transitions.
    - [x] Verified `SipEngine.startRecording` logic (no explicit routing change detected, but ensured safer resource management).
- [x] **Task 3: Redial and Last Number Functionality**
    - [x] Added `lastDialedNumber` tracking in `SipViewModel`.
    - [x] Implemented redial logic in `DialpadScreen` (tapping call button with empty dialer brings back last number).
    - [x] Added `defaultDomain` global setting in `AccountRepository` and `SipViewModel`.
    - [x] Integrated `defaultDomain` as the default value in `AccountEditSheet`.
    - [x] Added "Default SIP Domain" setting in `SettingsScreen`.
- [x] **Task 4: Dark/Light Mode Fix**
    - [x] Replaced `darkModeEnabled` (boolean) with `themeMode` (string: "system", "light", "dark") in `AccountRepository` and `SipViewModel`.
    - [x] Updated `MainActivity` to correctly prioritize `themeMode` over `systemDark`.
    - [x] Updated `SettingsScreen` with improved Dark Mode toggle (System -> Dark -> Light).
