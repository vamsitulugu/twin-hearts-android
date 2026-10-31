package com.twinhearts.app.core.notifications

import android.app.NotificationManager
import androidx.annotation.StringRes
import com.twinhearts.app.R

/**
 * Why this file exists: ONE list of every notification the app can send or receive, so the push
 * payload (`type`), the Android channel, the Settings switch and the screen that opens on tap
 * all come from the same row. Add a feature notification = add one line here.
 */

/** Android channels. Users can tune sound/vibration per channel in the phone's settings. */
enum class TwinChannel(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val importance: Int,
) {
    MESSAGES("twin_messages", R.string.notif_ch_messages, R.string.notif_ch_messages_desc, NotificationManager.IMPORTANCE_HIGH),
    CALLS("twin_calls", R.string.notif_ch_calls, R.string.notif_ch_calls_desc, NotificationManager.IMPORTANCE_HIGH),
    LOVE("twin_love", R.string.notif_ch_love, R.string.notif_ch_love_desc, NotificationManager.IMPORTANCE_HIGH),
    LOCATION("twin_location", R.string.notif_ch_location, R.string.notif_ch_location_desc, NotificationManager.IMPORTANCE_HIGH),
    /** Same id the Reminders feature already used, so people keep their existing sound settings. */
    REMINDERS("twin_reminders", R.string.notif_ch_reminders, R.string.notif_ch_reminders_desc, NotificationManager.IMPORTANCE_HIGH),
    ACTIVITY("twin_activity", R.string.notif_ch_activity, R.string.notif_ch_activity_desc, NotificationManager.IMPORTANCE_DEFAULT),
    UPDATES("twin_updates", R.string.notif_ch_updates, R.string.notif_ch_updates_desc, NotificationManager.IMPORTANCE_LOW),
}

/** The screen a notification opens. Mapped to real navigation routes in [NotificationTarget]. */
enum class NotifScreen {
    HOME, CHAT, CALL, LIVEMAP, CALENDAR, STUDY, MEET_PLANNER, ROUTINE, PERIOD, MONEY, GAMES, MOVIE,
    MUSIC, FIGHTS, US, MEMORIES, BUCKET, SURPRISES, LEVEL, PRO, SETTINGS, PAIRING, VAULT, PROFILE,
}

