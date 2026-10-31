# BUILD NOTES — Ads (AdMob)

Not compiled here (no Android SDK). The pure rules in `AdPolicy.kt` WERE compiled with Kotlin 2.0.21 and the
13 checks in `AdPolicyTest.kt` pass. Build in Android Studio and send me any error text.

## 1. Build and run (Windows)
1. Unzip over the project root. In Android Studio: **File > Sync Project with Gradle Files**.
2. Run the **debug** build on your phone. Debug always uses Google's TEST ads (safe to tap).

## 2. See the ads while testing (new accounts are protected by design)
The grace rules hide ads for new couples. To test, temporarily set these in `AdRules` (AdPolicy.kt), then put them back:
`NEW_USER_GRACE_MILLIS = 0`, `MIN_SESSIONS = 1`, `MIN_SESSION_AGE_MILLIS = 0`, `MIN_TIME_IN_FEATURE_MILLIS = 3_000`,
`MIN_GAP_MILLIS = 0`, `BREAKS_BETWEEN_ADS = 1`. Use a FREE (non-Pro) couple.

## 3. Manual tests
1. Free couple, Home, scroll down: a "Sponsored" test banner appears under "All pages". No gap before it loads.
2. Open Games for 5+ s, press back to Home: a full-screen test ad appears about 1 second later.
3. Open Chat / a call / Settings: no ad, ever.
4. Pro couple (or after buying Pro): no banner, no full-screen ad.
5. Turn on airplane mode, restart the app: no ads (server could not confirm Pro).

## 4. Going live (release)
Add to `local.properties` (never commit it):
```
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```
A release build with any of the three missing shows NO ads (it never shows test ads to real people).
NEVER tap your own live ads: Google can ban the account. Test with the debug build only.

## 5. In the AdMob console (strongly recommended)
- Blocking controls > Sensitive categories: block Dating, Gambling, Alcohol, Politics, Religion, Dietary/weight loss, Get-rich-quick.
- Set the banner refresh to 60 s or off.
- Payments: PIN letter (about $10 earned) and bank details; payout starts at $100 earned.

## Known gaps
- Rewarded ads not built (needs backend change, see docs/features/ads.md).
- If one phone switches from a free couple to a Pro couple, ads can show for up to 10 minutes until the next Pro check.
