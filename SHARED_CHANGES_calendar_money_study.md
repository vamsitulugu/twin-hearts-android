# SHARED_CHANGES — Calendar, Money, Study polish pack
(Everything below is already applied in this zip. Named with `_calendar_money_study` so it does not overwrite other features' files.)

## Gradle / catalog lines
None.

## Manifest entries (applied in this zip's AndroidManifest.xml)
- `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`
- `<receiver android:name=".core.reminders.ReminderReceiver" android:exported="false" />`
- `<receiver android:name=".core.reminders.ReminderBootReceiver" android:exported="false">` with BOOT_COMPLETED and MY_PACKAGE_REPLACED.
If you changed AndroidManifest.xml after commit 311d0d5, merge these three entries by hand instead of overwriting.

## AppNavHost / Routes / Home
None. The repo at 311d0d5 already has the Calendar, Money and Study routes, graphs and Home shortcuts (the "routes fix"). This zip does not touch the navigation files.

## New shared code (core)
`core/reminders/*` (ReminderRules, ReminderScheduler, ReminderReceiver, ReminderBootReceiver, RemindersSync, NotificationPermission), `core/data/StateCache.kt`, `core/common/AutoRefresh.kt`, `core/design/Petals.kt`, `core/design/OfflineBanner.kt`, `res/values/strings_polish.xml`.

## TwinDatabase entities
None. The existing `cache_entry` table is used (keys `calendar:<id>`, `money:<id>`, `study:<id>`).

## Backend changes
None.

## CHANGE REQUEST (not applied)
Promote `PetalBurst` to more pages (Calendar/Study use it now); consider per-item backend endpoints to remove the "later save wins" limit.