enum class NotificationType(
    /** Stable id for this type (saved in Settings). Never rename. */
    val key: String,
    val channel: TwinChannel,
    @StringRes val label: Int,
    val screen: NotifScreen,
    /** Hidden on the lock screen (period, vault, money …). */
    val sensitive: Boolean = false,
    /** Shown even while the app is open (reminders, SOS). Others are skipped: the app shows them itself. */
    val showInForeground: Boolean = true,
    /** Rings / vibrates even inside quiet hours. */
    val breaksQuietHours: Boolean = false,
    val defaultOn: Boolean = true,
) {
    // Chat
    CHAT_MESSAGE("chat_message", TwinChannel.MESSAGES, R.string.notif_t_chat_message, NotifScreen.CHAT, showInForeground = false),
    CHAT_REACTION("chat_reaction", TwinChannel.MESSAGES, R.string.notif_t_chat_reaction, NotifScreen.CHAT, showInForeground = false),

    // Calls
    CALL_INCOMING("call_incoming", TwinChannel.CALLS, R.string.notif_t_call_incoming, NotifScreen.CALL, showInForeground = false, breaksQuietHours = true),
    CALL_MISSED("call_missed", TwinChannel.CALLS, R.string.notif_t_call_missed, NotifScreen.CHAT),

    // Love
    PARTNER_HUG("partner_hug", TwinChannel.LOVE, R.string.notif_t_partner_hug, NotifScreen.HOME),
    PARTNER_MISS("partner_miss", TwinChannel.LOVE, R.string.notif_t_partner_miss, NotifScreen.HOME),
    PARTNER_TOUCH("partner_touch", TwinChannel.LOVE, R.string.notif_t_partner_touch, NotifScreen.HOME),
    SURPRISE_NEW("surprise_new", TwinChannel.LOVE, R.string.notif_t_surprise_new, NotifScreen.SURPRISES),
    SURPRISE_UNLOCKED("surprise_unlocked", TwinChannel.LOVE, R.string.notif_t_surprise_unlocked, NotifScreen.SURPRISES),
    ANNIVERSARY("anniversary", TwinChannel.LOVE, R.string.notif_t_anniversary, NotifScreen.US, breaksQuietHours = false),
    MILESTONE("milestone", TwinChannel.LOVE, R.string.notif_t_milestone, NotifScreen.US),
    MEMORY_ADDED("memory_added", TwinChannel.LOVE, R.string.notif_t_memory_added, NotifScreen.MEMORIES),
    MEMORY_THROWBACK("memory_throwback", TwinChannel.LOVE, R.string.notif_t_memory_throwback, NotifScreen.MEMORIES),
    BUCKET_ADDED("bucket_added", TwinChannel.LOVE, R.string.notif_t_bucket_added, NotifScreen.BUCKET),
    BUCKET_DONE("bucket_done", TwinChannel.LOVE, R.string.notif_t_bucket_done, NotifScreen.BUCKET),

    // Live Map
    LOCATION_ARRIVED("location_arrived", TwinChannel.LOCATION, R.string.notif_t_location_arrived, NotifScreen.LIVEMAP),
    LOCATION_LEFT("location_left", TwinChannel.LOCATION, R.string.notif_t_location_left, NotifScreen.LIVEMAP),
    LOCATION_OFFLINE("location_offline", TwinChannel.LOCATION, R.string.notif_t_location_offline, NotifScreen.LIVEMAP, defaultOn = false),
    LOCATION_SHARING("location_sharing", TwinChannel.LOCATION, R.string.notif_t_location_sharing, NotifScreen.LIVEMAP),
    LOCATION_SOS("location_sos", TwinChannel.LOCATION, R.string.notif_t_location_sos, NotifScreen.LIVEMAP, breaksQuietHours = true),
    MEET_PLANNER("meet_planner", TwinChannel.LOCATION, R.string.notif_t_meet_planner, NotifScreen.MEET_PLANNER),

    // Plans & reminders
    CALENDAR_REMINDER("calendar_reminder", TwinChannel.REMINDERS, R.string.notif_t_calendar_reminder, NotifScreen.CALENDAR),
    CALENDAR_EVENT("calendar_event", TwinChannel.ACTIVITY, R.string.notif_t_calendar_event, NotifScreen.CALENDAR),
    STUDY_REMINDER("study_reminder", TwinChannel.REMINDERS, R.string.notif_t_study_reminder, NotifScreen.STUDY),
    ROUTINE_REMINDER("routine_reminder", TwinChannel.REMINDERS, R.string.notif_t_routine_reminder, NotifScreen.ROUTINE),
    PERIOD_REMINDER("period_reminder", TwinChannel.REMINDERS, R.string.notif_t_period_reminder, NotifScreen.PERIOD, sensitive = true),
    MONEY_UPDATE("money_update", TwinChannel.ACTIVITY, R.string.notif_t_money_update, NotifScreen.MONEY, sensitive = true),

    // Together
    GAME_INVITE("game_invite", TwinChannel.ACTIVITY, R.string.notif_t_game_invite, NotifScreen.GAMES),
    MOVIE_INVITE("movie_invite", TwinChannel.ACTIVITY, R.string.notif_t_movie_invite, NotifScreen.MOVIE),
    MUSIC_SHARED("music_shared", TwinChannel.ACTIVITY, R.string.notif_t_music_shared, NotifScreen.MUSIC),
    FIGHT_LOGGED("fight_logged", TwinChannel.ACTIVITY, R.string.notif_t_fight_logged, NotifScreen.FIGHTS),
    FIGHT_RESOLVED("fight_resolved", TwinChannel.ACTIVITY, R.string.notif_t_fight_resolved, NotifScreen.FIGHTS),
    VAULT_ACTIVITY("vault_activity", TwinChannel.ACTIVITY, R.string.notif_t_vault_activity, NotifScreen.VAULT, sensitive = true),

    // Pairing
    PARTNER_REQUEST("partner_request", TwinChannel.ACTIVITY, R.string.notif_t_partner_request, NotifScreen.SETTINGS, breaksQuietHours = false),
    PARTNER_ACCEPTED("partner_accepted", TwinChannel.LOVE, R.string.notif_t_partner_accepted, NotifScreen.HOME),
    PARTNER_REMOVED("partner_removed", TwinChannel.ACTIVITY, R.string.notif_t_partner_removed, NotifScreen.PAIRING),

    // App
    LEVEL_UP("level_up", TwinChannel.UPDATES, R.string.notif_t_level_up, NotifScreen.LEVEL),
    PRO_UPDATE("pro_update", TwinChannel.UPDATES, R.string.notif_t_pro_update, NotifScreen.PRO),
    SYSTEM("system", TwinChannel.UPDATES, R.string.notif_t_system, NotifScreen.HOME);

    companion object {
        fun fromKey(key: String?): NotificationType? = entries.firstOrNull { it.key == key }

        /** The server labels every push with a `tag` (routes/auth.js). This turns it into our type. */
        fun fromServerTag(tag: String?): NotificationType {
            val t = tag.orEmpty()
            return when {
                t == "chat-msg" -> CHAT_MESSAGE
                t == "incoming-call" -> CALL_INCOMING
                t == "missed-call" -> CALL_MISSED
                t == "hug" -> PARTNER_HUG
                t == "touch" -> PARTNER_TOUCH
                t == "missyou" -> PARTNER_MISS
                t == "partner-request" -> PARTNER_REQUEST
                t == "partner-accepted" || t == "paired" -> PARTNER_ACCEPTED
                t == "ck-invite" || t == "both-nudge" -> GAME_INVITE
                t.startsWith("safety-") -> LOCATION_OFFLINE
                t == "events" -> CALENDAR_EVENT
                t == "reminder" -> CALENDAR_REMINDER
                t == "meetplan" || t == "meetup-complete" -> MEET_PLANNER
                t == "photos" || t == "journal" || t.startsWith("globe-") -> MEMORY_ADDED
                t == "capsule" -> SURPRISE_NEW
                t == "milestone" -> MILESTONE
                t.startsWith("music-") || t == "song" || t == "karaoke-rec" || t == "my-recordings" -> MUSIC_SHARED
                t == "bucket" -> BUCKET_ADDED
                t == "fight" -> FIGHT_LOGGED
                t == "money" -> MONEY_UPDATE
                t == "period" -> PERIOD_REMINDER
                t == "vault" -> VAULT_ACTIVITY
                t == "surprise" -> SURPRISE_NEW
                t == "daily-routine" || t == "habit" -> ROUTINE_REMINDER
                else -> SYSTEM
            }
        }
    }
}
