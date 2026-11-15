# SHARED CHANGES — Twin Cinema 3D inside Watch Together (Android)

Same entry points as the web app: the "Watch in Twin Cinema 3D" card, the "3D MOVIE (Twin Cinema)" button next to START TOGETHER,
the invite card, and "both ready → both go in together". The theatre itself is the web theatre (twin-cinemas) in a WebView, so
web ↔ Android and Android ↔ Android sit in the same room.

## Gradle/catalog lines
None. (WebView, Compose and Retrofit are already in the project.)

## Manifest entries
None. (INTERNET is already declared. The theatre is a Compose `Dialog` — still a single Activity.)

## AppNavHost line
None. (Everything is inside the existing Movie screen.)

## TwinDatabase entities
None.

## Strings/permissions
New file `app/src/main/res/values/strings_movie_cinema.xml` (wording copied from the web movie page). No new permissions.

## Backend changes
None. Uses the existing `routes/cinema.js` calls: `create`, `invite`, `pending`, `accept`, `decline`.
(The web side needs the matching `public/cinema-invite.js` from the us-app zip, and the cinema site needs the twin-cinemas zip.)

## CHANGE REQUEST: README R-01 ("no WebView screens")
- **Why:** the 3D theatre is a Three.js web page. A native 3D theatre was tried (commit 84d61c2) and reverted, and the user wants web and Android phones in the SAME theatre.
- **What:** ONE WebView, in `feature/movie/cinema/ui/CinemaTheatre.kt`. It only opens the cinema site the server gives us, answers two private addresses itself
  (`/twin-movie` streams the movie file picked on this phone, `/leave` closes the screen) and nothing else.
- **Features affected:** Movie only. Please approve by adding "override README rule R-01 for Twin Cinema 3D" to the README.

## Files outside `feature/movie/cinema/` that were changed (small edits)
- `feature/movie/ui/MovieScreen.kt` — card handler, invite card, follow-your-partner loop, theatre dialog.
- `feature/movie/ui/WatchViewModel.kt` — `localMovie()` and `pass3D()` (same free-movie / coin pass as START TOGETHER).
- `feature/movie/ui/components/LobbyContent.kt` — the "3D MOVIE" button under START TOGETHER.
- `feature/movie/ui/components/MovieTabs.kt` — the card above the Home and Watch tabs.
