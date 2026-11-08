package com.twinhearts.app.core.notifications

import androidx.annotation.StringRes
import com.twinhearts.app.R

/** Default title and body for each type (used when a push sends none). */
object NotificationDefaults {
    @StringRes fun title(type: NotificationType): Int = when (type) {
        NotificationType.CHAT_MESSAGE -> R.string.notif_d_chat_message_title
        NotificationType.CHAT_REACTION -> R.string.notif_d_chat_reaction_title
        NotificationType.CALL_INCOMING -> R.string.notif_d_call_incoming_title
        NotificationType.CALL_MISSED -> R.string.notif_d_call_missed_title
        NotificationType.PARTNER_HUG -> R.string.notif_d_partner_hug_title
        NotificationType.PARTNER_MISS -> R.string.notif_d_partner_miss_title
        NotificationType.PARTNER_TOUCH -> R.string.notif_d_partner_touch_title
        NotificationType.SURPRISE_NEW -> R.string.notif_d_surprise_new_title
        NotificationType.SURPRISE_UNLOCKED -> R.string.notif_d_surprise_unlocked_title
        NotificationType.ANNIVERSARY -> R.string.notif_d_anniversary_title
        NotificationType.MILESTONE -> R.string.notif_d_milestone_title
        NotificationType.MEMORY_ADDED -> R.string.notif_d_memory_added_title
        NotificationType.MEMORY_THROWBACK -> R.string.notif_d_memory_throwback_title
        NotificationType.BUCKET_ADDED -> R.string.notif_d_bucket_added_title
        NotificationType.BUCKET_DONE -> R.string.notif_d_bucket_done_title
        NotificationType.LOCATION_ARRIVED -> R.string.notif_d_location_arrived_title
        NotificationType.LOCATION_LEFT -> R.string.notif_d_location_left_title
        NotificationType.LOCATION_OFFLINE -> R.string.notif_d_location_offline_title
        NotificationType.LOCATION_SHARING -> R.string.notif_d_location_sharing_title
        NotificationType.LOCATION_SOS -> R.string.notif_d_location_sos_title
        NotificationType.MEET_PLANNER -> R.string.notif_d_meet_planner_title
        NotificationType.CALENDAR_REMINDER -> R.string.notif_d_calendar_reminder_title
        NotificationType.CALENDAR_EVENT -> R.string.notif_d_calendar_event_title
        NotificationType.STUDY_REMINDER -> R.string.notif_d_study_reminder_title
        NotificationType.ROUTINE_REMINDER -> R.string.notif_d_routine_reminder_title
        NotificationType.PERIOD_REMINDER -> R.string.notif_d_period_reminder_title
        NotificationType.MONEY_UPDATE -> R.string.notif_d_money_update_title
        NotificationType.GAME_INVITE -> R.string.notif_d_game_invite_title
        NotificationType.MOVIE_INVITE -> R.string.notif_d_movie_invite_title
        NotificationType.CINEMA_INVITE -> R.string.notif_d_cinema_invite_title
        NotificationType.MUSIC_SHARED -> R.string.notif_d_music_shared_title
        NotificationType.FIGHT_LOGGED -> R.string.notif_d_fight_logged_title
        NotificationType.FIGHT_RESOLVED -> R.string.notif_d_fight_resolved_title
        NotificationType.VAULT_ACTIVITY -> R.string.notif_d_vault_activity_title
        NotificationType.PARTNER_REQUEST -> R.string.notif_d_partner_request_title
        NotificationType.PARTNER_ACCEPTED -> R.string.notif_d_partner_accepted_title
        NotificationType.PARTNER_REMOVED -> R.string.notif_d_partner_removed_title
        NotificationType.LEVEL_UP -> R.string.notif_d_level_up_title
        NotificationType.PRO_UPDATE -> R.string.notif_d_pro_update_title
        NotificationType.SYSTEM -> R.string.notif_d_system_title
    }

    @StringRes fun body(type: NotificationType): Int = when (type) {
        NotificationType.CHAT_MESSAGE -> R.string.notif_d_chat_message_body
        NotificationType.CHAT_REACTION -> R.string.notif_d_chat_reaction_body
        NotificationType.CALL_INCOMING -> R.string.notif_d_call_incoming_body
        NotificationType.CALL_MISSED -> R.string.notif_d_call_missed_body
        NotificationType.PARTNER_HUG -> R.string.notif_d_partner_hug_body
        NotificationType.PARTNER_MISS -> R.string.notif_d_partner_miss_body
        NotificationType.PARTNER_TOUCH -> R.string.notif_d_partner_touch_body
        NotificationType.SURPRISE_NEW -> R.string.notif_d_surprise_new_body
        NotificationType.SURPRISE_UNLOCKED -> R.string.notif_d_surprise_unlocked_body
        NotificationType.ANNIVERSARY -> R.string.notif_d_anniversary_body
        NotificationType.MILESTONE -> R.string.notif_d_milestone_body
        NotificationType.MEMORY_ADDED -> R.string.notif_d_memory_added_body
        NotificationType.MEMORY_THROWBACK -> R.string.notif_d_memory_throwback_body
        NotificationType.BUCKET_ADDED -> R.string.notif_d_bucket_added_body
        NotificationType.BUCKET_DONE -> R.string.notif_d_bucket_done_body
        NotificationType.LOCATION_ARRIVED -> R.string.notif_d_location_arrived_body
        NotificationType.LOCATION_LEFT -> R.string.notif_d_location_left_body
        NotificationType.LOCATION_OFFLINE -> R.string.notif_d_location_offline_body
        NotificationType.LOCATION_SHARING -> R.string.notif_d_location_sharing_body
        NotificationType.LOCATION_SOS -> R.string.notif_d_location_sos_body
        NotificationType.MEET_PLANNER -> R.string.notif_d_meet_planner_body
        NotificationType.CALENDAR_REMINDER -> R.string.notif_d_calendar_reminder_body
        NotificationType.CALENDAR_EVENT -> R.string.notif_d_calendar_event_body
        NotificationType.STUDY_REMINDER -> R.string.notif_d_study_reminder_body
        NotificationType.ROUTINE_REMINDER -> R.string.notif_d_routine_reminder_body
        NotificationType.PERIOD_REMINDER -> R.string.notif_d_period_reminder_body
        NotificationType.MONEY_UPDATE -> R.string.notif_d_money_update_body
        NotificationType.GAME_INVITE -> R.string.notif_d_game_invite_body
        NotificationType.MOVIE_INVITE -> R.string.notif_d_movie_invite_body
        NotificationType.CINEMA_INVITE -> R.string.notif_d_cinema_invite_body
        NotificationType.MUSIC_SHARED -> R.string.notif_d_music_shared_body
        NotificationType.FIGHT_LOGGED -> R.string.notif_d_fight_logged_body
        NotificationType.FIGHT_RESOLVED -> R.string.notif_d_fight_resolved_body
        NotificationType.VAULT_ACTIVITY -> R.string.notif_d_vault_activity_body
        NotificationType.PARTNER_REQUEST -> R.string.notif_d_partner_request_body
        NotificationType.PARTNER_ACCEPTED -> R.string.notif_d_partner_accepted_body
        NotificationType.PARTNER_REMOVED -> R.string.notif_d_partner_removed_body
        NotificationType.LEVEL_UP -> R.string.notif_d_level_up_body
        NotificationType.PRO_UPDATE -> R.string.notif_d_pro_update_body
        NotificationType.SYSTEM -> R.string.notif_d_system_body
    }
}
