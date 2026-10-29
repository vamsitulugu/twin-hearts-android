# Feature Audit — Period Tracker

**Web source (read-only reference):** `us-app/public/index.html` → `#page-period`, `#periodModal`, `logPeriod / saveSymptoms / getPeriodPhase / renderPeriodPage / renderPeriodAnalytics / renderPeriodCalendar / delPeriod`; `public/gender-access.js` (who may edit); `routes/data.js` (server rule + push).
**Reference screenshot:** `docs/reference/images.pdf`, page 41 (view-only account).

## Who can do what (web `Gender`)
| Person | Can |
|---|---|
| Girl (`female`) | Log periods, tick + save symptoms, delete entries. |
| Boy (`male`) | See everything. Read-only. Banner: "👀 View only — {partner} manages this page. You can follow everything they log." |
| Not chosen | Read-only + banner "Tell us if you are a girl or a boy…" + **Choose** dialog (asked once per visit, "Not now" allowed). |

Gender is read from the shared profile (`profile.u1|u2.gender`), then falls back to the sign-up choice stored in the session. When saving, the app also writes the gender into the shared profile so the **server rule** works: an account marked `male` has `periods`/`symptoms` ignored by `POST /api/data/state`.

## Screen (top to bottom, as in the reference)
1. Access banner (only if not allowed to edit).
2. Insight card: "Log at least 2 periods to see cycle trend insights." / "Your cycle is very regular — averaging N days." / "Your average cycle is N days, varying by up to M days." (regular = max−min ≤ 3).
3. Hero card: "🩷 Period Tracker", phase badge (Menstrual / Follicular / Ovulation / Luteal / Start Tracking), **📝 Log Period** (girl only), stats *Days to Next* (— when empty) · *Cycle Length* (28) · *Flow Days* (5), tip line for the phase.
4. **Cycle Calendar** (current month): period days (burgundy), ovulation day = start + cycle − 14 (green), fertile ±2 days (yellow), today highlighted. A small legend was added for clarity.
5. **Recent Cycle Lengths** bars (last 6 periods; hidden with fewer than 2).
6. **Most Common Symptoms** (top 5; hidden when none).
7. **Today's Symptoms**: 10 chips (Cramps, Mood Swings, Bloating, Fatigue, Headache, Back Pain, Tender, Nausea, High Energy, Acne) + **Save Today's Symptoms** (girl only). Tapping a chip without access shows the web toast.
8. **Period History** (newest first, "Flow: N days · Cycle: N days", note, delete for girl) / "No periods logged yet".
9. **Log Period** sheet: Period Start Date, Flow Duration (1–14, default 5), Cycle Length (20–45, default 28), Notes → "Save 🩷" → toast "Period logged! 🩷".

States: loading skeleton, content, empty (hero + "No periods logged yet"), error with retry (R-41).

## API (R-65)
- `GET /api/data/state/{coupleId}` and `POST /api/data/state` (same as Bucket List).
- Keys we write: `periods` (array of `{id,start,flow,cycle,note}`, newest first), `symptoms` (object `yyyy-MM-dd → [names]`), `profile` (only to add our gender). Other keys are untouched (server merges top-level keys).
- Push: the server already notifies the partner "🩷 Period Logged" when `periods` grows (`ARRAY_WATCHERS`); nothing to do in the app.

## Not ported (with reason)
- Month navigation on the calendar: the web page shows only the current month.
- Search/Menu in the top bar (no app-wide search/menu yet).
- Bottom bar on this page (core shows it only on the five tab screens).
- Light theme (R-54).
- Yellow colour is a private feature constant (`PeriodColors.kt`); see CHANGE REQUEST in `SHARED_CHANGES_bucket_period.md`.
