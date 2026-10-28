# Twin Hearts — Native Android Rebuild: Master Rules

> **Every Claude session MUST read this whole file before doing anything.**
> If this file and a chat message disagree, **this file wins** unless the user says "override README rule R-xx".
> Rules have IDs (R-01, R-02 …). Quote the ID when you explain a decision.

---

## 0. How the user starts a session

The user pastes this into a new Claude chat and attaches this README (+ the files listed in §13):

```
Read README.md fully and follow it exactly.
TASK: <one feature or screen, e.g. "Phase 2 — Chat (messages, reply, voice note, call buttons)">
```

One session = **one feature**. Never build a second feature "while you're there".

---

## 1. Mission

Rebuild **Twin Hearts** (a two-person couple app) as a **real native Android app**, from scratch, clean code, step by step, like a production project.

- **Same product:** every feature, colour, card, button, label and name matches the current web app.
- **New code:** nothing is copied from the web files. The old repo is a **reference to read**, never a source to paste.
- **Zero cost:** no paid service, library, or plan (see §3).
- **Many Claude sessions** will build different parts. They must all produce code that fits together without the user fixing conflicts. That is why these rules are strict.

Old web app (read-only reference): `https://github.com/vamsitulugu/us-app.git`
(`public/*.html` = screens, `routes/*.js` = API, `migrations/*.sql` = tables, `public/*.css` = styling).

---

## 2. Fixed decisions (do not change; do not "improve")

| ID | Decision | Value |
|---|---|---|
| R-01 | Language | **Kotlin** only. No Java files, no Flutter, no React Native, no WebView screens. |
| R-02 | UI | **Jetpack Compose + Material 3**. No XML layouts (XML only for manifest/resources). |
| R-03 | Architecture | Single `app` module, **feature packages**, MVVM + Repository (see §5). |
| R-04 | DI | **Hilt**. |
| R-05 | Navigation | **Navigation Compose**, type-safe routes (`@Serializable`), single Activity. |
| R-06 | Networking | **Retrofit + OkHttp + kotlinx.serialization**. |
| R-07 | Local data | **Room** (structured/offline data), **DataStore** (settings). |
| R-08 | Images | **Coil**. Video/audio: **Media3**. Camera: **CameraX**. |
| R-09 | Async | Coroutines + Flow. No RxJava, no LiveData, no AsyncTask. |
| R-10 | Push | **Firebase Cloud Messaging** (free). |
| R-11 | Maps | **MapLibre Native** + free OpenStreetMap-based tiles. No Google Maps (needs billing). |
| R-12 | Package name | `com.twinhearts.app` (user may edit this ONE value before Phase 0 starts; then it is frozen). |
| R-13 | SDK | `minSdk 26`. `compileSdk`/`targetSdk` = current stable, set once in Phase 0 in the version catalog. |
| R-14 | Build files | Gradle **Kotlin DSL** + **version catalog** (`gradle/libs.versions.toml`). |
| R-15 | Versions | Only **Phase 0** sets versions. Later sessions **read** the catalog. Never invent or guess a version number; if you need a new dependency, add one catalog line and list it in `SHARED_CHANGES.md`. Look up the current stable version before adding. |

Want to change any of these? Do **not** change them silently. Write a **Change Request** (§11).

---

## 3. Zero-cost rules

- **R-20** Only free tiers: Android Studio, GitHub (free), Firebase Spark (FCM, Crashlytics), Supabase free, Render free, TMDB free key, Groq free tier, OpenStreetMap-based tiles.
- **R-21** Never add a library that needs a paid licence or billing account.
- **R-22** Google Play ($25 one-time) is **out of scope** for now. Distribution = signed APK shared directly / GitHub Releases.
- **R-23** If something truly needs money, **stop and tell the user** before writing code for it.
- **R-24** Known free-tier limits to design for: Render free **sleeps** (first request is slow → show proper loading states + retry, never a blank screen); Supabase free **pauses** after inactivity.

---

## 4. Repo layout (new repo)

