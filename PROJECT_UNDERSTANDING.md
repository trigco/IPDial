# IPDial — Codebase Understanding Report

> Generated: 2026-06-12. Reference this file to reduce token usage in future sessions.

## Project Overview
Android VoIP/SIP calling app built with Kotlin, Jetpack Compose (Material3), PJSIP library.

## Architecture
- **UI Layer**: Jetpack Compose, single `MainActivity`, `NavHost` for routing
- **State**: `SipViewModel` (AndroidViewModel) holds all reactive state via `StateFlow`
- **SIP Engine**: `SipEngine` singleton wraps PJSIP (pjsua2) — account management, call control
- **Service**: `SipService` (foreground service) — manages SIP lifecycle, call logging, ringtone, audio routing
- **Data**: `AccountRepository` (DataStore-backed), `CallLogRepository` (SharedPreferences JSON), `ContactsRepository` (ContentResolver)
- **Models**: `SipModels.kt` — `SipAccount`, `CallSession`, `CallLogEntry`, `CallState`, `RegStatus`, enums

## File Map (app/src/main/java/com/ipdial/)

### Core
| File | Purpose | Key Functions |
|------|---------|---------------|
| `MainActivity.kt` | Activity, NavHost, drawer, bottom bar, call screen routing | `IPDialApp()` composable, permission handling |
| `IPDialApplication.kt` | App class | — |

### Service Layer
| File | Purpose | Key Details |
|------|---------|-------------|
| `service/SipEngine.kt` | PJSIP wrapper singleton | `init()`, `addAccount()`, `removeAccount()`, `reconnectAccount()`, `makeCall()`, `answerCall()`, `hangupCall()`, codec config. Inner classes: `PjAccount` (handles `onRegState`, `onIncomingCall`), `PjCall` (handles `onCallState`, `onCallMediaState`) |
| `service/SipService.kt` | Foreground service | Starts PJSIP, registers accounts from DataStore, observes call state, handles ringtone/audio, notifications. Actions: START, STOP, ANSWER, DECLINE, HANGUP, TEST_CALL |
| `service/SipConnectionService.kt` | Telecom ConnectionService | For system call integration |
| `service/TelecomHelper.kt` | Telecom registration | Phone account, incoming/outgoing call helpers |
| `service/BootReceiver.kt` | Boot receiver | Auto-start service on boot |

### UI Layer
| File | Purpose | Key Details |
|------|---------|-------------|
| `ui/SipViewModel.kt` | Main ViewModel | Network callback (lines 101-129), account management, call control, contacts, call log, dialer state. `NetworkCallback.onAvailable` → `addAccount` + `reconnectAccount`. `onLost` → set ERROR status |
| `ui/CommonComponents.kt` | Shared UI | `IPDialTopBar` (status dot + app name + hamburger), `RegStatusIndicator` |
| `ui/screens/HomeScreen.kt` | Home tab | Filter chips (History/Missed/Dialed/Received/Contacts), search, call log list, contact list. Uses bare `Column` (no Scaffold). `CallLogRow` shows name + direction icon + via/time. `ContactItem` shows name + numbers + call button |
| `ui/screens/ContactsScreen.kt` | Contacts tab | Alphabetical list with `AlphabetIndexer`, search. `ContactItem` reused. Calls first number only |
| `ui/screens/DialpadScreen.kt` | Keypad tab | Number input + dial buttons |
| `ui/screens/CallScreen.kt` | Active call UI | Timer, mute/speaker/hold/record/DTMF controls |
| `ui/screens/IncomingCallScreen.kt` | Incoming call | Swipe slider (answer/decline), calling cards (full-screen photo), pulsing icon animation |
| `ui/screens/AccountsScreen.kt` | Account management | Add/edit/delete SIP accounts |
| `ui/screens/SettingsScreen.kt` | Settings | Sections: Donation, Audio (ringtone), General (calling cards, dark mode, DND), System (activity log) |
| `ui/screens/AboutScreen.kt` | About | Logo, app name, version, update check button, developer info. **Update check is here + also in MainActivity auto-check** |
| `ui/screens/RecordingsScreen.kt` | Recordings | List recorded calls |
| `ui/screens/ActivityLogScreen.kt` | Activity log | SIP debug logs |
| `ui/screens/AlphabetIndexer.kt` | Alphabet sidebar | A-Z quick scroll |
| `ui/screens/Utils.kt` | Utilities | `clickableWithRipple`, `uppercaseCharCompat`, `cleanUri`, `DonationCardSmall` |

