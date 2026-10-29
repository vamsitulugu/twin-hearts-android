# Feature: Memories ("Our Memories")

Native Compose page; the web app (`us-app`, `#page-camera` in index.html) was the reference. Entry: Home "Memories" chip → `Routes.Memories`. Code: `feature/memories/`.

## What it has
- **Capture a Moment:** Take Photo and Record Video (the phone's camera app), Add Photos / Add Video (gallery picker, up to 20 at once), and a "+ Add" tile in the grid for either.
- **Tip card** ("You've added N memories this month…") and **Memories Added by Month** (last six months, count above each bar).
- **Our Memories grid** (3 columns, same order as the web: oldest first, newest last, favourites starred). Tap = full-screen viewer. Long-press = select several; the bar shows Share and Delete.
- **Viewer:** swipe between photos and videos, pinch / double-tap to zoom, tap to hide the bars, favourite, share, delete, and a menu with Download (opens the file's address) and Details (date added, type, file name).
- Upload progress ("Uploading 2 of 5…"), a Retry strip for files that failed, petals when something is added, offline copy with banner, silent refresh every ~30 s.

## Server calls
| Call | Used for |
|---|---|
| `GET api/data/state/{coupleId}` | read the album (`photos`) |
| `POST api/data/state` `{coupleId, state:{photos:[…]}, senderRole, myName}` | save the album (the server replaces the whole `photos` list and tells my partner about a new memory) |
| `POST api/media/upload` (multipart `file`, `coupleId`) → `{url, path}` | send a photo or video (3 at a time) |
| `DELETE api/media/delete` `{path, bucket:"couple-photos", coupleId}` | remove the stored file |

Album item: `{id, url, name, type:"image"|"video", date:"yyyy-mm-dd", path, favorite?}`; the same shape the web writes, so both apps read each other's items. Items this app does not understand are kept untouched.

## Decisions
- Files are uploaded first, then ONE save adds them all. If the save fails after uploading, the files are in storage but not in the album (the toast says it failed; add them again).
- Delete removes the stored file first, then the album item; an item whose file could not be removed stays and is counted in "couldn't be deleted" (same as the web).
- The server only accepts jpeg/png/gif/webp/heic/heif and mp4/quicktime/webm/3gpp/mkv; anything else is refused on the phone with the server's own wording.
- Offline copy keeps only the web addresses (very old items saved as inline "data:" text are left out, and a list over ~1 MB is not cached) so the local database can never break.

## Not done / known gaps
- Not compiled or run here (no Android SDK).
- No live camera preview with Snap / Flip inside the page (the web has one): the phone's own camera app is used, which also gives flash, zoom and HDR for free.
- Video tiles show a play badge, not a frame from the video (needs an extra library).
- Writes need a connection. Two phones saving at the same moment: the later save wins (the server replaces the whole list).
- Download opens the browser; it does not save directly to the gallery.