```
twin-hearts-android/
├─ README.md                      ← this file (kept up to date)
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/twinhearts/app/
│     │  ├─ TwinHeartsApp.kt      (Application, @HiltAndroidApp)
│     │  ├─ MainActivity.kt       (single Activity)
│     │  ├─ core/
│     │  │  ├─ design/            theme + reusable components (§6)
│     │  │  ├─ network/           Retrofit, interceptors, session, error mapping
│     │  │  ├─ data/              TwinDatabase, DataStore, common models
│     │  │  ├─ navigation/        AppNavHost, bottom bar, route registry
│     │  │  └─ common/            Result/UiState, extensions, formatters
│     │  └─ feature/
│     │     └─ <name>/            ONE folder per feature (§5)
│     └─ res/                     fonts, drawables, strings.xml, themes
├─ gradle/libs.versions.toml
├─ docs/
│  ├─ reference/                  user's screenshots of the web app (read-only)
│  └─ features/<name>.md          one audit/explanation file per delivered feature
└─ backend-changes/               SQL / server changes a feature needs (if any)
```

- **R-30** A feature session may **only create/modify files inside its own `feature/<name>/` folder**, its own `docs/features/<name>.md`, its own `res` files (prefixed with the feature name), and files listed in `SHARED_CHANGES.md`.
- **R-31** Never edit `core/` yourself unless the task says "Phase 0" or a Change Request was approved. Use what exists. If a component you need is missing, build it **inside your feature** as `private` and list a Change Request to promote it.

---

## 5. Architecture inside every feature

```
feature/chat/
├─ ChatRoute.kt              @Serializable route + NavGraphBuilder.chatGraph()
├─ ui/
│  ├─ ChatScreen.kt          stateless composable (takes UiState + callbacks)
│  ├─ ChatViewModel.kt       @HiltViewModel, exposes StateFlow<ChatUiState>
│  ├─ ChatUiState.kt         one sealed/data class: Loading | Content | Empty | Error
│  └─ components/            small composables used only here
├─ domain/
│  ├─ model/                 plain Kotlin data classes (what the UI uses)
│  └─ usecase/               ONLY when logic is non-trivial; otherwise skip
├─ data/
│  ├─ remote/ChatApi.kt      Retrofit interface + DTOs (@Serializable)
│  ├─ local/                 Room entity/DAO (only if offline is needed)
│  ├─ mapper/                DTO → domain, entity → domain
│  └─ ChatRepository.kt      interface + ChatRepositoryImpl
└─ di/ChatModule.kt          Hilt @Module binding repository + api
```

Rules:
- **R-40** UI never talks to Retrofit/Room. Flow: `Screen → ViewModel → Repository → (Api | Dao)`.
- **R-41** Every screen exposes **all four states**: loading, content, empty, error (with retry). The empty and error texts must match the web app's wording where it has any.
- **R-42** Composables are **stateless**; state lives in the ViewModel. One-off events (toast, navigate) use a `SharedFlow`/channel, not state.
- **R-43** Each feature registers itself through **one** function `NavGraphBuilder.<name>Graph(navController)`. The session tells the user the single line to add to `AppNavHost` (in `SHARED_CHANGES.md`).
- **R-44** Hilt modules live in the feature's `di/` folder and are auto-discovered. Do not edit shared modules.
- **R-45** Room: entities are registered in `TwinDatabase` by the user via `SHARED_CHANGES.md`. Bump the DB version and provide a migration. Prefer **no Room** unless offline use is real.
- **R-46** Errors: map exceptions to a small sealed `AppError` (network, unauthorized, server, unknown) in the repository. Never show raw exception text to the user.
- **R-47** No `!!`, no `GlobalScope`, no `runBlocking` on UI paths, no blocking calls on the main thread.
- **R-48** Names: `PascalCase` classes, `camelCase` functions, `SCREAMING_SNAKE` constants, files named after the main class. Package names lowercase.

---

## 6. Design system (the look must match the web app)

Reference screenshots are in `docs/reference/`. **Always open the matching screenshot before building a screen** and match layout, spacing, order, icons and wording.

**Look in one sentence:** deep wine/burgundy dark theme, frosted "glass" cards with soft borders, burgundy primary buttons, serif titles + sans body, pill-shaped controls, a floating bottom bar with a centre "Twin" orb.

### Tokens (from the web CSS — Phase 0 re-verifies them against `public/*.css` and `docs/reference/`)

