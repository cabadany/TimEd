# Mobile Integration Tasks

Based on `MOBILE_INTEGRATION_PLAN.md`.

## Phase 1: Foundation
- [/] **Create `TimeSettingsManager.kt`**
    - [ ] Create singleton object in `com.example.timed_mobile.utils`.
    - [ ] Implement `fetchTimeSettings()` to listen to `settings/timeWindow` and `settings/lateThreshold`.
    - [ ] Implement `isTimeInAllowed()`:
        - [ ] Return `false` if too early (before start time).
        - [ ] Return `true` if too late (after end time).
        - [ ] Return `false` if in break window (hardcoded 12:00 PM - 1:00 PM for now, or fetch if available).
    - [ ] Implement `isTooEarlyToTimeOut()`:
        - [ ] Return `true` if current time < end time.
    - [ ] Implement `isLate()`:
        - [ ] Return `true` if current time > late threshold.

## Phase 2: Home Screen Enforcement (Guard Clauses)
- [/] **Update `HomeActivity.kt`**
    - [ ] Locate `setupActionButtons()`.
    - [ ] **Time In Button**: Add guard clause at top of listener.
        - [ ] Check `!TimeSettingsManager.isTimeInAllowed()`.
        - [ ] Show specific error dialog (Too Early or In Break).
        - [ ] `return@setOnClickListener` if blocked.
    - [ ] **Time Out Button**: Add guard clause at top of listener.
        - [ ] Check `TimeSettingsManager.isTooEarlyToTimeOut()`.
        - [ ] Show error dialog (Too Early).
        - [ ] `return@setOnClickListener` if blocked.

## Phase 3: Activity Logic Replacement (Cleanup)
- [ ] **Update `TimeInActivity.kt`**
    - [ ] `checkAndCapturePhoto()`: Remove hardcoded `startTime`/`endTime` and `ENFORCE_TIME_WINDOW`.
    - [ ] Replace with `TimeSettingsManager.isTimeInAllowed()` check (mostly for redundancy/safety).
    - [ ] `logTimeIn()`: Add `"attendanceBadge"` to the payload.
        - [ ] Use `TimeSettingsManager.isLate()` to set value to "Late" or "On Time".

- [ ] **Update `TimeOutActivity.kt`**
    - [ ] `logTimeOutToFirebase()`: Remove hardcoded `TIMEOUT_HOUR` and `ENFORCE_TIMEOUT_WINDOW`.
    - [ ] Replace with `TimeSettingsManager.isTooEarlyToTimeOut()` check.

## Phase 4: Verification
- [ ] **Test Scenarios**
    - [ ] **Too Early Time-In**: Set DB start time to future. Verify Popup on Home Screen.
    - [ ] **Too Late Time-In**: Set DB end time to past. Verify Allowed & Badge is "Late".
    - [ ] **Break Time**: Set time to 12:30 PM. Verify Popup.
    - [ ] **Too Early Time-Out**: Set DB end time to future. Verify Popup on Home Screen.
    - [ ] **Valid Time-Out**: Set DB end time to past. Verify Allowed.
