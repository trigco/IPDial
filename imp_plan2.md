# Ad and Balance Integration — Implementation Tracker

- [x] **Task 1: Balance Fetching & Ad Logic**
    - [x] Host filtering (sip.amarip.net / 103.170.231.10)
    - [x] Ad trigger only after 5 checks (implemented in `fetchBalance`)
    - [x] Auto-dismiss after 10s (standard `triggerAd` behavior)
- [x] **Task 2: Audio Record Ad Trigger**
    - [x] Trigger ad when playing recording (synced with `mediaPlayer` duration)
    - [x] Trigger ad on share button click (10s auto-dismiss)
- [x] **Task 3: Codec Selection Ad Trigger**
    - [x] Trigger ad after 3 codec interactions (threshold implemented)
    - [x] Auto-dismiss after 10s
- [x] **Task 4: Ad Component UI Update**
    - [x] Updated script key to `45b31fc24c18f055ba13d7742fbd8eae` in `MainActivity.kt`
    - [x] Added support message "Kindly see ads to support developer" in `AdDialog`
    - [x] Ensured "X" close button visibility and correct alignment
