# Twin AI — v6 (all remaining features)

Built on top of the repo at commit 5a80a0f.

| Feature | What it does | Needs from you |
|---|---|---|
| Pro gate for BOTH | Tapping BOTH checks `/api/pro/status`. Not Pro: "Go Pro" sheet (Monthly ₹99 / Lifetime ₹499) > pay with any UPI app > "I've Paid" form + screenshot > `/api/pro/upgrade-request`. Pro cached once confirmed; Sync re-checks. | Nothing |
| Live BOTH updates | WebSocket to Supabase Realtime, same `both_round:<coupleId>` channel as the web. 3-second check stays as fallback. | Paste Supabase URL + **anon** key in `data/realtime/RealtimeConfig.kt` |
| Journal mood | Reads newest journal mood from `/api/data/state/:coupleId` (cached, 4 s limit) and sends it as `recentMood` | Nothing |
| Sync button | Reloads name, forgets cached mood and Pro status, reloads BOTH list | Nothing |
| Orb flight | First question: small orb flies from the robot to the input-bar orb (skipped when animations are off) | Nothing |
| Voice permission lookups | `<queries>` in the manifest so speech recognition / voice work on Android 11+ | Included |

Also replaced the stray `BothViewModelTest.kt` (it referenced a `BothPeople` class that does not exist in the code).
