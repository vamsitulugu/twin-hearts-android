package com.twinhearts.app.feature.chat.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the GIF search key must come from `local.properties` through BuildConfig
 * (R-61: no key typed into code). It is read by name, so the app still builds when the person has
 * not added the key yet; the GIF picker then shows a clear "needs a key" message instead.
 */
@Singleton
class GifKeyProvider @Inject constructor() {
    val key: String = try {
        Class.forName("com.twinhearts.app.BuildConfig").getField("GIPHY_API_KEY").get(null) as? String ?: ""
    } catch (e: ReflectiveOperationException) {
        ""
    }
}
