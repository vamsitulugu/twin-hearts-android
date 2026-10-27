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
| BOTH on? | `GET /api/ai-both/flag/:coupleId` | – | `{enabled}` |
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
- BOTH checks the server flag; the tab is hidden if the flag is off or unreachable (assumption, the web gives no visible text for it).
- BOTH live updates: the web uses a realtime channel with polling fallback. Android polls every 3 s only while the BOTH tab is on screen and the round is waiting; it stops in the background (R-66).
- Back inside a discussion goes to the list; on the list it goes Home.
- Mic uses the phone's speech dialog and sends the words to Twin. Listen uses the phone's text-to-speech.

## Not ported (and why)
- Orb travelling from hero to dock and the full-screen "voice mode": the web itself dropped the hero travel in the redesign; voice mode is replaced by the system speech dialog + Listen (no extra permission or library).
- Chip carousel auto-recentering after 2 s: a plain scrolling row is used.
- `recentMood` is sent empty (the Journal is not built yet; the server treats it as optional).
- Real names endpoint `/api/ai-both/names`: names come from the saved Session (same source the server uses).
- Search and menu buttons: global search and the side menu are not built yet; they show the "built in a later step" note, like Chat's call buttons.
- Sync: re-reads your name and the BOTH flag and reloads the history (there is no shared app-state blob in this app).
- Chat text is not saved between app launches (the web page does not save it either).
- Voice input inside BOTH answers (the web has a mic there): not ported; typing only.
