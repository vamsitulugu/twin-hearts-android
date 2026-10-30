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
- BOTH tab is always shown (the web removed its flag check too). The subtitle changes to "For Both of Us" in BOTH. The last open discussion is reopened next time (like the web).
- BOTH live updates: the web uses a realtime channel with polling fallback. Android polls every 3 s only while the BOTH tab is on screen and the round is waiting; it stops in the background (R-66).
- Back inside a discussion goes to the list; on the list it goes Home.
- Mic uses the phone's speech dialog and sends the words to Twin. Listen uses the phone's text-to-speech.

## v2 additions (all earlier gaps closed)
- **Voice mode** (mic button or menu): full-screen orb; listens, sends to Twin, reads the answer, listens again. Mute, interrupt, end. Stops when the app goes to the background or Back is pressed.
- **Mic in BOTH answers**: speaks into your answer box (phone speech dialog).
- **Search button**: filters this chat's messages (YOU) or the discussion list (BOTH).
- **Menu button**: New chat, Voice mode, Quick prompts, Copy conversation.
- **Chip row**: rests with "Write something sweet" centred; springs back 2 s after a swipe.
- Hero card now has the soft burgundy glow from the reference.

## Still different from the web (on purpose)
- Orb does not travel from hero to dock (web dropped it too).
- `recentMood` sent empty (Journal not built yet).
- Names come from the saved Session, not `/api/ai-both/names`.
- Sync re-reads your name, the BOTH flag and the list.
- Chat text is not saved between launches (web does not either).
