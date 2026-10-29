# Optional web patch — show video / voice-note surprises made on the Android app

**Nothing is required on the server.** Surprises and capsules use the existing `/api/data/state`,
`/api/media/upload` and `/api/media/upload-voice` routes.

The only gap is in the old web page. It saves a file as a `data:` link and decides how to show it
with `s.mediaUrl.startsWith('data:video')`. The Android app uploads the file and saves a normal
link instead, so on the web a **photo** still shows, but a **video** or **voice note** made on the
phone would be drawn as a broken picture.

If you want the web to show them too, in `public/index.html` (`renderSurprises`, the line with
`s.mediaUrl.startsWith('data:video')`) replace the two tests:

- `s.mediaUrl.startsWith('data:video')` with `(s.mediaKind === 'video' || s.mediaUrl.startsWith('data:video'))`
- `s.mediaUrl.startsWith('data:audio')` with `(s.mediaKind === 'audio' || s.mediaUrl.startsWith('data:audio'))`

The Android app writes a `mediaKind` field (`"image" | "video" | "audio"`) on every surprise that has
a file. Old web items have no such field and keep working.
