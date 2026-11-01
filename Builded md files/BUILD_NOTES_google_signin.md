# Continue with Google (native Android)

**What it does**
- Sign In and Sign Up screens get a **Continue with Google** button (hidden if `GOOGLE_WEB_CLIENT_ID` is empty).
- Known email → signed in straight away (Sign In → Home; Sign Up tab → Home too, since that account may already be paired).
- New email → new **"Almost there"** screen (`Routes.CompleteProfile`): name (pre-filled from Google), Girl/Boy,
  phone, password, vault PIN + confirm. Then the account is created and the person goes to Pairing, like Sign Up.

**You must do**
1. Add to `local.properties`: `GOOGLE_WEB_CLIENT_ID=<Web OAuth client id>` (same value as `GOOGLE_CLIENT_ID` on the server).
2. In Google Cloud create an **Android** OAuth client: package `com.twinhearts.app` + SHA-1 of debug and release keys.
3. Deploy the matching server changes from the web patch (`/api/auth/google`, `/api/auth/google/complete`).

**CHANGE REQUEST (R-81/R-83, requested by the user): ** new route `Routes.CompleteProfile(signupToken, email, name)` in
`core/navigation/Routes.kt`, and added to `noPullRefreshRoutes` in `TwinApp.kt`.
**Dependencies added (R-15):** `androidx.credentials` + `credentials-play-services-auth` 1.3.0, `googleid` 1.1.1
(chosen because they build with Kotlin 2.0.21).
**Tests:** `GoogleSignInTest.kt`.
