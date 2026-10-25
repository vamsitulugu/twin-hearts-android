# Feature 1.1 — Get Started (audit + explanation)

Source read: `public/landing.html` → `#mobileStart` markup, its CSS, and the slider script (`msSlide`).

## Screen (top to bottom)
1. Logo + "Twin Hearts" (serif) + "EVERYTHING TOGETHER" (tiny, spaced capitals).
2. "Stay" (soft pink, serif) and "Connected" (pink-to-lilac gradient, serif) with a small glowing heart; below it "NO DISTANCE, JUST US".
3. The couple picture (`start-couple.webp`) sitting on the wine background photo (`start-bg.jpg`, anchored to the bottom).
4. White pill slider "GET STARTED" with a round burgundy knob and arrow.

## Behaviour
- Drag the knob; past **82%** it completes and opens **Sign In**. Let go earlier and it springs back (same as the web).
- Small "nudge" of the knob every ~2.4 s until you touch it (stopped when the phone has animations off).
- A short haptic tick on completion.
- TalkBack: double-tap the control to continue (no dragging needed).
- Back from Sign In returns here.

## Not ported (and why)
- "Download the app / Continue in browser" step: that is for people on a website. Inside the app the web itself skips it and goes straight to Sign In (`msNext`: `isInsideNativeApp`).
- Marketing sections of the landing page (install guide, what's new): website only.

## Assets copied (your own files)
`start-bg.jpg` → `getstarted_bg.jpg`, `start-couple.webp` → `getstarted_couple.webp`, `icon-192.png` is already `core_logo.png`.

## States
No data to load, so no loading/empty/error states and no ViewModel (R-41 does not apply).
Previews: normal and large font (1.3).