### Data Layer
| File | Purpose | Key Details |
|------|---------|-------------|
| `data/model/SipModels.kt` | Data classes | `SipAccount` (with `displayName` computed property), `CallSession`, `CallLogEntry` (has `durationSeconds`), enums: `Transport`, `PreferredCodec`, `RegStatus`, `CallDirection`, `CallState` |
| `data/model/Contact.kt` | Contact model | `id`, `name`, `numbers: List<String>`, `photoUri`, `isFavorite` |
| `data/repository/AccountRepository.kt` | Account persistence | DataStore-backed, manages SIP accounts, settings (dark mode, calling cards, DND, ringtone) |
| `data/repository/CallLogRepository.kt` | Call log persistence | SharedPreferences JSON, singleton, max 200 entries. Stores `durationSeconds` |
| `data/repository/ContactsRepository.kt` | Device contacts | Queries `ContactsContract.CommonDataKinds.Phone`. Groups by `CONTACT_ID`. Dedup: exact string match only (no normalization!) |

### Theme & Utils
| File | Purpose |
|------|---------|
| `ui/theme/IPDialTheme.kt` | Material3 theme, colors (defines `ForestGreen`, `EndRed`) |
| `ui/theme/IPDialTypography.kt` | Typography |
| `util/SipLogger.kt` | Ring-buffer logger for activity log |
| `util/UpdateChecker.kt` | GitHub release checker |

## Navigation Structure
- **Bottom tabs**: Home, Keypad (visible only on these 2 routes)
- **Drawer items**: Home, Accounts, Recordings, Settings, About
- **Other routes**: Logs (from Settings)
- **Call overlay**: When `callSession != null`, replaces all navigation with `IncomingCallScreen` or `CallScreen`

## Key Behavioral Details
- `IPDialTopBar` uses `statusBarsPadding()` + fixed 56dp height
- HomeScreen uses bare `Column`, other screens use `Scaffold(topBar=...)`
- Drawer is RTL-wrapped to open from right side
- Call duration is tracked in `SipService.observeCallState()` — stored as `callStartTime` on CONFIRMED, calculated on disconnect
- Contact matching: extracts digits, uses last-10-digit suffix matching
- BD number handling: 11-digit starting with 0 → +880..., 10-digit starting with 1 → +880...

## Recent Fixes & Architecture Updates (June 2026)
1. **Centralized Registration & Reconnection**: All `SipEngine` account registrations, updates, and network-state observations have been centralized inside `SipService` (the foreground service).
2. **Infinite Registration Loop Fix**: Added an `activeConfigs` cache (`ConcurrentHashMap`) in `SipService` to observe only actual database configuration changes. Updates to registration status (e.g. `REGISTERED`, `ERROR`) no longer trigger duplicate registration calls, eliminating the infinite loop and the native PJSIP crash (`SIGSEGV` in `pj::Account::setRegistration`).
3. **Background Network Reconnection**: The network availability callback was moved to `SipService` to guarantee reliable re-registration and reconnection (via `SipEngine.forceReconnectAll()`) when the app is running in the background.
4. **Resolved UI & Functional Issues**:
   - Fixed navbar height and empty space.
   - Removed intermediate filters ("Missed", "Dialed", "Received") from the Home screen.
   - Normalized contact numbers to eliminate format duplicates.
   - Added multi-number selection popup for contacts with multiple numbers.
   - Displayed formatted call duration in the history list.
   - Added detailed call history dialog (recent 7 days) on tapping history items.
   - Moved update check interface from About to Settings.
   - Removed redundant app title from the About screen logo.
   - Improved incoming call UI visibility/photo alignment and introduced the top-level Incoming Call Banner.
