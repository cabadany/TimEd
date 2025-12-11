# Mobile Integration Plan: Time Logic Synchronization

This document outlines the plan to integrate the dynamic time settings (Time Window, Late Threshold, Break Window) from Firebase Realtime Database into the Android mobile application.

## 1. Critical Constraints

*   **Preserve Existing Logic**:
    *   **HomeActivity.kt**: Do NOT modify any existing tutorial logic (`isInteractiveTutorialActive`), intent extras, or UI animations. The time check must be added as a guard clause at the *start* of the listener.
    *   **home_page.xml**: Do NOT modify this file.
    *   **Activities**: Do NOT modify camera/face detection in `TimeInActivity` or `TimeOutActivity`.
*   **Surgical Changes Only**:
    *   In `HomeActivity.kt`: Insert `if` checks at the top of `btnTimeIn` and `btnTimeOut` listeners.
    *   In `TimeInActivity.kt`: Replace hardcoded time checks in `checkAndCapturePhoto` and add badge in `logTimeIn`.
    *   In `TimeOutActivity.kt`: Replace hardcoded time checks in `logTimeOutToFirebase`.
*   **No Refactoring**: Do not attempt to refactor the activity structure or move code unless absolutely necessary for the time check.

## 2. Firebase Database Structure

We will utilize the following existing and proposed nodes in Firebase Realtime Database:

*   `settings/timeWindow`:
    *   `start` (String, "HH:mm", e.g., "13:30") - Earliest allowed Time-In.
    *   `end` (String, "HH:mm", e.g., "17:00") - Latest allowed Time-In (and potentially Earliest Time-Out).
*   `settings/lateThreshold` (String, "HH:mm", e.g., "09:00") - Time after which a Time-In is marked as "Late".
*   `settings/breakWindow` (Proposed):
    *   `start` (String, "HH:mm", e.g., "12:00")
    *   `end` (String, "HH:mm", e.g., "13:00")

## 2. Implementation Strategy

### Step 1: Create `TimeSettingsManager`
Create a singleton helper class `TimeSettingsManager` to handle fetching and caching of time settings.

*   **Responsibilities**:
    *   Fetch `timeWindow`, `lateThreshold`, and `breakWindow` once on app start (or `HomeActivity` resume).
    *   Provide helper methods:
        *   `isTimeInAllowed(currentTime: Calendar): Boolean`
        *   `isLate(currentTime: Calendar): Boolean`
        *   `isInBreak(currentTime: Calendar): Boolean`
        *   `isTooEarlyToTimeOut(currentTime: Calendar): Boolean`
        *   `getFormattedTimeWindow(): String`

### Step 2: Update `HomeActivity.kt`

**Target Function**: `setupActionButtons()`

*   **Action**: Locate the `btnTimeIn.setOnClickListener` and `btnTimeOut.setOnClickListener` blocks.
*   **Modification**: Insert a **Guard Clause** at the very beginning of the listener. Do NOT wrap or indent the existing code to minimize changes.
*   **Logic**:
    *   **Time In Button**:
        *   **Insert at Top**:
            ```kotlin
            if (!TimeSettingsManager.isTimeInAllowed()) {
                // Show "Too Early" or "In Break" Popup
                return@setOnClickListener
            }
            // ... Existing code (Tutorial logic, etc.) remains below ...
            ```
        *   **Note**: "Too Late" is allowed, so no check needed for that scenario.

    *   **Time Out Button**:
        *   **Insert at Top**:
            ```kotlin
            if (TimeSettingsManager.isTooEarlyToTimeOut()) {
                // Show "Too Early" Popup
                return@setOnClickListener
            }
            // ... Existing code remains below ...
            ```

### Step 3: Update `TimeInActivity.kt`

**Target Function**: `checkAndCapturePhoto(uid: String)`

*   **Action**: Replace the hardcoded `startTime`/`endTime` and `ENFORCE_TIME_WINDOW` check.
*   **Modification**:
    *   Remove the hardcoded `Calendar` setup for 7:00 AM and 5:00 PM.
    *   Insert call to `TimeSettingsManager.isTimeInAllowed()`.
*   **Logic**:
    *   **If Allowed**: Proceed to `checkAlreadyTimedIn(uid)`.
    *   **If Too Early**: Show AlertDialog: "You are too early. Time In starts at [Start Time]." and return.
    *   **If Too Late**: **ALLOW**. (Do not return, proceed to `checkAlreadyTimedIn`).
    *   **If In Break**: Show AlertDialog: "Cannot Time-In during break hours..." and return.

**Target Function**: `logTimeIn(imageUrl: String, studentUid: String)`

*   **Action**: Add `"attendanceBadge"` to the `log` map.
*   **Logic**: `val badge = if (TimeSettingsManager.isLate()) "Late" else "On Time"`

### Step 4: Update `TimeOutActivity.kt`

**Target Function**: `logTimeOutToFirebase()`

*   **Action**: Replace the hardcoded `TIMEOUT_HOUR` and `ENFORCE_TIMEOUT_WINDOW` check.
*   **Modification**:
    *   Remove the hardcoded `targetTimeOut` setup.
    *   Insert call to `TimeSettingsManager.isTooEarlyToTimeOut()`.
*   **Logic**:
    *   **If Too Early**: Show AlertDialog: "Too Early to Time-Out. Please wait until [End Time]." and return.
    *   **If Allowed**: Proceed with existing `dbRef.push().setValue(log)` logic.

## 3. Detailed Logic Specifications

### Parsing Times
*   All times from Firebase are in "HH:mm" (24-hour) format.
*   Mobile app must parse these strings into `Calendar` objects or compare hour/minute integers against `Calendar.getInstance()`.

### Fallbacks
*   If Firebase fetch fails or nodes are missing, fallback to default values:
    *   Time In Window: 07:00 - 17:00
    *   Late Threshold: 09:00
    *   Break Window: 12:00 - 13:00

## 4. Proposed Code Changes (Draft)

#### `TimeSettingsManager.kt` (New File)
```kotlin
object TimeSettingsManager {
    var timeWindowStart: String = "07:00"
    var timeWindowEnd: String = "17:00"
    var lateThreshold: String = "09:00"
    // ... fetch logic ...
}
```

#### `TimeInActivity.kt`
```kotlin
// In checkAndCapturePhoto
if (!TimeSettingsManager.isTimeInAllowed(now)) {
    // Show error
    return
}

// In logTimeIn
val badge = if (TimeSettingsManager.isLate(System.currentTimeMillis())) "Late" else "On Time"
val log = mapOf(..., "attendanceBadge" to badge)
```

## 5. Verification Plan
1.  **Unit Tests**: Test `TimeSettingsManager` logic with various time inputs.
2.  **Integration Test**:
    *   Change Firebase settings.
    *   Verify Mobile App respects new Time Window immediately (or after restart).
    *   Verify "Late" badge appears in Dashboard when timing in after threshold.
