# Feature 1.3 — Partner pairing (audit + explanation)

Sources read: `routes/partner.js` (all 6 routes) and `public/index.html` (`sendPartnerRequest`, `showPartnerRequestPopup`, `acceptPartnerRequest`, `declinePartnerRequest`, `loadPendingRequestsList`, `updatePairStatus`, `fmtSentAgo`).

## What the web does
Pairing has no page of its own on the web: a phone form sits in Settings, a popup shows an incoming "Connection Request", Settings lists "Partner Requests" and a "waiting" card with "Cancel Request". The app polls every 5 s and also listens for a realtime signal.
Here it is one screen (`Routes.Pairing`) that shows the right card for the current state.

## States of the screen
| Server status | Card shown |
|---|---|
| `not_connected` / `invitation_received` | phone box + "Send Connection Request ❤️", then "Partner Requests" list (each with Accept ❤️ / Decline) and "Skip for now" |
| `waiting` | "Waiting for Partner ❤️", who it was sent to, "Sent N mins ago", "Cancel Request", "Skip for now" |
| `connected` | "Connected ❤️", "You and NAME are linked.", "Continue" |
Plus: loading skeleton, error state with "Try again" (first load only).

## API calls (routes/partner.js, mounted at /api/partner)
| Action | Call | Answer | Errors (shown as the server wrote them) |
|---|---|---|---|
| Status | `GET /status/:userId` | `{status, partnerName?, requestId?, receiverName?, receiverPhone?, createdAt?, senderName?}` | 404 Account not found |
| Incoming list | `GET /pending-list/:userId` | `{requests:[{id, senderName, senderPhone, senderAvatar, createdAt}]}` | 500 |
| Send | `POST /request {userId, partnerPhone}` | `{ok, requestId}` | 400 cannot connect with yourself · 404 no account with this phone · 409 already connected / pending request |
| Accept | `POST /accept {requestId, userId}` | `{ok, coupleId, role, partnerName}` | 403/404/409 (not yours, gone, already connected) |
| Decline | `POST /reject {requestId, userId}` | `{ok}` | 403/404/409 |
| Cancel | `POST /cancel {requestId, userId}` | `{ok}` | 403/404/409 |

## Session changes
- Accept: saves `coupleId`, `role`, `partnerName`, `paired = true` in the Session.
- If my partner accepts while I wait, the next status check saves `paired = true` and the partner's name.

## Behaviour notes
- Checks every 5 s while the screen is visible (stops in the background). No realtime channel yet; that comes with Chat (2.2).
- After **Sign Up** the app now opens this screen (a new account has no partner). **Skip for now** or **Continue** opens Home. Sign In still opens Home directly. The only change outside this feature is one line in `AuthRoute.kt` (included).
- The sender's avatar shows the photo when the server has one, otherwise the first letter.

## Not ported
- Push notification "New Partner Request" (needs Firebase, Phase 2.5).
- "Remove partner / unpair" (belongs to Settings 2.5).
- Realtime broadcast (`partner_requests:<id>`): replaced by the 5 s check for now.

## New wording (not in the web)
"Connect with your partner" (title), "Skip for now", "Invitation sent to NAME", "You and NAME are linked." Everything else is copied.

## Tests
`ElapsedTimeTest`, `PairingMapperTest`, `PairingViewModelTest` (15 tests).