| Token | Value | Used for |
|---|---|---|
| `background` | `#14030E` (with subtle burgundy radial gradient at top) | screen background |
| `card` | `#400E2A` / white @ 7% glass | cards |
| `accent` | `#C21A8B` | highlights, links |
| `primary` | gradient `#B0127A → #8A0E5F` | primary buttons |
| `brand` | `#6B093A` | status/app bars, sent chat bubbles |
| `ink` | `#FEF4FA` | main text |
| `inkMuted` | white @ 52–74% | secondary text |
| `line` | `rgba(194,26,139,.20)` / white @ 13% | borders |
| `neutralGlass` | `#8A8A92` | active segmented tab / secondary pill buttons |

- **R-50** **Never hard-code a colour, size, or font** in a feature. Use `TwinTheme` tokens. If a value is missing → Change Request.
- **R-51** Fonts: **Inter** (body/UI) and **DM Serif Display** (titles, big numbers, italic accent word like *together*). Both free (OFL). Bundled in `res/font/`.
- **R-52** Shared components Phase 0 must provide (features **must reuse**, not rebuild):
  `TwinBackground`, `GlassCard`, `GlassButton` (primary / secondary / ghost / icon), `TwinTopBar` (logo, menu, title, search, **Sync**), `TwinBottomBar` (Home · Chat · **Twin orb** · Map · Settings), `SegmentedTabs`, `FilterChip`, `StatTile`, `SectionHeader` ("Us", "Explore Together" …), `EmptyState`, `ErrorState`, `LoadingSkeleton`, `Avatar`/`CoupleAvatars`, `PillSwitch`, `TwinTextField`.
- **R-53** **Wording is sacred.** Copy labels exactly as in the web app/screenshots ("Stay Connected", "Miss You", "Log a Fight", "Plan New" …). All user-visible text goes in `strings.xml`, named `<feature>_<what>`.
- **R-54** Light theme exists in the web CSS but is **not** in scope until a task says so. Build dark first, structure tokens so light can be added.
- **R-55** Animation: use Compose animation APIs, animate only `alpha`/`scale`/`translation` (via `graphicsLayer`) where possible. Respect the system "remove animations" setting. **Avoid `Modifier.blur` and big shadows on scrolling content** — fake glass with translucent fills, gradients and 1 dp borders instead. This is the main reason the web app felt slow.
- **R-56** Layout must work from a 360 dp phone up to a tablet: no fixed pixel widths, use `fillMaxWidth`, weights, and `WindowSizeClass` where it matters. Respect system bars/insets (`enableEdgeToEdge`, `WindowInsets`).
- **R-57** Every screen has `@Preview` functions for each state (light data, empty, error) and a large-font preview (fontScale 1.3).

---

## 7. Backend rules (read carefully)

What the old backend is (verified from the repo):
- Node/Express on Render, Supabase Postgres, **~35 `/api/*` route files** (auth, chat, call, music, lyrics, movie, meetplanner, daily-routine, location, route, tracking, globe, profile, partner, ai, ai-both, media, recordings, support, flags, releases, pro …). Admin routes (`/api/admin/*`) are **web-only — out of scope**.
- **Login is custom**: email + password, bcrypt-hashed **on the server**, using the Supabase **service key**. It is NOT Supabase Auth. Rate limits: 20 auth attempts / 15 min / IP, 60 API calls / min / IP.
- Push is sent by the server with `firebase-admin` (tables `fcm_tokens`, `push_subscriptions`).
- Server holds secrets: Supabase service key, Groq (AI), TMDB, Firebase service account, Metered (calls). 

Rules:
- **R-60** The Android app talks **only to the Express API**. **Never call Supabase directly** from the app and never ship the service key (an app can be decompiled; the service key bypasses all security).
- **R-61** **No secret of any kind** inside the app: no API keys except the public Firebase config (`google-services.json`) and the API base URL (set in `BuildConfig` from `local.properties`, not committed).
- **R-62** Default plan: **reuse the existing Supabase + Render** (accounts and data carry over, zero setup). Create new ones **only if the user says so**.
- **R-63** If the server needs a change (token auth instead of cookies, a new endpoint, a new table), do **not** edit the web repo. Put the change in `backend-changes/<feature>/` as: SQL file, patch snippet, and 5-line explanation. Mark it in `SHARED_CHANGES.md` as **REQUIRES BACKEND CHANGE**.
- **R-64** Session/token handling is implemented **once** in `core/network` (Phase 1 auth session decides cookie-jar vs token after reading `server.js`/`routes/auth.js`). Features only inject their `Api` interface and never touch auth headers.
- **R-65** Every feature's audit lists, for each screen action: HTTP method + path, request body, response shape, error cases — **read from the old `routes/*.js`**, not guessed. If the code is unclear, say what you couldn't verify.
- **R-66** Realtime (chat, presence, live location): the old repo shows no websocket server. Read how the web app does it (polling/SSE) and document it; implement the same mechanism in a lifecycle-aware way (stop when app is in background, use FCM to wake).

