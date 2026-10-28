# Feature: Calendar

Native Compose page; the web app (`us-app`) was the reference. Entry: Home "Calendar" shortcut → `Routes.Calendar`. Code: `feature/calendar/`.

## What it has
Month grid (dot on days with events, today ring, selected day), Today button, events of the tapped day, next 10 upcoming events, Add Event sheet (title, date, time, note, category, alarm), delete with confirm.
Polish pack: long-press **multi-select** (selection bar, tap to add/remove, delete selected with one confirm; Back leaves selection), petals after adding, **offline copy** (banner when there is no connection), silent **auto-refresh** every ~30 s while open, phone **reminders** (see reminders.md).

## Server calls
`GET api/data/state/{coupleId}` and `POST api/data/state` (patch of top-level keys). Keys written: `events` (array replaced whole) and `reminders` (when an alarm + time is set; entry `{id,title:"📅 …",date,time,forWhom,note,fired:false,type:"event",by}`).

## Decisions
- Deleting an event also removes the reminder that was filed with it (matched by type, title, date, time), so it cannot ring afterwards. The web leaves that reminder behind.
- Only `events` is cached on the phone (key `calendar:<coupleId>`), never the whole state (it can hold large photos).
- Writes need a connection; offline the buttons show the normal error toast.

## Not done / known gaps
- Not compiled here (no Android SDK).
- Two phones editing at the same moment: the server replaces the whole `events` array, so the later save wins. Needs a backend change (per-item endpoints) to fix.
- No search across events (no global search feature exists yet).
