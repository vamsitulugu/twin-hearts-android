# Twin AI page UI update (neutral look, full-width switch, grey-glass typing bar)

Unzip over the project root, then Sync and Run. Only files inside `feature/twinai/` changed. No core/ edits, no new dependencies.

What changed
- Page background: near-black (#0A0A0C), no pink/burgundy shading (same as Chat and Home).
- YOU / BOTH switch: now full width, grey glass track, burgundy selected side.
- Typing bar: grey glass bar + lighter grey glass text field; "+" is a grey glass button, send stays burgundy.
- Header buttons, chips, Twin answer bubbles, hero card: grey glass instead of pink-tinted glass.
- BOTH tab: grey cards, numbered badges in "How it works", burgundy main buttons, no pink text.

Manual tests
1. Open Twin AI: background is black/grey, no pink glow at the top.
2. YOU / BOTH switch spans the whole width; tap both sides.
3. Type a message: bar and field look grey and glassy; send works.
4. BOTH tab: Start Together, open a discussion, submit an answer.
5. Font scale 1.3: nothing is cut off.

## Update 2
- Top bar: logo and back arrow removed. Left: menu. Then Twin AI title, search, sync. System back goes to Home.
- Bottom: the strip under the typing bar (system navigation area) is now black like the page, not burgundy.
  Needs one small core edit in `core/navigation/TwinApp.kt` (Twin AI owns the bottom inset, same as Chat).

## Update 3
- Menu button: no card/border, just the icon.
- Send button: solid burgundy (#6B093A), stays clearly burgundy even when the box is empty.

## Update 4: Settings page
- Same neutral look: near-black page, grey glass cards/fields/dialogs, burgundy primary buttons and "on" switches, no pink.
- Top bar: logo removed, menu (side menu) on the left, then title, search, sync.
- Done with one new file (`feature/settings/ui/components/SettingsStyle.kt`) that swaps the colour set inside Settings only. No core/ edit.
- The coloured row icons are unchanged (they match the reference screenshots).

## Update 5: bottom navigation bar
- Black backing plate under and around the bar (Home too), with a small gap around the pill and its two top corners curved up to match the bar. It hides whatever scrolls behind.
- On Settings the strip beside the orb is near-black too, so no burgundy shows.
- The bar's top edge now dips in a U shape around the Twin orb instead of the border running behind it.
- Files: `core/design/TwinBottomBar.kt`, `core/navigation/TwinApp.kt` (core edit, requested).
- If the U looks too wide/narrow, change `NotchRadius` / `NotchCenterY` at the bottom of TwinBottomBar.kt.

## Update 6: Live Map page
- Same neutral look: near-black page, grey glass cards/fields/dialogs, burgundy primary buttons and "on" switches, light-grey links and icons, no pink.
- One new file (`feature/livemap/ui/components/LiveMapStyle.kt`) swaps the colour set inside Live Map only.
- Map markers keep their own colours (you / partner / notes), so you can still tell them apart.
- The bottom-bar slot beside the orb is near-black on Live Map too (`core/navigation/TwinApp.kt`).
- Top bar is unchanged in layout (back, title, search, menu), because those buttons drive the Live Map sub-pages.
