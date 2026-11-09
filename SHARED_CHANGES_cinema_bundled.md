# SHARED CHANGES — Cinema bundled inside the APK

## CHANGE REQUEST (README R-31 / R-83)
New shared folder `core/cinema/` (BundledCinema.kt, BundledCinemaServer.kt). Two features (`feature/cinema`, `feature/movie/cinema`)
need the same loader, and R-83 forbids importing one feature from the other, so it lives in `core/`. No existing core file changed.

## Changed files
- `app/build.gradle.kts` – `androidResources.noCompress` for glb/mp3/mp4/wav/ogg/webp (exact sizes + byte ranges).
- `feature/cinema/domain/CinemaLinks.kt` – removed the remote-site rules (`isSecure`, `isSameSite`).
- `feature/cinema/ui/CinemaViewModel.kt` – opens `BundledCinema.localUrl(serverLink)`; a link with no room settings is an error.
- `feature/cinema/ui/CinemaWebView.kt` – answers requests from the APK; blocks anything not on the app's own host.
- `feature/movie/cinema/ui/CinemaTheatre.kt` – same; the movie stream and /leave addresses are unchanged.
- Tests: `CinemaLinksTest`, `CinemaViewModelTest` updated, `BundledCinemaTest` added.
- Docs: `docs/features/cinema.md`.

## New
- `app/src/main/assets/cinema/**` – copy of `us-app/public/cinema` (~41 MB, 76 files).
- `tools/sync-cinema.ps1` / `.sh` – build + copy in one command.

## Web repo (us-app) – 1 line
`server.js`: add `https://appassets.androidplatform.net` to `allowedOrigins` (patch: us-app-cors-android-cinema.patch).
