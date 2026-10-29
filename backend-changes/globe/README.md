# Backend note for the Memory Globe (optional, not needed for the Android app)

The Android Globe works with your server **as it is today**. These notes are things I found while reading the web app's code. Fixing them improves the web Globe too.

## 1. `public/globe.js` is a misplaced copy of the backend route
`public/globe.js` is not browser code. It is a copy of the **server route** (`require('express')`, `supabase`, ...), and it is the **newer** one: it accepts `mediaList` (many photos in one request). The real `routes/globe.js` is the older version and does not understand `mediaList`.
The web page (`public/globe.html`) sends `mediaList` when you add several photos, so on the old route those photos are saved as empty rows.

**Fix:** replace `routes/globe.js` with `routes-globe.js` from this folder (it is the newer file, unchanged), then delete `public/globe.js` (it is also publicly downloadable from your site, which you do not want for server code).

The Android app sends one `media` object per request, which works with both versions.

## 2. Web voice notes on the Globe are rejected by the server
`globe.html` uploads voice notes (`audio/webm`) to `POST /api/media/upload`, but that route only accepts images and videos (see `ALLOWED_MEDIA_MIME` in `routes/media.js`). So voice notes added from the web Globe fail with "That file type isn't supported".
The Android app uses `POST /api/media/upload-voice` (the route Chat already uses), which accepts audio. To fix the web page, change its upload URL to `/api/media/upload-voice` and also send `senderRole`.

## 3. Rotate the Cesium token that is in `globe.html`
`public/globe.html` contains a Cesium ion access token in plain text in a public repo. Anyone can copy it and use your quota. In the Cesium ion dashboard, create a new token (limit it to your website address) and delete the old one. The Android app does not use Cesium or this token.
