# IPDial — Implementation Plan

> All tasks have been successfully implemented and verified to compile cleanly.

---

## Task 1: Fix navbar empty space below status bar — ✅ DONE
**Files**: `ui/CommonComponents.kt`
**What**: The `IPDialTopBar` has `statusBarsPadding()` (line 88) which adds inset padding. But when used inside a `Scaffold(topBar=...)`, the Scaffold may already handle insets, causing double padding. Additionally, HomeScreen uses bare Column which may not consume status bar insets at all, causing inconsistent behavior.
**Fix**: Changed `Surface` container to extend fully behind the status bar and wrap inner content with status bar padding, eliminating the empty gap.

---

## Task 2: Fix auto-register/reconnect on network restore — ✅ DONE
**Files**: `ui/SipViewModel.kt`, `service/SipEngine.kt`
**Problem**: After network changes, addAccount early returns on unchanged config. Reconnection callbacks did not stabilize properly.
**Fix**: Updated `SipEngine` to trigger re-registration and force reconnection even if config is unchanged. Reconnect logic stabilized with network callback delay and fresh repository fetches.

---

## Task 3: Remove Missed/Dialed/Received filters from Home — ✅ DONE
**Files**: `ui/screens/HomeScreen.kt`
**What**: Change `filterLabels` from `["History", "Missed", "Dialed", "Received", "Contacts"]` to `["History", "Contacts"]`
**Fix**: Removed intermediate tabs. HomeScreen now only displays "History" and "Contacts".

---

## Task 4: Fix duplicate contact numbers (normalize) — ✅ DONE
**Files**: `data/repository/ContactsRepository.kt`
**What**: Normalize phone numbers before dedup comparison
**Fix**: Updated `ContactsRepository` to filter out non-digit characters during deduplication so format differences do not cause duplicate numbers.

---

## Task 5: Multi-number selection popup — ✅ DONE
**Files**: `ui/screens/ContactsScreen.kt`, `ui/screens/HomeScreen.kt`
**What**: When a contact has >1 number, show a dialog to pick which number to call
**Fix**: Introduced reusable `NumberPickerDialog` in `CommonComponents.kt`. When a contact has multiple numbers, tapping Call pops up the selection dialog.

---

## Task 6: Show call duration in history list — ✅ DONE
**Files**: `ui/screens/HomeScreen.kt`
**What**: In `CallLogRow`, show duration next to the time
**Fix**: Added call duration formatting and displays duration (e.g., `5m 12s`) next to time for non-missed call entries.

---

## Task 7: Tap history item → detailed call history (7 days) — ✅ DONE
**Files**: `ui/screens/HomeScreen.kt`
**What**: Tapping a history item should show all calls to/from that number in the last 7 days, with duration for each
**Fix**: Implemented `CallHistoryDetailDialog` which shows a mini list of all call records for the caller over the past 7 days, including call directions and duration.

---

## Task 8: Move update section from About to Settings — ✅ DONE
**Files**: `ui/screens/AboutScreen.kt`, `ui/screens/SettingsScreen.kt`
**What**: Remove update check UI from About, add it to Settings under Donation section
**Fix**: Cleared update checking logic and buttons from `AboutScreen.kt`. Added the update checker to `SettingsScreen.kt` inside an "Updates" section, with native Toast confirmations when the app is up to date.

---

## Task 9: Remove app name below logo in About — ✅ DONE
**Files**: `ui/screens/AboutScreen.kt`
**What**: Remove the "IPDial" text+Surface (lines 129-141) below the logo
**Fix**: Removed the redundant app name headings below the launcher icon.

---

## Task 10: Fix incoming call screen calling cards visibility — ✅ DONE
**Files**: `ui/screens/IncomingCallScreen.kt`
**What**: When `isFullScreenPhoto=true`, hide the circular avatar (redundant since photo is background). Improve slider visibility.
**Fix**: Hid the avatar and increased contrast on the slider track when a fullscreen blurred photo background is active.

---

## Task 11: Incoming call banner — ✅ DONE
**Files**: `MainActivity.kt`, new composable
**What**: Show compact banner at top of screen for incoming calls instead of immediately going full-screen. If user taps banner -> expand to full screen. If already full screen -> no banner.
**Fix**: Implemented `IncomingCallBanner` overlay card in `IPDialApp` enclosing layout. Tapping it opens full-screen call UI, with an auto-expansion fallback timer of 5 seconds (or immediately if calling cards is enabled).

---

## Execution Status
1. Task 1 (navbar) — ✅ COMPLETED
2. Task 2 (auto-reconnect) — ✅ COMPLETED
3. Task 3 (remove filters) — ✅ COMPLETED
4. Task 9 (remove app name) — ✅ COMPLETED
5. Task 6 (show duration) — ✅ COMPLETED
6. Task 4 (contact dedup) — ✅ COMPLETED
7. Task 5 (number picker) — ✅ COMPLETED
8. Task 8 (move update) — ✅ COMPLETED
9. Task 10 (calling cards fix) — ✅ COMPLETED
10. Task 7 (history detail) — ✅ COMPLETED
11. Task 11 (banner) — ✅ COMPLETED
