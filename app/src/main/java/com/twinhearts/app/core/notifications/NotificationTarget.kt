package com.twinhearts.app.core.notifications

import android.content.Intent
import com.twinhearts.app.core.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Where a tapped notification should take the person. */
data class NotificationTarget(val screen: NotifScreen, val video: Boolean = false, val usTab: Int = 0) {

    /** The real navigation route for this target. */
    fun toRoute(): Any = when (screen) {
        NotifScreen.HOME -> Routes.Home
        NotifScreen.CHAT -> Routes.Chat
        NotifScreen.CALL -> Routes.Call(video = video, incoming = true)
        NotifScreen.LIVEMAP -> Routes.LiveMap
        NotifScreen.CALENDAR -> Routes.Calendar
        NotifScreen.STUDY -> Routes.Study
        NotifScreen.MEET_PLANNER -> Routes.MeetPlanner
        NotifScreen.ROUTINE -> Routes.DailyRoutine
        NotifScreen.PERIOD -> Routes.PeriodTracker
        NotifScreen.MONEY -> Routes.Money
        NotifScreen.GAMES -> Routes.Games
        NotifScreen.MOVIE -> Routes.Movie
        NotifScreen.CINEMA -> Routes.Home
        NotifScreen.MUSIC -> Routes.Music
        NotifScreen.FIGHTS -> Routes.Fights
        NotifScreen.US -> Routes.Us(usTab)
        NotifScreen.MEMORIES -> Routes.Memories
        NotifScreen.BUCKET -> Routes.BucketList
        NotifScreen.SURPRISES -> Routes.Surprises
        NotifScreen.LEVEL -> Routes.Level
        NotifScreen.PRO -> Routes.Pro()
        NotifScreen.SETTINGS -> Routes.Settings
        NotifScreen.PAIRING -> Routes.Pairing
        NotifScreen.VAULT -> Routes.Vault
        NotifScreen.PROFILE -> Routes.Profile
    }

    companion object {
        const val EXTRA_SCREEN = "twin_notif_screen"
        const val EXTRA_VIDEO = "twin_notif_video"
        const val EXTRA_US_TAB = "twin_notif_us_tab"
    }
}

/**
 * Hand-over between a tapped notification (MainActivity's intent) and the nav graph.
 * MainActivity calls [handle]; the nav host opens [pending], then calls [consume].
 */
@Singleton
class NotificationNavigator @Inject constructor() {
    private val _pending = MutableStateFlow<NotificationTarget?>(null)
    val pending: StateFlow<NotificationTarget?> = _pending.asStateFlow()

    fun handle(intent: Intent?) {
        val name = intent?.getStringExtra(NotificationTarget.EXTRA_SCREEN) ?: return
        val screen = NotifScreen.entries.firstOrNull { it.name == name } ?: return
        _pending.value = NotificationTarget(
            screen = screen,
            video = intent.getBooleanExtra(NotificationTarget.EXTRA_VIDEO, false),
            usTab = intent.getIntExtra(NotificationTarget.EXTRA_US_TAB, 0),
        )
        // Same intent must not open the screen again after a rotation.
        intent.removeExtra(NotificationTarget.EXTRA_SCREEN)
    }

    fun consume() {
        _pending.value = null
    }
}
