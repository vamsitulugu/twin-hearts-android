# Our Profile — feature audit (Phase 2.6)

Old web files read: `public/profile.js`, `public/profile.css`, the `#page-profile` markup, `pushMyProfile` / `confirmRemovePartner` / `sendPartnerRequestFromProfile` in `index.html`, `routes/profile.js`, `routes/partner.js`, `routes/auth.js` (unpair), `routes/data.js`, `routes/location.js`. Look: reference sheet `docs/reference/images.pdf`, page "Our Profile".
Opened from: Home avatar / More → `Routes.Profile` (one route). The 6 sub-screens use the page's own back stack; Back returns to the screen before, and from Main to the previous page.

## Screens
| Screen | What it does |
|---|---|
| Main (Our Profile) | Couple hero (my photo with camera badge, "Together since" + date, partner photo, presence), years/months/days together, Edit Profile / Partner Details / Add Date, counters (Memories, Milestones, Special Dates, Dreams), Next Important Date, Our Story card, ⋮ menu |
| Partner Details | Photo (tap = full screen), connection pill, Message, Edit Details, birthday + zodiac, love language, about, last active, Partner Connection (send request by 10-digit phone), Remove Partner (two confirmations) |
| Profile & Settings | Grouped list (⋮ menu) |
| Milestones | Timeline by year, add (6 types + custom), delete with confirm |
| Love Languages | Pick yours and your partner's (tap again clears), tip for the pair |
| Our Story | Two text areas, Save, then back |
Sheets: Edit Profile (name, birthday, together since, Girl/Boy, bio), Edit Details, Add Event (title, date, time, note, 7 categories, alarm), Add Milestone.
States: loading, content, error with Try again; empty states for milestones and next date (R-41).

## API calls
`GET|POST /api/data/state` (keys `myName`, `myBday`, `anniversary`, `myBio`, `partnerBio`, `partnerName`, `ptBday`, `myLoveLang`, `ptLoveLang`, `events`, `reminders`, `milestones`, `settings`, `profile.u1|u2`), `GET /api/profile/:userId`, `GET /api/profile/:userId/partner`, `POST /api/profile/:userId/avatar` (multipart, photo shrunk to 800 px JPEG first), `GET /api/partner/status/:userId`, `POST /api/partner/request`, `POST /api/auth/unpair`, `GET /api/location/:coupleId` (partner "last seen").
Saves read the newest state first and send only the changed keys, so the partner's newer data is never overwritten.

## Native behaviour
- Photo picker uses the system picker (no storage permission). Export My Data saves a `.json` file with the system "Save as" screen (no permission).
- Removing a partner as the **second** person signs you out (same as the web); the first person stays signed in.
- Name, anniversary and gender are also saved on this phone's session, so Home and other screens update at once.

## Not ported (nothing dropped silently)
| Web feature | Why |
|---|---|
| Top-bar Search | Global search belongs to Home; only Sync (reload) is shown |
| Notifications / Privacy / Backup / Theme rows | Open `Routes.Settings`, which is not built yet (shows the placeholder) |
| "Relationship Goals" and the Dreams counter | Open the Us tab (Dream Goals) |
| Memories counter / row | Open Collection (Our Memories camera page is a later phase) |
| Account details & change email/password | Not part of the web profile page |
| Offline/last-backup status text | `settings.lastBackup` is written on export but not shown |

## To verify on the phone
- `profiles.avatar_url` upload needs the server's Supabase `avatars` bucket (already used by the web app).
- Calendar events added here use the same fields as the Calendar feature, so they appear there.