---

## 8. The workflow every session follows (in this order)

1. **Read** this README. Clone the old repo for reference. Open the attached core files and the matching `docs/reference/` screenshots.
2. **Restate the task** in 3 lines (feature, screens, what is out of scope).
3. **Feature Audit** — read the old HTML section, JS, CSS, and `routes/*.js` for this feature. Write `docs/features/<name>.md` with:
   - every screen, state, button, input, dialog, setting, animation;
   - every API call (R-65) and table used;
   - behaviours/edge cases (empty, offline, permissions, validation);
   - things from the web app you decided **not** to port, with a reason.
4. **Ask questions only if blocked** — one message, max 3 questions, each with a recommended default. Otherwise continue.
5. **Write the code** following §5–§6. Rewrite the behaviour cleanly; never port line by line.
6. **Self-review** against the Definition of Done (§9). Fix before delivering.
7. **Package** the zip (§10) and present it.
8. **Reply to the user** in short, simple English: what was built, how to apply the zip, how to test, what to send back if something fails.

You **cannot compile Android code in the Claude sandbox** (no Android SDK / Gradle repositories). Therefore: write conservative, standard APIs; double-check imports, nullability and Hilt wiring by reading your own code; never claim "it builds" — say "not compiled here; build in Android Studio". If the user pastes a build error, fix it in the same session and re-deliver only the changed files.

---

## 9. Definition of Done (checklist before delivering)

- [ ] Every item in the feature audit is implemented or explicitly listed as "not ported" with a reason.
- [ ] UI matches the reference screenshot: order, labels, icons, spacing, colours (via tokens).
- [ ] Loading / content / empty / error states all exist and have previews.
- [ ] No hard-coded colours, strings, or dimensions outside theme/resources.
- [ ] No secrets, no `Log` of personal data, no `!!`, no unused imports/dead code/TODOs.
- [ ] Network calls only through the feature's `Api`; errors mapped (R-46).
- [ ] Works in portrait on small phones and large font; handles rotation/process death (state survives via `SavedStateHandle`/`rememberSaveable` where relevant).
- [ ] Permissions requested **only when needed**, with a rationale screen; manifest entries listed in `SHARED_CHANGES.md`.
- [ ] Unit tests for ViewModel + mappers (JUnit, MockK, Turbine, coroutines-test).
- [ ] Every file has a short top comment explaining **why it exists**; non-obvious code has "why" comments (not "what").
- [ ] `docs/features/<name>.md`, `SHARED_CHANGES.md`, `BUILD_NOTES.md` included.
- [ ] README tracker (§12) updated.

---

## 10. Delivery format (one zip per session)

File name: `th-<phase>-<feature>-v<N>.zip`, e.g. `th-p2-chat-v1.zip`.

```
th-p2-chat-v1.zip
├─ app/src/main/java/com/twinhearts/app/feature/chat/...   (new files only)
├─ app/src/main/res/...                                     (prefixed: chat_*)
├─ docs/features/chat.md                                    (audit + explanation)
├─ backend-changes/chat/...                                 (only if needed)
├─ SHARED_CHANGES.md                                        (exact lines the user must add elsewhere)
├─ BUILD_NOTES.md                                           (how to build/test/verify, known gaps)
└─ README.md                                                (tracker row updated)
```

- **R-70** Paths inside the zip are **project-relative**, so the user can unzip straight over the project root.
- **R-71** The zip contains **only new or changed files**. Never include `build/`, `.gradle/`, `.idea/`, `local.properties`, keystores, or `google-services.json`.
- **R-72** `SHARED_CHANGES.md` has fixed sections: **Gradle/catalog lines**, **Manifest entries**, **AppNavHost line**, **TwinDatabase entities**, **Strings/permissions**, **Backend changes**. Write "None" when empty. Each item is a copy-paste snippet with the **file path** it goes in.
- **R-73** `BUILD_NOTES.md` = Windows-friendly steps: sync, run, what screen to open, what to tap, expected result, and 3–5 manual test cases.
- **R-74** Create the zip with `bash`, save it in `/mnt/user-data/outputs/`, and share it with `present_files`.
- **R-75** Explanations are written for a learner: plain English, short sentences, no jargon without a one-line meaning.

