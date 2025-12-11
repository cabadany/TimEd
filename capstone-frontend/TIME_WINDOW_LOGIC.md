# Time Window, Late Threshold, and Break Window Logic

This document explains the logic behind the Time Window, Late Threshold, and Break Window features found in `src/Dashboard/Dashboard.jsx`.

## 1. Late Threshold

The **Late Threshold** determines the time after which a faculty member's time-in is considered "Late".

*   **State**: `lateThreshold` (default: `'09:00'`).
*   **Storage**: Stored in Firebase Realtime Database at `settings/lateThreshold`.
*   **Loading**: `loadLateThreshold()` fetches the value on component mount and sets up a real-time listener.
*   **Saving**: `saveLateThreshold(newThreshold)` updates the value in Firebase. Only authenticated users (and effectively admins, based on rules) can modify this.
*   **Usage**:
    *   **Display**: Shown in the "Late Threshold" card.
    *   **Logic**:
        *   The `getLateAttendanceDetails()` function uses this threshold to filter faculty logs for the "Late Faculty Members" modal. It compares the `timeIn` timestamp of the log against the `lateThreshold` time for the selected date.
        *   *Note*: The main statistics counters (`attendanceStats`) rely on the `attendanceBadge` property ('On Time', 'Late', 'Absent') stored directly in the `timeLogs` database entries, rather than recalculating it on the frontend using the current threshold state.

## 2. Allowed Time-In Window

The **Allowed Time-In Window** defines the valid time range during which faculty members can time in.

*   **State**: `timeWindow` object with `start` and `end` properties (default: `start: '13:30'`, `end: '17:00'`).
*   **Storage**: Stored in Firebase Realtime Database at `settings/timeWindow`.
*   **Loading**: `loadTimeWindow()` fetches the `start` and `end` times from Firebase.
*   **Saving**: `saveTimeWindow(start, end)` updates the values in Firebase. Checks ensure `start` is before `end`.
*   **Usage**:
    *   **Display**: Shown in the "Allowed Time-In Window" card.
    *   **Logic**: This setting is primarily for display in the Dashboard and likely enforced on the mobile/client side (TimeInActivity) or backend functions that create the logs, as the Dashboard mainly displays this setting.

## 3. Faculty Break Window

The **Faculty Break Window** is currently a **static display element**.

*   **Value**: Hardcoded as `12:00 PM - 1:00 PM`.
*   **Usage**:
    *   Displayed in the "Faculty Break Window" card.
    *   **Logic**: There is currently no dynamic state, database storage, or active logic (like preventing time-ins) associated with this specific break window in the `Dashboard.jsx` file. It is purely informational in the current implementation.

## Relevant Functions

*   `loadLateThreshold()`: Fetches late threshold setting.
*   `saveLateThreshold()`: Saves late threshold setting.
*   `loadTimeWindow()`: Fetches time window setting.
*   `saveTimeWindow()`: Saves time window setting.
*   `getLateAttendanceDetails()`: Filters logs based on `lateThreshold` to show the list of late faculty.
*   `calculateAttendanceStats()`: Aggregates attendance counts (Present, Late, Absent) based on the `attendanceBadge` field in the logs.
