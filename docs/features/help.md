# Help & Customer Care — audit and explanation

Reference: `docs/reference/images.pdf` page 45 (Questions tab). Web source: `public/help.html`. Server: `routes/support.js`, `lib/supportStore.js`, `migrations/support_care.sql`.

## Screens
| Part | Web | Android |
|---|---|---|
| Top bar: back, "Help & Customer Care", green dot + status text | yes | `HelpTopBar` (status: "AI & Support Active" on Questions; "Twin Care AI Active" / "Admin Care Connected" on the chat) |
| Tabs "📚 Questions & QA" / "💬 Live Support & AI Chat" | yes | `HelpPillTabs` (pink-filled active tab, as in the screenshot) |
| Hero card "Twin Hearts Care Center" | yes | `HeroCard` |
| Search box, 7 topic chips (All Topics … Sync & Offline) | yes | `SearchField`, `CategoryChips` (`TwinFilterChip`) |
| Question list (tap to open; several can be open); open card shows answer, "🤖 Ask AI for more details", "💬 Message Support" | yes | `FaqCard` |
| "No questions matched your search." + "Ask Twin Care AI about "…"" | yes | `NoMatches` (`EmptyState`) |
| Pink banner "Still have a question…" + "💬 Start Chat with Care Team →" | yes | `CareBanner` |
| Chat header: 🤖 Twin Care AI / 👤 Live Agent toggle + my avatar and name | yes | `ChatHeader` |
| Welcome card 💌 + 4 quick chips | yes | `WelcomeCard` |
| Messages: avatar, sender, "AI Response"/"Care Agent" badge, bubble, time | yes | `MessageRow` |
| Typing line with dots | yes | `TypingLine` |
| Input bar + send button | yes | `InputBar` |
| Toasts | yes | `Toast` from one-off `HelpEvent` |

States (R-41): questions = loading skeleton / list / empty / error with retry. Chat = connecting skeleton / thread / error with retry.

## API (read from `routes/support.js`)
| Action | Request | Answer |
|---|---|---|
| Load questions | `GET /api/support/faqs` | `{faqs:[{id,category,question,answer,tags}]}` |
| Open chat | `POST /api/support/ticket` `{ticketId?, userId, couple_id, userName, userPhone, mode}` | `{ticket:{id,mode,messages[]}}` (reuses the saved or my open ticket) |
| Send | `POST /api/support/ticket/:id/message` `{text, senderName, askAi}` | `{userMessage, aiMessage, ticket}`; `askAi` = AI mode |
| Switch helper | `POST /api/support/ticket/:id/switch-mode` `{mode:"ai"|"human"}` | `{ticket}` (server adds a system message) |
| New replies | `GET /api/support/ticket/:id` every 4 s | `{ticket}` |
Errors: server `{error}` text is shown through `AppError`. No tokens: ids go in the body (R-64). No backend change needed.

## Behaviour copied from the web
- Ticket id kept on the phone (web: `th_support_ticket_id`), so the chat continues next time.
- My message shows at once; the server's thread replaces it.
- "Ask AI for more details" opens the chat in AI mode and asks the question. "Message Support" switches to Live Agent and sends `I have a question about: "…"`. Banner opens Live Agent.
- Mode comes from the server ticket on open (a chat left in Live Agent returns in Live Agent).
- Wording and quick-chip sentences are the web's.

## Different from the web (and why)
- **Polling is lifecycle-aware** (R-66): only while the chat tab is open and the app is on screen.
- **Ticket id is stored in DataStore**, which sign-out clears: the next person on the phone does not see the old chat.
- Tapping the already-active mode does nothing (the web re-sent switch-mode and added a duplicate system message).
- A failed send removes the bubble and puts the text back in the box (web left it and lost the text).
- A failed mode switch moves the toggle back and shows the error.
- If the AI mode gets no AI answer, a toast shows the server's own sentence "Could not generate an AI response right now. Please switch to Live Agent chat." (the web showed nothing).
- The topic chip "Privacy & Vault" highlights correctly (web bug: it never highlighted).
- AI avatar/badge use the theme's green `success` token instead of the web's teal (no teal token; R-50).

## Not ported
- ☀️/🌙 theme toggle (light theme is out of scope, R-54).
- URL parameters `?chat=1`, `?mode=`, `?q=` (no external links in the app).
- Unused tab badge `chatBadge` (web never shows it).
- `POST /api/support/ai-ask` (the web page does not use it).
- Push when an agent replies: replies appear while the chat is open (polling). FCM push needs a backend change (later, with Notifications 2.5).
- Settings "Customer Care & Help" row: Settings (2.5) is not built yet; add `navController.navigate(Routes.Help)` there later.

## Backend note (no code change)
Run `migrations/support_care.sql` once in Supabase. Without it the server keeps chats in a local JSON file on Render, which is wiped on restart/sleep.
