# Time-In Window Guide (24-Hour Format)

1. **Input Format**: Use `HH:mm` to represent time (e.g., `07:00` for 7 AM, `13:30` for 1:30 PM) so there's no AM/PM ambiguity.
2. **Enforced Window**: The app enforces `13:30` through `17:00` when `ENFORCE_TIME_WINDOW` is `true`. Internally, the logic compares the current `Calendar` against these boundary timestamps.
3. **User Message**: Display the message `"You can only Time-In between 13:30 and 17:00."` whenever the check prevents a time-in, matching the internal range.
4. **Toggle Behavior**: Setting `ENFORCE_TIME_WINDOW = false` bypasses the restriction entirely while still logging the actual time for records.
5. **Examples**: 7 AM → `07:00`, 9:15 AM → `09:15`, 1:30 PM → `13:30`, 5 PM → `17:00`. Match this everywhere you mention the window to keep guidance consistent.
6. **Full Hour Reference**: 
	- `01:00` → 1 AM
	- `02:00` → 2 AM
	- `03:00` → 3 AM
	- `04:00` → 4 AM
	- `05:00` → 5 AM
	- `06:00` → 6 AM
	- `07:00` → 7 AM
	- `08:00` → 8 AM
	- `09:00` → 9 AM
	- `10:00` → 10 AM
	- `11:00` → 11 AM
	- `12:00` → 12 PM (noon)
	- `13:00` → 1 PM
	- `14:00` → 2 PM
	- `15:00` → 3 PM
	- `16:00` → 4 PM
	- `17:00` → 5 PM
	- `18:00` → 6 PM
	- `19:00` → 7 PM
	- `20:00` → 8 PM
	- `21:00` → 9 PM
	- `22:00` → 10 PM
	- `23:00` → 11 PM
	- `24:00` → 12 AM (midnight)
7. **Audit Note**: Even when enforcement is disabled, the system still records timestamps, so ensure external guidance notes whether the 24h window was active during the session.
