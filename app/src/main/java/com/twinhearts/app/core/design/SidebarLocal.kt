package com.twinhearts.app.core.design

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Opens the side menu (the web hamburger drawer). Provided once by TwinApp; screens never
 * touch the drawer state. The default does nothing, so previews and the debug gallery are safe.
 */
val LocalOpenSidebar = staticCompositionLocalOf<() -> Unit> { {} }