---

## 11. Collaboration rules (many Claudes, no conflicts)

- **R-80** Only **Phase 0** touches `core/`. After Phase 0, `core/` is frozen except via Change Requests.
- **R-81** **Change Request** = a section in `SHARED_CHANGES.md` titled `CHANGE REQUEST: <what>` with: why, exact proposed code, which features are affected. The user decides; the next session applies it.
- **R-82** Sessions can run **in parallel only if their features are independent** (the build order in §12 marks dependencies). A session never assumes another feature exists, unless the tracker says **Done**.
- **R-83** To open another feature, navigate through a route object defined in `core/navigation` (`Routes.kt`) — never import another feature's classes.
- **R-84** Do not rename or move existing files from other sessions.
- **R-85** If this README is wrong or incomplete, do **not** work around it silently: say what is wrong and propose the exact fix text.

---

## 12. Build order & progress tracker

Status: ⬜ not started · 🟨 in progress · ✅ done (user confirmed it builds and works).
**Depends on** = must be ✅ first.

### Phase 0 — Foundation (one session, done first)
| # | Item | Depends on | Status |
|---|---|---|---|
| 0.1 | Project setup, version catalog, Hilt, manifest, build types (R8 on release, debug tools off in release) | — | ✅ |
| 0.2 | `core/design`: theme, fonts, tokens, all components in R-52 + a "design gallery" debug screen | 0.1 | ✅ |
| 0.3 | `core/network`: Retrofit/OkHttp, error mapping, base URL via BuildConfig | 0.1 | ✅ |
| 0.4 | `core/navigation`: AppNavHost, Routes, bottom bar with Twin orb | 0.2 | ✅ |
| 0.5 | `core/data`: DataStore, empty `TwinDatabase` | 0.1 | ✅ |

### Phase 1 — Entry
| # | Feature | Depends on | Status |
|---|---|---|---|
| 1.1 | Get Started / onboarding (from `landing.html`) | 0 | ✅ |
| 1.2 | Sign up, Sign in, session storage (decides cookie vs token, R-64) | 1.1 | ✅ |
| 1.3 | Partner connection / couple pairing | 1.2 | ✅ |

### Phase 2 — Core
| # | Feature | Depends on | Status |
|---|---|---|---|
| 2.1 | Home (greeting, search, quick chips, Stay Connected Hug/Touch/Miss You, Coming up, Mood, hub sections, quote) | 1.3 | 🟨 |
| 2.2 | Chat (all web features except typing indicator; see docs/features/chat.md) | 1.3 | 🟨 |
| 2.3 | Calls (voice/video buttons in chat) | 2.2 | ⬜ |
| 2.4 | Twin AI (You / Both) | 1.3 | ⬜ |
| 2.5 | Settings (all sub-pages) + Notifications (FCM) | 1.3 | ⬜ |
| 2.6 | Our Profile | 1.3 | ⬜ |

### Phase 3 — Us
Love Counter · Level · Fight Log · Dream Goals · Our Memories (camera/gallery) · Collection

### Phase 4 — Plans & My Space
Calendar · Surprises · Money · Bucket List · Daily Routine · Meet Planner · Places · My Space · Period Tracker · Vault · Study · Help / Customer Care

### Phase 5 — Media & Location
Music (library, player, lyrics/karaoke, recordings) · Watch Together (movies) · Live Map (Map / Track / Partner / Places)

### Phase 6 — Heavy
Games (Ludo, Tic-Tac-Toe, Truth or Dare, Memory, Mind Reader, Puzzle, Draw, Bomb — one session each) · Memory Globe (decide native approach in its own audit) · Pro upgrade

### Phase 7 — Release
Performance pass (Baseline Profile, jank check), signing, release APK, crash reporting.

