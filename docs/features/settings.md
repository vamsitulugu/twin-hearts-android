# Settings (2.5) — audit and explanation

Web source: `public/index.html` (`#page-settings`, `SettingsUI`, `saveSettings`, `loadStorageStats` ...), `routes/auth.js`, `routes/partner.js`, `routes/data.js`, `routes/profile.js`. Reference screenshots: `docs/reference/images.pdf` pages 10–11.

## What it is (plain English)
One screen with a list. Each row opens a sub-page inside the same screen (like the web). Back goes from a sub-page to the list; the open sub-page survives the phone killing the app (`SavedStateHandle`).

## Screens and what each one does
| Screen | What the person can do | Where it is saved |
|---|---|---|
| **List (home)** | Couple header (two avatars, names, "Together since … · N days" or "Add your anniversary"), search box (24 entries, same words as the web), 11 rows in 6 groups, **Sync** button in the top bar, search icon focuses the box | — |
| **Profile** | Change photo (photo picker → resized JPEG → upload), your name, partner's name, "I am" Girl/Boy, Together since, your birthday, partner's birthday, **Save changes** | Server shared state (`profile.u1/u2`, `anniversary`, `myBday`, `ptBday`) + the phone's Session |
| **Partner Connection** | Status; send request by phone; waiting card + **Cancel Request**; invitations list with **Accept / Decline**; **Remove partner** (two questions); page checks the server every 5 s while open | Server (`/api/partner/*`, `/api/auth/unpair`) |
| **Notifications** | Push switch (asks the phone's permission with a short explanation first), Quiet hours From/To | Permission = the phone; hours = DataStore |
| **Appearance** | Theme (Dark; Light/System are shown but not selectable yet), **Reduce motion** | DataStore; applied to every animation in the app |
| **Chat & Media** | Auto-save photos, Voice message playback | DataStore |
| **Twin AI** | Use relationship context | DataStore |
| **Privacy & Security** | Lock app on background, **Change vault PIN** (current + new, 3 weak-PIN rules, result line) | Lock = DataStore; PIN = server |
| **Storage & Data** | Usage ring + Used/Available/Total, 5 category bars, **Clear Cache**, **Export Photos** (to a folder), **Full Backup**, **Export backup**, **Import backup** (asks first), **Cloud sync / Sync now**, "Last backup" | Files you choose (system file picker, no storage permission) |
| **App Info** | Version, Customer Care row, Privacy policy (opens browser) | — |
| **Log Out** | Asks, clears the phone's Session, back to Get Started. Cloud data is untouched | — |

## API calls used (R-65)
`GET/POST /api/data/state` · `GET /api/profile/:id`, `GET /api/profile/:id/partner`, `POST /api/profile/:id/avatar` (multipart `avatar` + `requestingUserId`) · `POST /api/auth/change-pin`, `POST /api/auth/unpair` · `GET /api/partner/status/:id`, `GET /api/partner/pending-list/:id`, `POST /api/partner/request|accept|reject|cancel`. No Supabase calls from the app (R-60). No backend change needed.

## Rules and edge cases
- Weak PIN is refused on the phone with the web's three messages (not 4 digits / repeated / sequence); the server still has the final say.
- Blank names save as "You" / "Partner" (web behaviour).
- Saving the profile sends only the keys the web sends. The server keeps the partner's `profile` entry untouched.
- A refresh (Sync, 5-second check) that fails never replaces content already on screen with an error.
- Remove partner: after success a **user2** account is signed out (same as the web); a user1 account stays.
- Import backup: the file must be a JSON object; keys that say *who you are* (`userId`, `coupleId`, `role`, `myName`, `vaultPin` …) are never sent. A confirmation question comes first because the restored data is shared with the partner.
- Notifications: if the person says no (or turned it off in the phone), the row says "Blocked — enable in phone settings" and a tap opens the phone's notification settings.

## Decided NOT to port (with reason)
1. **FCM token registration / real push delivery** — needs the Firebase project and `google-services.json`, which only you can create (README §13). The Notifications page is ready; this is the one open part of tracker row 2.5.
2. **Light and System theme** — README R-54: light theme is out of scope until a task says so. The two options are visible but disabled, with a one-line note.
3. **Lock app on background does not lock yet.** In the web app this switch only saves a value; nothing ever locks. A real lock needs a lock screen in `core/` and a PIN set up in the Secret Vault (Phase 4). The switch is saved (`AppPrefKeys.APP_LOCK`) so that feature can use it.
4. **Auto-save photos, Voice playback, Use relationship context** — in the web app these only save a value too. Saved now under `AppPrefKeys`; Chat and Twin AI must read them in their own sessions (not edited here, R-30).
5. **Switches are per phone.** On the web they lived in the *shared* couple state, so your partner's change could overwrite yours, and the server never read them. Here they are stored on each phone (DataStore).
6. **Live name preview while typing** (`updateNamesLive`) — names update after Save.
7. **Customer Care & Help** — page is Phase 4; its two rows show the standard "built in a later step" note.
8. "Share your new code" in the *Partner removed* message was old wording (codes no longer exist); it now says "you can pair again anytime".
9. Web "Export Photos" downloaded one by one; here you choose a folder once and all pictures are saved there.

## Files
`feature/settings/` — `SettingsRoute.kt`; `ui/` (screen, ViewModel, state, home, pages, previews, `components/`); `domain/` (models, rules); `data/` (repository, prefs, API, DTOs, mapper, file helpers); `di/`.
Core additions (see SHARED_CHANGES.md): `core/data/AppPrefKeys.kt` (new), small edits in `ReduceMotion.kt`, `AppViewModel.kt`, `TwinApp.kt`, `FeatureGraphs.kt`, manifest.
Resources: `res/values/strings_settings.xml`, `res/values/colors_settings.xml`.
Tests: `SettingsRulesTest`, `SettingsMapperTest`, `SettingsViewModelTest`.
