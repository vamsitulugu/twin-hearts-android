# BUILD_NOTES — Phase 1: Get Started + Sign In / Sign Up / Forgot Password

**Not compiled here.** Build in Android Studio; send me the first red error if there is one.

## Apply
1. Unzip `th-p1-entry-v1.zip` over `C:\Users\vamsi\AndroidStudioProjects\TwinHearts` (replace files).
2. Android Studio: **File → Sync Project with Gradle Files**, then ▶ Run on your phone.
3. If you are already signed in from an earlier test you will see the stand-in Home page. Otherwise you start on Get Started.

## Expected result
Dark wine screen with the couple picture, "Stay Connected", and a white slider. Slide it to the end → Sign In.

## Manual tests
1. **Slider:** drag halfway and let go → knob springs back. Drag to the end → Sign In opens, phone gives a small tick. Press Back → Get Started again.
2. **Sign Up:** leave a field empty → "Fill in all fields". Try PIN 1234 → "...avoid sequences like 1234". Use two different PINs → mismatch message. Use a real new email, phone, PIN like 2580 → the app opens the Home stand-in page.
3. **Sign In (second account or after clearing app data):** wrong password → "Incorrect password." in red. Correct → Home. Close the app and reopen → it opens Home directly.
4. **Forgot Password:** Sign In → "Forgot Password?" → your email + registered phone → "Choose a new password" → set one → green message → back at Sign In → sign in with the new password.
5. **Offline:** turn on airplane mode, tap Sign In → "Can't reach the server..." message. Turn it off and try again. The first try after a long idle may take up to a minute and shows the "Waking up the server" line.

Tip: to test Get Started again after signing in, uninstall the app (or Settings → Apps → Twin Hearts → Storage → Clear data).

## Known gaps
- Home, Chat, etc. are still stand-in pages (next phases).
- Check the Privacy Policy address in `strings_auth.xml` (`auth_privacy_url`).
