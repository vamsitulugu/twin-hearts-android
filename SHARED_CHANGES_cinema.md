# SHARED_CHANGES — Twin Cinema v1

This zip unzips straight over the project root (R-70). It **adds** the `feature/cinema/` folder and **changes 11 shared files**
(listed below, every change is tiny). Nothing else is touched. If you edited any of these files since the last delivery,
apply the small snippets by hand instead of overwriting.

## Gradle / catalog lines
None. (No new library: `WebView`, `androidx.core`, Navigation and Hilt are already in the project.)

## Manifest entries
None. `INTERNET` is already declared. No new permission is asked.

## AppNavHost line
1. `core/navigation/Routes.kt`, inside `object Routes`:
   ```kotlin
   @Serializable data class Cinema(val sessionId: String? = null)
   ```
2. `core/navigation/FeatureGraphs.kt`:
   ```kotlin
   import com.twinhearts.app.feature.cinema.cinemaGraph
   ...
   cinemaGraph(navController) // Twin Cinema (3D theatre)
   ```
3. `core/navigation/TwinApp.kt`:
   - add `Routes.Cinema::class` to `noPullRefreshRoutes` (pull-down must never reload the cinema);
   - add `import com.twinhearts.app.feature.cinema.ui.invite.CinemaInviteWatcher`;
   - right after `val destination = backStackEntry?.destination` add:
     ```kotlin
     CinemaInviteWatcher(
         enabled = session != null && destination?.hasRoute<Routes.Cinema>() != true,
         onAccept = { sessionId -> navController.navigate(Routes.Cinema(sessionId)) { launchSingleTop = true } },
     )
     ```
4. `feature/ads/ui/AdBreakWatcher.kt`: add `d.hasRoute<Routes.Cinema>() ||` next to `Routes.Movie` in `canCoverWithAd` (no ad over a movie night).
5. `feature/home/ui/HomeShortcuts.kt`: new `Cinema(R.string.cinema_chip_label, Icons.Rounded.Theaters)` after `Movie`, and `HomeShortcut.Cinema` after `HomeShortcut.Movie` in `homeChips`.
6. `feature/home/HomeRoute.kt`: `HomeShortcut.Cinema -> navController.navigate(Routes.Cinema())`.
7. `feature/home/ui/HomeComponents.kt`: `HomeChips` is now a row that keeps each tile at least 64 dp and scrolls sideways when six tiles do not fit (before it was 5 fixed tiles).
8. `core/navigation/TwinSidebar.kt`: `SidebarEntry(Routes.Cinema(), R.string.cinema_chip_label, Icons.Outlined.Theaters)` under Together, after Movie.
9. Notifications (`core/notifications/`):
   - `NotificationType.kt`: `NotifScreen.CINEMA`; `CINEMA_INVITE("cinema_invite", TwinChannel.ACTIVITY, R.string.notif_t_cinema_invite, NotifScreen.CINEMA)`; server tag `"cinema-invite" -> CINEMA_INVITE`.
   - `NotificationTarget.kt`: `NotifScreen.CINEMA -> Routes.Home` (the invite watcher then shows the card).
   - `NotificationDefaults.kt`: title and body lines for `CINEMA_INVITE`.

## TwinDatabase entities
None.

## Strings / permissions
New file `res/values/strings_cinema.xml` (`cinema_*` and the three `notif_*cinema_invite*` strings). No permissions.

## Backend changes
None. `routes/cinema.js` already has create / invite / pending / accept / decline and sends the push.
One-time setup that only you can do (details in `docs/features/cinema.md`): Supabase SQL `migrations/cinema_sessions.sql`,
Render deploy, Vercel env vars `VITE_SUPABASE_URL` + `VITE_SUPABASE_ANON_KEY` then redeploy.

## CHANGE REQUEST: allow ONE WebView screen (README R-01 and §14) — please confirm
- **Why:** you asked for the 3D cinema to open inside the app (not a browser) and to update without a new APK. That needs a WebView.
  README R-01 / §14 forbid WebView screens and say the README wins unless you say "override README rule R-xx".
  I treated your request as that override. I did **not** change the README rules themselves.
- **Proposed README text** (R-01): `Kotlin only. No Java files, no Flutter, no React Native, no WebView screens, except the Cinema screen (feature/cinema), which shows the live twin-cinemas website.`
  and (§14) `... or wrap web pages in a WebView (except the Cinema screen).`
- **Affected features:** only Cinema. The WebView only loads the cinema's own https host.
- **Also touched shared files** (normally off-limits by R-30/R-31): the ones listed above. They are the minimum needed
  to open the screen, show the chip and invite, and handle the notification tap.