### Handoff log (each session appends one line)
| Date | Session task | Zip name | Notes / open issues |
|---|---|---|---|
| 2026-10-25 | Phase 0 — Foundation (0.1–0.5) | th-p0-foundation-v1.zip | Delivered, not compiled in sandbox. Waiting for user build confirmation. Package = com.twinhearts.app. Session = ids in DataStore (no tokens). |
| 2026-10-25 | Phase 0 fix | th-p0-foundation-v2.zip | Missing `dp` import in GlassCard.kt. User confirmed Phase 0 runs on phone (design gallery OK) → 0.x ✅. |
| 2026-10-25 | Phase 1 — 1.1 Get Started, 1.2 Sign In/Sign Up/Forgot Password | th-p1-entry-v1.zip | Not compiled in sandbox. Plug-in via FeatureGraphs.kt (included). Privacy URL to verify. User confirmed 1.1 + 1.2 working (✅). |
| 2026-10-25 | Phase 1.3 Partner pairing | th-p1-pairing-v1.zip | Not compiled in sandbox. Sign Up now opens Pairing (AuthRoute.kt change included). User confirmed pairing works (✅). |
| 2026-10-26 | Phase 2.1 Home | th-p2-home-v1.zip | Not compiled in sandbox. Stay Connected card works through shared state (/api/data/state). Events + mood widgets not built yet. |
| 2026-10-26 | Phase 2.2 Chat (messages, reply, voice, photo/camera, search, star, scheduled) | th-p2-chat.zip | Not compiled in sandbox. Continues the earlier session's 5 files. |
| 2026-10-26 | Phase 2.1 Home v3 — Card photos editor | th-p2-home-v3.zip | Not compiled in sandbox. |
| 2026-10-27 | Phase 2.2 Chat v2 — full rebuild (media, polls, reactions, pin, edit/delete, forward, wallpaper, disappearing, export, auto-lock, effects) | th-p2-chat-v2.zip | Not compiled in sandbox. Typing indicator not ported (needs backend). |
| 2026-10-26 | Phase 2.1 Home v2 — Coming up, Mood card, Mood Calendar | th-p2-home-v2.zip | Not compiled in sandbox. Search/chips/grid wait for their pages. |
| 2026-10-27 | Phase 5 — Watch Together (movies) | th-p5-movie-v1.zip | Not compiled here. Needs Media3 catalog lines + Routes.Movie (applied in zip). Cloud upload, subtitles, reactions, sleep timer, in-movie calls not ported (see docs/features/movie.md). |

---

## 13. What the user attaches to every session

1. This `README.md` (latest version).
2. `gradle/libs.versions.toml` and the `core/` folder (zipped) — after Phase 0.
3. The matching screenshots from `docs/reference/` for the feature.
4. For fixes: the exact Android Studio error text or a screenshot.

Things **only the user can do** (Claude must list them clearly, never assume they're done): create the Firebase project and download `google-services.json`; create/copy API keys into `local.properties`; create a new Supabase/Render only if the user decides to; run builds on the PC/phone.

User's environment: Windows PC, Android Studio, JDK 21 (Temurin), real Android phone with USB debugging. Give Windows paths/commands.

---

## 14. Things a Claude must NEVER do

- Copy-paste code from the old web files, or wrap web pages in a WebView.
- Put any secret key, service key, or password in the app or the zip.
- Call Supabase directly from the app (R-60).
- Change versions, package name, architecture or design tokens on its own.
- Edit `core/` or another feature's files outside a Change Request.
- Say "this builds/works" without having compiled it (you can't here).
- Add a feature that isn't in the old app, or silently drop one that is.
- Leave TODOs, placeholder text, fake data, or half-finished screens in a delivery.
- Spend the user's money or add anything that needs a card.
- Write long, jargon-heavy explanations. Short and clear wins.

---

## 15. Copy-paste prompts for the user

**Phase 0**
```
Read README.md fully and follow it exactly.
TASK: Phase 0 — Foundation (items 0.1 to 0.5). Reference screenshots attached.
```

**A feature**
```
Read README.md fully and follow it exactly.
TASK: Phase <n> — <feature name>.
Attached: README.md, core.zip, libs.versions.toml, screenshots for this feature.
```

**Fix a build error**
```
Read README.md. The last delivery (th-<...>.zip) failed to build.
Error: <paste text>. Fix it and send only the changed files as th-<...>-v2.zip.
```

| Meet Planner | v1 delivered (not compiled) | `th-p4-meetplanner-v1.zip`; see docs/features/meetplanner.md |
