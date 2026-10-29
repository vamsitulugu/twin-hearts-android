# Surprises (Phase 4) — Surprises + Love Capsule

Opened from Home → Plans → **Surprises**. Reference: `docs/reference/images.pdf`, page 44
("Surprises" with the Surprises / Love Capsule tabs, empty state).

## What it does (copied from the web, `#page-surprise`)
| Part | Behaviour |
|---|---|
| Surprises tab | "+ New" opens a sheet: type (🎁 💌 📷 🎬 🎙️), title, message, optional photo / video / audio, unlock date, **Hide it Away 🎁**. |
| Locked, partner's | Only "A surprise is waiting for you! Opens <date> 🎉". No title, no content, no delete. |
| Locked, mine | Title and "Opens <date>"; can be deleted. |
| Unlocked | Title, message and file (photo opens full screen; video / voice note play on tap). |
| Love Capsule tab | Write a message, pick a date, **Seal Capsule 🔒**. Both partners see the list. Locked shows "A sealed message is waiting...". On/after the date the **Open** button reveals the message. Either partner can delete. |
| XP | +10 for a surprise, +15 for a capsule, added to `xp` in the same save (as the web's `addXP`). |
| Toasts | Same wording as the web: "Surprise hidden! 🎁", "Capsule sealed! 🔒", "Title and unlock date required", "Write a message and pick a date", "🔒 Still locked until <date>". |

## Where the data lives
Shared state document (`/api/data/state`), keys `surprises`, `capsules`, `xp`. Field names are the web's own
(`id, type, title, msg, mediaUrl, unlock, by, opened` / `id, msg, unlock, by, created, opened`), so the web
app and the phone read each other's items. Each save reads the newest state first and sends only the keys it changed.
Files go through `/api/media/upload` (photo, video) and `/api/media/upload-voice` (audio).

## Differences from the web (on purpose)
1. **Files are uploaded, not embedded.** The web stores a `data:` link inside the state (megabytes of base64). The
   phone uploads the file and saves its link, plus two extra fields the web ignores: `mediaKind`, `mediaPath`.
   It still *reads* the web's `data:` links. See `backend-changes/surprises/web-render-patch.md`.
2. **New items are appended**, not put first. The server's push-notification check looks at items added at the end of
   a list; the screen sorts newest first anyway.
3. **Today = the phone's local date.** The web uses the UTC date, so for a few hours after midnight (India: 00:00–05:30)
   the web still showed yesterday.
4. **Unlock date cannot be in the past** (the picker starts today). The web accepted any date.
5. Photos are shrunk to 1600 px; videos up to 50 MB, audio up to 15 MB (server limit), checked when the file is picked.

## Not built (and why)
- Search and the ☰ menu in the top bar: their screens do not exist yet (no dead taps, R-42).
- The bottom navigation bar on this page: other sub-screens (Calendar, Movie…) do not show it either; back returns to Home.
- The web's `openSurprise()` / gift-opened animation: it exists in the web code but no button calls it, so it was not ported.

## Files
`feature/surprises/` — `SurprisesGraph.kt`, `domain/` (models + rules, no Android), `data/` (API, mapper, repository, uploader),
`di/SurprisesModule.kt`, `ui/` (screen, cards, sheet, media, form parts, ViewModel).
Strings: `res/values/strings_surprises.xml`. Tests: `src/test/.../feature/surprises/`.
