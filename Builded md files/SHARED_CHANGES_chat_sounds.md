# SHARED_CHANGES — chat + call sound effects

Requested directly by the user ("add sounds for chat and calls on web and Android"), so this deliberately
touches `core/` and `feature/calls` (README R-30 / R-31 normally forbid that).

## Files changed
| File | Change |
|---|---|
| `core/sound/TwinSound.kt` | +16 synthesized cues (`msgSent`, `msgReceived`, `mediaSent`, `funSent`, `voiceStart/Stop/Cancel`, `react`, `heart`, `star`, `pin`, `deleted`, `callConnected`, `callEnded`, `switchOn/Off`) and a 120 ms same-cue throttle (`cue()`). No new library, no audio files. |
| `feature/chat/ui/ChatViewModel.kt` | send / failed / receive / reaction / star / pin / delete sounds. Muted chat and the do-not-disturb window silence incoming sounds. |
| `feature/chat/ui/ChatScreen.kt` | voice-note start, stop and cancel sounds. |
| `feature/calls/data/CallEngine.kt` | connected, ended, failed, and mic / speaker / camera toggle sounds. |

## Sound map (identical to web `twin-sounds.js` v2)
| Action | Sound |
|---|---|
| Send text / voice note | soft rising pop / recording-stopped blip |
| Receive message | two-note bloop (silent if chat muted or in DND) |
| Photo / video / file | swish + pop |
| Sticker / GIF | playful two-note boing |
| Gift, hug / kiss sticker | warm heart chime |
| Reaction added (mine or partner's) | bright sparkle |
| Star / Pin / Delete | two-note ding / soft tock / downward whoosh |
| Voice start / cancel | rising blip / falling bloop |
| Send failed | gentle "not now" thud |
| Call connected / ended | 3 rising / 3 falling notes |
| Mic, speaker, camera on / off | tiny up / down blip |

All sounds obey the existing single mute switch (`TwinSound.setEnabled`).
No Gradle, manifest or DB changes.
