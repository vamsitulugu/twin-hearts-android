# Twin AI (2.4) — audit and explanation

Reference: `docs/reference/images.pdf` page 9 (the Twin page). Web source: `public/index.html` (`#page-ai`), `twin-art.js`, `both-mode.js`, `routes/ai.js`, `routes/ai-both.js`.

## Screens and parts
| Part | Web | Android |
|---|---|---|
| Header: back, logo, menu, "Twin / Your Personal AI", search, Sync | yes | `TwinAiHeader` (private copy; see Change Request 5) |
| YOU / BOTH switch | yes | `TwinModeTabs`, chosen side burgundy |
| Hero "Hey <name>, / I'm Twin AI." + robot | yes | `TwinHero` + `TwinRobot` (drawn in code) |
| 5 chips; same 5 prompts in the "+" sheet | yes | `PromptChips`, `TwinAiDock` |
| Dock: +, input "Ask Twin anything...", mic, send, orb | yes | `TwinAiDock`, `TwinOrb` |
| Bubbles: my (tick + time), Twin (avatar, markdown, Copy/Regenerate/Listen, time) | yes | `AiMessages.kt`, `AiMarkdownText` |
| Thinking dots, error bubble + Retry | yes | yes |
| BOTH: Start Together, How It Works?, Recent discussions, open discussion, submit, edit, remind, analyzing, result, Continue Together, failed + Retry, safety card, delete | yes | `BothContent`, `BothViewModel` |

## API calls (read from the routes, R-65)
| Action | Call | Body | Answer |
|---|---|---|---|
| Ask (streaming) | `POST /api/ai/chat/stream` | `{message, myName, partnerName, anniversary, recentMood}` | SSE lines `data: {"delta":"…"}`, `data: {"error":"…"}`, `data: [DONE]` |
| Ask (fallback) | `POST /api/ai/chat` | same | `{reply}` |
| BOTH allowed? (Pro) | `GET /api/pro/status?coupleId=` | – | `{isPro, plan, expiresAt}` |
| History | `GET /api/ai-both/sessions/:coupleId?limit=20` | – | list of sessions (`id,title,last_activity_at,round_count`) |
| Start | `POST /api/ai-both/sessions` | `{coupleId, role}` | `{session, round}` |
| Open | `GET /api/ai-both/session/:id?role=` | – | `{session, rounds[]}` (partner text hidden until done) |
| Poll round | `GET /api/ai-both/rounds/:id?role=` | – | round (status, flags, result) |
| Submit | `POST /api/ai-both/rounds/:id/submit` | `{coupleId, role, content}` | `{status: waiting/analyzing}`; 409 if locked |
| Edit | `POST /api/ai-both/rounds/:id/edit` | same | `{status:"edited"}`; 409 "Too late to edit" |
| Remind | `POST /api/ai-both/rounds/:id/nudge` | `{coupleId, role}` | 5-minute cooldown on server |
| Next round | `POST /api/ai-both/sessions/:id/next-round` | – | `{round}` |
| Retry | `POST /api/ai-both/rounds/:id/retry` | `{coupleId}` | `{status:"analyzing"}` |
| Delete | `DELETE /api/ai-both/session/:id` | `{coupleId}` | `{deleted:true}` |

Result shape: `{intent, sections:[{title,content}], safety_flag}`. Round status: `pending, analyzing, done, safety, failed`.

## Behaviour
- Answer streams into the bubble. If the stream gives nothing, the app asks once for the whole answer (same as web). If text arrived and then the link broke, the bubble is marked failed with Retry.
- Regenerate / Retry ask the same question again in a new bubble (no second "me" bubble), as on the web.
- Max 120 bubbles kept.
- **Fixed in update 2:** the web always shows the YOU/BOTH switch (`both-mode.js` says the feature flag is no longer used). The first version hid BOTH behind the `ai_both_mode` flag, which was wrong. BOTH is now always visible; tapping it checks Pro, exactly like the web (`ProCard.requirePro('AI Both Mode')`). Not Pro: the tab stays on YOU and a Pro card shows.
- BOTH live updates: the web uses a realtime channel with polling fallback. Android polls every 3 s only while the BOTH tab is on screen and the round is waiting; it stops in the background (R-66).
- Back inside a discussion goes to the list; on the list it goes Home.
- Mic uses the phone's speech dialog and sends the words to Twin. Listen uses the phone's text-to-speech.

