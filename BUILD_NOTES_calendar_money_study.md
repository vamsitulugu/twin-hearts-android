# BUILD NOTES — Calendar, Money, Study (v2 polish pack)

Built against repo commit 311d0d5 ("Update fight page").

## Verification done here
- No Android SDK in the sandbox, so nothing Android-specific was compiled (Compose screens, Hilt, Room, receivers).
- The pure logic (CalendarMath, MoneyMath, StudyMath, ReminderRules + models) was compiled with Kotlin 2.0.21 and its 33 unit tests were run with a stub JUnit: all pass.
- Brace/paren balance of every changed Kotlin file was checked.

## If the build fails
Send the exact error; the likely spots are import names in the three screens and `Petals.kt`.

## Try on the phone
1. Calendar: add an event with a time and "For both of us" 2 minutes ahead; close the app; a notification should arrive. Long-press an event, select two, delete.
2. Turn on airplane mode, reopen Calendar/Money/Study: saved data shows with the offline strip.
3. Money: use ‹ › to change month; type a goal and wait a second (no button), reopen to see it kept; tap a trend bar.
4. Study: tick a slot (petals); a slot with a odd day name from the web shows under that name.
5. Two phones: add something on one; the other updates within ~30 s while the page is open.
