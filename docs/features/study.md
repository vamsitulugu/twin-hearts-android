# Feature: Study

Native Compose page; the web app (`us-app`) was the reference. Entry: Home "Study" shortcut → `Routes.Study`. Code: `feature/study/`.

## What it has
Chapters ring + weekly hours, study-rhythm tip, weekly hours bars, my subjects (+/- chapters, delete) and timetable (add, tick done, delete), my partner's subjects and timetable to look at.
Polish pack: slots saved with an **unknown day name** are shown under that name (and counted in the hours) instead of vanishing; **petals** when a slot is ticked done; phone **reminders**; offline copy + banner; auto-refresh every ~30 s.

## Server calls
`GET api/data/state/{coupleId}` / `POST api/data/state`. Keys: `mySubjects`/`myTimetable` (user1) and `ptSubjects`/`ptTimetable` (user2); `reminders` when a slot has an alarm and a start time (entry has `by`).

## Decisions
- Each person writes only their own pair of keys, and reads the other pair as "partner". The web lets either person overwrite the `my*` keys; mapping by role avoids that, but a couple that used the web as the "wrong" role will see their old data under the other person. Check once on a real account.
- Writes are queued (Mutex) so quick +1 taps are never dropped.

## Not done / known gaps
- Not compiled here (no Android SDK). Concurrent edits: later save wins.