## Added in update 2 (found by comparing with `index.html`, `both-mode.js`, `twin-art.js`)
- **Voice mode (full screen)** `TwinVoiceController` + `TwinVoiceOverlay`: mic opens a screen with the big orb, "Listening... / Thinking... / Speaking...", live words, mute and end buttons. Spoken question goes through the same Twin chat; the answer is read aloud; then the screen closes itself. Tap the mic while Twin speaks to interrupt. Silence, no-speech, mic refused and failed answers all show a message and close after about 1.2 to 1.6 s. Back, the end button and leaving the app all close it. The orb grows with your voice (speech recognizer loudness).
- **Orb states:** idle, thinking, responding, error, listening, speaking (`OrbState`), drawn on the dock orb and the voice screen.
- **BOTH always visible + Pro check**, **subtitle "For Both of Us"** in BOTH, back on BOTH home goes to YOU.
- **BOTH history cards:** intent label, date, title, round count, and the vertical-dots menu with "Delete Discussion". Delete card now has the web's full warning text and a "Deleting…" state.
- **Discussion screen:** "Back", round number, you / Twin / partner row, and earlier finished rounds folded into "Round N - intent" rows you tap to open.
- **Mic inside BOTH answer box** (one-shot dictation added to your text, like the web).
- **Remind:** shows "Nudged <name>" or "<name> doesn't have notifications enabled yet", and shows the server's own text for the 5-minute wait.
- **Edit answer:** if the partner submitted first (server says locked), the screen refreshes to the new state instead of staying stuck.
- **Chip row** rests on "Write something sweet" and glides back to it after 2 s without touch.

## Not ported (and why)
- Orb travelling from hero to dock: the web itself dropped the hero travel in the redesign.
- Live updates in BOTH: the web uses a realtime channel; Android asks the server every 3 s only while the BOTH tab is on screen (R-66).
- Search and menu buttons: global search and the side menu are not built yet; they show the "built in a later step" note.
- Sync: re-reads your name and reloads the BOTH list (there is no shared app-state blob in this app).
- Chat text is not saved between app launches (the web page does not save it either).
- The Pro upgrade card (UPI payment screen) is its own feature; here a simple "AI Both Mode is a Pro feature" card shows.
- Voice language follows the phone language (the web fixes en-US).

## Added in update 3 (web parity pass)
- `recentMood`: read from `GET /api/data/state/:coupleId` (`journal[0].mood`), cached 60 s.
- BOTH names and photos: saved names first, then `GET /api/ai-both/names/:coupleId`; photos from the same state (`profile.<role>.avatar`, http links only).
- Open discussion is remembered through `SavedStateHandle` (survives the app being killed); "Opening discussion…" card while it loads.
- Offline orb (grey) from a connectivity listener (`TwinAiConnectivity`).
- Markdown links `[text](https://…)`; blinking stream caret; bubble pop-in; send button ready / sending states; tap outside closes the "+" sheet; safety-round formatting in folded earlier rounds; Start Together chevron.
- Hero robot redrawn with the web shapes/gradients and a pair-of-robots hero for BOTH (`TwinRobotPair`).
- Solid `#6B093A` (theme `brand`) for hero title, tabs, send and BOTH buttons (`TwinBrandButton`).
- Bug fixes: round number kept after a 409 on Edit; "analyzing" instead of "waiting" when you submit second; Remind unlocks after 2 s; BOTH list no longer stuck on "Loading…" when signed out.
- Bottom bar hidden on the Twin page; manifest `<queries>` applied.
