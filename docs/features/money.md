# Feature: Money

Native Compose page; the web app (`us-app`) was the reference. Entry: Home "Money" shortcut → `Routes.Money`. Code: `feature/money/`.

## What it has
Saved-ring hero with the month's net, spending tip, income/expense/savings tiles, spending by category (donut), 8-week trend, transactions list (add / delete), each partner's list, and my monthly goal.
Polish pack: **month picker** (‹ ›, tap the title or "Back to this month" to return) driving the hero, tiles, donut and tip; items with an unreadable date are listed as "No date" and left out of month maths; the goal **saves itself** ~0.8 s after typing (no Save button, no toast); **tap a trend bar** to see its amount; offline copy + banner; auto-refresh every ~30 s.

## Server calls
`GET api/data/state/{coupleId}` / `POST api/data/state`. Keys: `transactions` (array, newest first like the web's `unshift`) and `finGoals` (object per role; my role's value is replaced, the partner's kept).

## Decisions
- Lists keep the saved order (no re-sorting), like the web. Limits: 60 in the main list, 30 per person.
- All writes run one after another (Mutex) because each is read-then-save; a goal change typed while an add is saving is queued, not dropped.
- Category colours in the donut stay distinct per category (the web uses one hue family); deliberate, easier to read.
- Rupees are grouped Indian style by hand (`formatRupees`) so unit tests match the phone.

## Not done / known gaps
- Not compiled here (no Android SDK). Concurrent edits from two phones: later save wins (needs backend per-item endpoints).
- Editing an existing transaction is not in the web either; delete and re-add.
