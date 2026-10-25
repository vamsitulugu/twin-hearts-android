# Feature 1.2 — Sign In, Sign Up, Forgot Password (audit + explanation)

Sources read: `public/landing.html` (`#authModal`, `doEmailLogin`, `doEmailRegister`, `doVerifyIdentity`, `doUpdatePassword`, `saveAndGo`) and `routes/auth.js`.

## Screens
| Screen | Web part | Fields / buttons (wording kept) |
|---|---|---|
| Sign In | `#loginView` | tabs Sign In/Sign Up · "Welcome back 💕" · Email · Password · "Forgot Password?" · "Sign In 💕" · "— or —" · "Create new account →" · privacy line |
| Sign Up | `#registerView` | "Create your account ✨" · Email · Password (min 6 chars) · Your Name · I am 👩 Girl / 👨 Boy · Phone Number (+ help text) · Secret Vault PIN (4 digits) · Confirm Vault PIN (+ help text) · "Create Account ✨" · "Already have an account?" · "Sign In →" |
| Forgot Password | `#forgotView` | "Reset your password 🔑" · intro · Email · Phone Number · "Continue" · "Remembered your password?" · "Back to Login →" |
| New password | `#changePasswordView` | "Choose a new password 🔒" · New Password · Confirm Password · "Update Password" |

## API calls (read from `routes/auth.js`, mounted at `/api/auth`)
| Action | Request | Success answer | Errors seen in the code |
|---|---|---|---|
| Sign in | `POST /api/auth/login` `{email, password}` | `{userId, coupleId, myName, email, phoneNumber, partnerName, anniversary, paired, role}` | 400 missing fields · 401 "No account found with this email." / "Incorrect password." · 403 suspended/disabled · 429 too many tries (20 per 15 min per IP) |
| Sign up | `POST /api/auth/register` `{email, password, myName, phoneNumber, vaultPin}` | `{userId, coupleId, myName, phoneNumber, partnerName:"Partner", role:"user1", paired:false}` | 400 (fields, password < 6, PIN rules, phone < 7 digits) · 409 email or phone already used · 500 |
| Verify identity | `POST /api/auth/verify-reset-identity` `{email, phone}` | `{resetToken}` (valid 10 min) | 400 one generic message for any mismatch · 429 (5 per 15 min) |
| New password | `POST /api/auth/reset-password` `{token, newPassword}` | `{message:"Password updated"}` | 400 missing / < 6 chars / "This password reset link has expired." · 500 · 429 (10 per 15 min) |

Gender is **not** sent to the server at sign up (the web keeps it on the device and copies it into the profile later). Here it is saved in the `Session` (`gender`). Moving it into the profile is for the Profile feature (2.6).

## Session (R-64)
There are **no tokens or cookies**. After login the app keeps `userId`, `coupleId`, … in DataStore (`core/data/SessionStore`). On the next launch a saved session opens Home directly.
Not done here (on purpose): re-checking the session with `GET /api/auth/session/:userId` at launch. A sleeping server must not sign people out; this check belongs to Home (2.1), which can show it softly.

## Behaviour and edge cases
- Checks run on the phone first, in the web's order; the server checks again. Server messages (e.g. "Incorrect password.") are shown as the server wrote them.
- No internet / sleeping server: friendly network message. After 6 s of waiting an extra line explains the server may be waking up (free Render plan, R-24). Buttons show the web's loading words ("Signing in...", "Creating...", "Checking...", "Updating...") and are disabled meanwhile.
- PIN boxes take digits only, max 4, number keyboard, hidden text.
- Email, name, phone and gender survive the app being killed in the background; passwords and PINs never do.
- Reset token lives only in memory and is dropped after use. After success the green message shows 1.6 s, then back to Sign In (same as the web).
- The Sign In / Sign Up tabs swap places, so Back always returns to Get Started.
- After sign in/up the entry screens are removed from Back history and the app opens Home.
- "Privacy Policy" opens `https://twinhearts.vercel.app/privacy-policy.html` in the browser. **Please check this address is correct** — I took it from your old Capacitor config.

## Not ported
- The web's `SessionManager.rememberAccount` (account picker after reinstall): only matters for several accounts in one browser.
- Auth overlay as a popup: replaced by full screens.

## States
Forms have three states: normal, loading (button disabled + hint) and error (red box). Empty does not apply. Previews: normal, error, loading + large font.

## Tests
`AuthValidatorsTest`, `AuthMapperTest`, `SignInViewModelTest`, `SignUpViewModelTest` (17 tests).
