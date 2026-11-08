package com.twinhearts.app.feature.cinema.domain

import java.net.URI
import java.net.URLEncoder

/**
 * Why this file exists: all the small rules about cinema links in one plain-Kotlin place, so they can be
 * unit tested without Android. The cinema (twin-cinemas) is loaded live from the web; these rules keep
 * that safe and make its "Leave" button return to the app.
 */
object CinemaLinks {

    /**
     * Passed to the cinema as `b=`. The cinema's Leave button navigates here, the WebView sees it and
     * closes the screen. `.invalid` is reserved (RFC 2606): the address can never be a real website.
     */
    const val BACK_LINK = "https://twinhearts.invalid/"

    /** The cinema avatar for this person: girl = 1, boy = 2, unknown gender falls back to the role. Same rule as the web app. */
    fun avatarSlot(gender: String?, role: String): Int = when (gender) {
        "male" -> 2
        "female" -> 1
        else -> if (role == "user1") 1 else 2
    }

    /** Adds the back link to the cinema link the server gave us. The cinema reads its settings from the `#` part. */
    fun withBackLink(url: String): String {
        val separator = if (url.contains('#')) "&" else "#"
        return url + separator + "b=" + URLEncoder.encode(BACK_LINK, "UTF-8")
    }

    /** True when the WebView is being sent to the back link, meaning "Leave" was pressed in the cinema. */
    fun isBackLink(url: String?): Boolean = hostOf(url) == hostOf(BACK_LINK)

    /** Only secure (https) links are ever loaded in the cinema screen. */
    fun isSecure(url: String?): Boolean = url != null && schemeOf(url) == "https" && hostOf(url) != null

    /** True when [url] is on the same website as [cinemaUrl]: the only pages the cinema screen may show. */
    fun isSameSite(url: String?, cinemaUrl: String): Boolean {
        val host = hostOf(url) ?: return false
        return isSecure(url) && host == hostOf(cinemaUrl)
    }

    private fun hostOf(url: String?): String? = parse(url)?.host?.lowercase()

    private fun schemeOf(url: String): String? = parse(url)?.scheme?.lowercase()

    private fun parse(url: String?): URI? = try {
        url?.let { URI(it) }
    } catch (ignored: Exception) {
        null // a link that cannot be read is treated as "not allowed"
    }
}
