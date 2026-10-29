# Build notes - Side menu + glass bottom bar (web UI match)

Changed (all under app/src/main/java/com/twinhearts/app/):
- core/design/TwinBottomBar.kt - glass dark pill, line icons (filled + soft highlight when active), raised Twin orb with ring.
- core/navigation/TwinSidebar.kt (NEW) - side menu: logo, couple widget + sync state, grouped pages, Settings at bottom.
- core/navigation/TwinApp.kt - ModalNavigationDrawer; back closes the menu; rows without a screen show the "later step" note.
- core/navigation/AppViewModel.kt - exposes `session` for the couple widget.
- core/design/SidebarLocal.kt (NEW) - `LocalOpenSidebar`.
- core/design/TwinTopBar.kt - order is now back, logo, hamburger, title (like web); new `showSidebarButton`.
- core/design/TwinColors.kt / TwinBackground.kt - nav tokens; burgundy header fading to dark body.
- feature/chat/ui/ChatScreen.kt, core/navigation/PendingFeatureScreen.kt - hamburger on.
- feature/home/HomeRoute.kt - Home grid button opens the side menu.
- res/values/strings_sidebar.xml (NEW).

Open the menu: hamburger in the top bar (Chat, Settings, Profile) or the grid button next to Home search.
Not compiled here (no Android SDK) - Sync Gradle and Run in Android Studio.
Still placeholders: Settings, Profile screens (they open the "later step" page with the new top bar).

## Colour match (web theme-tokens.css dark)
TwinColors: glass = magenta 9%, line 18% + new line2 30%, card = web surface #2B061D, primaryEnd = brand-d #7C0A57,
inkMuted = web text2, backgroundGlow = #3A0425. GlassButton Secondary/GlassIconButton use glass + line2 (web btn-glass).
Home: burgundy hero + black rounded sheet (HomeScreen.kt).
