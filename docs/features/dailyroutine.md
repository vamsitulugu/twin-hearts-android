# Daily Routine (Phase 4)

Android version of `us-app/public/daily-routine.html` + `routes/daily-routine.js` (mounted at `/api/daily-routine`).
Reference screenshots: `docs/reference/images.pdf` pages 23 and 25.

## What it does
Mine / Partner tabs · date bar with previous / next / date picker · 7-day strip (2 back, 4 ahead) · summary card (percent, remaining, Next) ·
day list with NOW line and free-time gaps (20 min or more, tap "+ Add" to prefill) · Add / Edit sheet · detail sheet
(Mark Complete, Skip, Edit, Duplicate, Delete with confirm) · clock tick every minute.

## Rules copied from the web (all in `domain/RoutineLogic.kt`)
Which routines fall on a day (none / daily / weekdays / weekends / custom, Sunday = 0) · status from the clock
(upcoming, in progress, missed; completed and skipped are kept; overnight routines) · overlap check on my own day ·
partner routines show only when "Shared", and are read-only · 12-hour times · same error texts.

## API
`GET /api/daily-routine/:coupleId`, `POST /`, `PUT /:id`, `PATCH /:id/status`, `POST /:id/duplicate`, `DELETE /:id`
(the server archives, the web never hard-deletes). Rows are read as raw JSON and bad rows are skipped.

## Notes
- Edit saves the open day as the routine's `date`, exactly like the web.
- The form offers four repeats; a server-side "custom" routine edited here becomes "Does not repeat" (same as web).
- Reminders are stored for the server; the web page itself sets no local alarm, so neither does this screen.
- Reached from the Home "Daily Routine" chip (`Routes.DailyRoutine`). Bottom bar is hidden here like other sub-pages.

## Tests
`RoutineLogicTest` (rules), `RoutineViewModelTest` (load, validation, conflict, save, status, delete).
