# Ads (AdMob) — feature audit

Goal: earn from free couples without annoying anyone. Pro couples never see an ad (Pro already lists "No Ads").

## What the person sees
| Where | What | When |
|---|---|---|
| Home, between "All pages" and the quote | Small "Sponsored" banner card | Only after a real ad has loaded. Nothing (no gap) otherwise |
| Back on Home after using a game or planning tool | One full-screen ad | Only when every rule below passes AND an ad was already loaded |

Never shown: Chat, Calls, Live Map / navigation, Twin AI, Settings, Pro page, sign-in/sign-up/pairing, Music, Movie,
Memories, Collection, Globe, Love Counter, Fight Log, Surprises, Vault, Period Tracker, My Space.
No ad on app open, no ad while typing, no ad during a call. No "wait for the ad" spinner: if none is ready we skip.

## The rules (all in `domain/AdPolicy.kt`, numbers in `AdRules`)
- No ads for 48 hours after first install, and not before the 3rd app launch.
- Full-screen ad: not in the first 90 s of a launch; only after 45 s+ inside the feature; at least 8 min apart;
  max 3 per day; only every 2nd qualifying break.
- Pro couple, signed-out, or server unreachable → no ads at all (fail closed).
- If a couple buys Pro while the app is open, ads switch off at once (listens to ProGate).
- Content rating capped at "teen"; block more categories in the AdMob console (see BUILD_NOTES_ads.md).

## Files
- `feature/ads/domain/AdPolicy.kt` — pure rules, unit tested.
- `feature/ads/data/AdsPrefs.kt` — counters in the shared DataStore (no personal data).
- `feature/ads/data/AdsManager.kt` — Pro check → grace → consent (UMP) → SDK start → preload → show.
- `feature/ads/data/AdsConfig.kt` — reads ids from BuildConfig.
- `feature/ads/ui/AdSlot.kt` — Home banner. `feature/ads/ui/AdBreakWatcher.kt` — spots "used a feature, back Home".
- `feature/ads/di/AdsEntryPoint.kt` — `rememberAdsManager()`.

## API calls
Only the existing `GET /api/pro/status?coupleId=` through `ProGate.refresh()`. No new backend.

## Not ported / not built (and why)
- Rewarded ads ("watch an ad to unlock Both mode for a day"): Pro limits are enforced by the server, so a
  client-only unlock would not work. Needs a backend change first. Left for a later session.
- App-open ads and ads in Chat/Calls: bad experience, deliberately left out.
- Native (custom-styled) ads: more code and more risk uncompiled; the banner card is the safe first step.
