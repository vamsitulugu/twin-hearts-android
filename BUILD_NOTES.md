# BUILD_NOTES — Phase 2.1 Home

**Not compiled here.** Build in Android Studio; send me the first red error if there is one.

## Apply
Unzip `th-p2-home-v1.zip` over your project folder (replace files), **Sync Project with Gradle Files**, ▶ Run.

## What you should see
Logo and your avatar, "Good evening, <your name> 👋", the daily quote, search bar, chips, **Stay Connected** card with a picture, then the page groups and a quote at the bottom.

## Tests
1. **Not connected yet:** a "Connect with your partner ❤️" card shows; the three buttons are greyed. Tap the card → Pairing.
2. **Connected (two accounts):** on phone A tap **❤️ Touch** → green "Sent a touch to NAME 💓", picture changes, caption "❤️ Touch sent · just now". On phone B (Home open) within ~8 s: caption "❤️ Touch received", same picture. Try **🥺 Miss You**.
3. **Hug:** A taps **🤗 Hug** → "Hug sent — waiting for NAME to accept". B sees "A is sending you a hug 🤗" → **Accept Hug** → both show "🤗 Hug ..." on the card. Also try **Later**.
4. Web and app share this data: do a Touch from the old web app, the Android card shows it (and the other way round).
5. Tap the avatar → Profile stand-in page. Tap any other page tile → "This screen is built in a later step."
6. Turn on a large font: the three buttons must still fit on the card.

## Known gaps
No vibration/notification when your partner sends something yet (Firebase, Phase 2.5). "Coming up" and mood card come with Calendar/Mood.
