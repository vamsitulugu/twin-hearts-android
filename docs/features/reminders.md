# Feature: Phone reminders (core/reminders)

Rings the phone at the time of a shared reminder, even when the app is closed. Used by Calendar (events with an alarm) and Study (slots with an alarm). The web app's own reminder rules were the reference (`checkAlarms` in `us-app`).

## How it works
- Calendar and Study already read/save the shared state (`reminders` key). After each read or save they pass that array to `RemindersSync.sync(reminders, role)`.
- `ReminderRules.upcoming` (pure, unit tested) picks what this phone should ring for: not `fired`, readable date + time, time still in the future, nearest 60.
- `ReminderScheduler` replaces the whole schedule each time (`AlarmManager.setAndAllowWhileIdle`, no special permission) and saves it in SharedPreferences. `ReminderBootReceiver` restores it after a restart or app update.
- `ReminderReceiver` shows the notification (channel "Reminders"); tapping opens the app. Nothing is shown if the notification permission is denied.
- The permission (Android 13+) is asked once, when the person saves an item with an alarm.

## Who it rings for
| `forWhom` | Rings on |
|---|---|
| `both` | both phones |
| `self` (or blank) | the phone that made it. New reminders carry `by: "user1"/"user2"`; reminders from the web have no `by`, so they ring on whichever phone reads them |
| `partner` | never on this phone |

## Known gaps
- The schedule is refreshed only when Calendar or Study loads (or auto-refreshes while open). A reminder added on the other phone rings here after the next load; open the page, or wait for it.
- Alarms are "inexact but prompt" (can be a minute or so late in battery saver). Exact alarms would need the extra "Alarms & reminders" permission.
- A Study reminder is filed for today (like the web), so a slot for another weekday rings today at its start time only if that time is still ahead.
- The web marks reminders `fired` on its side; the phone does not write that back. Deleting the item removes the reminder.
