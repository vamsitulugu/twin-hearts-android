package com.twinhearts.app.feature.search.domain

import android.content.Context
import androidx.annotation.StringRes
import com.twinhearts.app.R
import com.twinhearts.app.feature.search.domain.model.SearchDestination
import com.twinhearts.app.feature.search.domain.model.SearchDoc
import com.twinhearts.app.feature.search.domain.model.SearchSection

/**
 * Why this file exists: the list of every page, sub-page and feature of the app, so Search can
 * find them by name or by what a person would call them (the keywords). Names reuse the labels
 * already shown on Home and in Settings, so a result reads exactly like the screen it opens.
 *
 * Sub-pages open their owning page: pages that have inner tabs keep that state private, so the
 * result lands on the page and the person taps the tab.
 */
internal data class CatalogEntry(
    val id: String,
    val section: SearchSection,
    val destination: SearchDestination,
    val emoji: String,
    @StringRes val title: Int,
    /** The page this feature lives in (its title), or null for a page itself. */
    @StringRes val where: Int?,
    @StringRes val keywords: Int,
)

internal object SearchCatalog {

    /** Resolved for the current app language, so a language change never leaves stale text. */
    fun docs(context: Context): List<SearchDoc> = entries.map { e ->
        SearchDoc(
            key = "catalog:${e.id}",
            section = e.section,
            title = context.getString(e.title),
            detail = "",
            where = e.where?.let { context.getString(it) }.orEmpty(),
            emoji = e.emoji,
            destination = e.destination,
            keywords = context.getString(e.keywords) +
                (e.where?.let { " " + context.getString(it) }.orEmpty()),
        )
    }

    /** Short, useful first ideas shown on the empty search page. */
    val suggestionTitles: List<Int> = listOf(
        R.string.home_page_calendar,
        R.string.home_chip_music,
        R.string.home_page_money,
        R.string.settings_notifications,
        R.string.home_page_games,
    )

    private val entries = listOf(
    CatalogEntry("home", SearchSection.Pages, SearchDestination.Home, "🏠", R.string.core_nav_home, null, R.string.search_k_home),
    CatalogEntry("chat", SearchSection.Pages, SearchDestination.Chat, "💬", R.string.core_nav_chat, null, R.string.search_k_chat),
    CatalogEntry("twinai", SearchSection.Pages, SearchDestination.TwinAi, "✨", R.string.search_t_twinai, null, R.string.search_k_twinai),
    CatalogEntry("livemap", SearchSection.Pages, SearchDestination.LiveMap, "🗺️", R.string.search_t_livemap, null, R.string.search_k_livemap),
    CatalogEntry("settings", SearchSection.Pages, SearchDestination.Settings, "⚙️", R.string.core_nav_settings, null, R.string.search_k_settings),
    CatalogEntry("profile", SearchSection.Pages, SearchDestination.Profile, "👫", R.string.home_page_profile, null, R.string.search_k_profile),
    CatalogEntry("lovecounter", SearchSection.Pages, SearchDestination.LoveCounter, "❤️", R.string.home_page_love_counter, null, R.string.search_k_lovecounter),
    CatalogEntry("level", SearchSection.Pages, SearchDestination.Level, "🏆", R.string.home_page_level, null, R.string.search_k_level),
    CatalogEntry("fights", SearchSection.Pages, SearchDestination.Fights, "⚖️", R.string.home_page_fights, null, R.string.search_k_fights),
    CatalogEntry("dreamgoals", SearchSection.Pages, SearchDestination.DreamGoals, "🎯", R.string.home_page_dream_goals, null, R.string.search_k_dreamgoals),
    CatalogEntry("globe", SearchSection.Pages, SearchDestination.Globe, "🌍", R.string.home_page_globe, null, R.string.search_k_globe),
    CatalogEntry("places", SearchSection.Pages, SearchDestination.Places, "📍", R.string.home_page_places, null, R.string.search_k_places),
    CatalogEntry("collection", SearchSection.Pages, SearchDestination.Collection, "🎁", R.string.home_page_collection, null, R.string.search_k_collection),
    CatalogEntry("games", SearchSection.Pages, SearchDestination.Games, "🎮", R.string.home_page_games, null, R.string.search_k_games),
    CatalogEntry("study", SearchSection.Pages, SearchDestination.Study, "📚", R.string.home_page_study, null, R.string.search_k_study),
    CatalogEntry("myspace", SearchSection.Pages, SearchDestination.MySpace, "🌸", R.string.home_page_my_space, null, R.string.search_k_myspace),
    CatalogEntry("period", SearchSection.Pages, SearchDestination.PeriodTracker, "💧", R.string.home_page_period, null, R.string.search_k_period),
    CatalogEntry("vault", SearchSection.Pages, SearchDestination.Vault, "🔒", R.string.home_page_vault, null, R.string.search_k_vault),
    CatalogEntry("help", SearchSection.Pages, SearchDestination.Help, "🛟", R.string.home_page_help, null, R.string.search_k_help),
    CatalogEntry("surprises", SearchSection.Pages, SearchDestination.Surprises, "🎉", R.string.home_page_surprises, null, R.string.search_k_surprises),
    CatalogEntry("money", SearchSection.Pages, SearchDestination.Money, "💰", R.string.home_page_money, null, R.string.search_k_money),
    CatalogEntry("calendar", SearchSection.Pages, SearchDestination.Calendar, "📅", R.string.home_page_calendar, null, R.string.search_k_calendar),
    CatalogEntry("bucket", SearchSection.Pages, SearchDestination.BucketList, "✅", R.string.home_page_bucket_list, null, R.string.search_k_bucket),
    CatalogEntry("memories", SearchSection.Pages, SearchDestination.Memories, "📸", R.string.home_chip_memories, null, R.string.search_k_memories),
    CatalogEntry("music", SearchSection.Pages, SearchDestination.Music, "🎵", R.string.home_chip_music, null, R.string.search_k_music),
    CatalogEntry("movie", SearchSection.Pages, SearchDestination.Movie, "🎬", R.string.search_t_movie, null, R.string.search_k_movie),
    CatalogEntry("meet", SearchSection.Pages, SearchDestination.MeetPlanner, "🧭", R.string.home_chip_meet_planner, null, R.string.search_k_meet),
    CatalogEntry("routine", SearchSection.Pages, SearchDestination.DailyRoutine, "🌤️", R.string.home_chip_daily_routine, null, R.string.search_k_routine),
    CatalogEntry("pro", SearchSection.Pages, SearchDestination.Pro, "⭐", R.string.search_t_pro, null, R.string.search_k_pro),
    CatalogEntry("set_profile", SearchSection.Features, SearchDestination.Settings, "🙂", R.string.settings_profile, R.string.core_nav_settings, R.string.search_k_set_profile),
    CatalogEntry("set_partner", SearchSection.Features, SearchDestination.Settings, "🔗", R.string.settings_partner, R.string.core_nav_settings, R.string.search_k_set_partner),
    CatalogEntry("set_notifications", SearchSection.Features, SearchDestination.Settings, "🔔", R.string.settings_notifications, R.string.core_nav_settings, R.string.search_k_set_notifications),
    CatalogEntry("set_appearance", SearchSection.Features, SearchDestination.Settings, "🎨", R.string.settings_appearance, R.string.core_nav_settings, R.string.search_k_set_appearance),
    CatalogEntry("set_chatmedia", SearchSection.Features, SearchDestination.Settings, "🖼️", R.string.settings_chatmedia, R.string.core_nav_settings, R.string.search_k_set_chatmedia),
    CatalogEntry("set_twin", SearchSection.Features, SearchDestination.Settings, "🤖", R.string.search_t_set_twin, R.string.core_nav_settings, R.string.search_k_set_twin),
    CatalogEntry("set_privacy", SearchSection.Features, SearchDestination.Settings, "🛡️", R.string.settings_privacy, R.string.core_nav_settings, R.string.search_k_set_privacy),
    CatalogEntry("set_storage", SearchSection.Features, SearchDestination.Settings, "💾", R.string.settings_storage, R.string.core_nav_settings, R.string.search_k_set_storage),
    CatalogEntry("set_help", SearchSection.Features, SearchDestination.Help, "🛟", R.string.settings_help, R.string.core_nav_settings, R.string.search_k_set_help),
    CatalogEntry("set_about", SearchSection.Features, SearchDestination.Settings, "ℹ️", R.string.settings_about, R.string.core_nav_settings, R.string.search_k_set_about),
    CatalogEntry("set_logout", SearchSection.Features, SearchDestination.Settings, "🚪", R.string.settings_logout, R.string.core_nav_settings, R.string.search_k_set_logout),
    CatalogEntry("home_hug", SearchSection.Features, SearchDestination.Home, "🤗", R.string.search_t_home_hug, R.string.core_nav_home, R.string.search_k_home_hug),
    CatalogEntry("home_touch", SearchSection.Features, SearchDestination.Home, "👆", R.string.search_t_home_touch, R.string.core_nav_home, R.string.search_k_home_touch),
    CatalogEntry("home_miss", SearchSection.Features, SearchDestination.Home, "🥺", R.string.search_t_home_miss, R.string.core_nav_home, R.string.search_k_home_miss),
    CatalogEntry("home_mood", SearchSection.Features, SearchDestination.Home, "😊", R.string.search_t_home_mood, R.string.core_nav_home, R.string.search_k_home_mood),
    CatalogEntry("home_moodcal", SearchSection.Features, SearchDestination.Home, "🗓️", R.string.search_t_home_moodcal, R.string.core_nav_home, R.string.search_k_home_moodcal),
    CatalogEntry("home_coming", SearchSection.Features, SearchDestination.Home, "⏳", R.string.search_t_home_coming, R.string.core_nav_home, R.string.search_k_home_coming),
    CatalogEntry("home_cardphotos", SearchSection.Features, SearchDestination.Home, "🖼️", R.string.search_t_home_cardphotos, R.string.core_nav_home, R.string.search_k_home_cardphotos),
    CatalogEntry("chat_voice", SearchSection.Features, SearchDestination.Chat, "🎙️", R.string.search_t_chat_voice, R.string.core_nav_chat, R.string.search_k_chat_voice),
    CatalogEntry("chat_media", SearchSection.Features, SearchDestination.Chat, "📷", R.string.search_t_chat_media, R.string.core_nav_chat, R.string.search_k_chat_media),
    CatalogEntry("chat_poll", SearchSection.Features, SearchDestination.Chat, "📊", R.string.search_t_chat_poll, R.string.core_nav_chat, R.string.search_k_chat_poll),
    CatalogEntry("chat_react", SearchSection.Features, SearchDestination.Chat, "😍", R.string.search_t_chat_react, R.string.core_nav_chat, R.string.search_k_chat_react),
    CatalogEntry("chat_pin", SearchSection.Features, SearchDestination.Chat, "📌", R.string.search_t_chat_pin, R.string.core_nav_chat, R.string.search_k_chat_pin),
    CatalogEntry("chat_star", SearchSection.Features, SearchDestination.Chat, "⭐", R.string.search_t_chat_star, R.string.core_nav_chat, R.string.search_k_chat_star),
    CatalogEntry("chat_schedule", SearchSection.Features, SearchDestination.Chat, "⏰", R.string.search_t_chat_schedule, R.string.core_nav_chat, R.string.search_k_chat_schedule),
    CatalogEntry("chat_wallpaper", SearchSection.Features, SearchDestination.Chat, "🌄", R.string.search_t_chat_wallpaper, R.string.core_nav_chat, R.string.search_k_chat_wallpaper),
    CatalogEntry("chat_disappear", SearchSection.Features, SearchDestination.Chat, "💨", R.string.search_t_chat_disappear, R.string.core_nav_chat, R.string.search_k_chat_disappear),
    CatalogEntry("chat_export", SearchSection.Features, SearchDestination.Chat, "📤", R.string.search_t_chat_export, R.string.core_nav_chat, R.string.search_k_chat_export),
    CatalogEntry("chat_lock", SearchSection.Features, SearchDestination.Chat, "🔐", R.string.search_t_chat_lock, R.string.core_nav_chat, R.string.search_k_chat_lock),
    CatalogEntry("chat_search", SearchSection.Features, SearchDestination.Chat, "🔎", R.string.search_t_chat_search, R.string.core_nav_chat, R.string.search_k_chat_search),
    CatalogEntry("chat_effects", SearchSection.Features, SearchDestination.Chat, "🎊", R.string.search_t_chat_effects, R.string.core_nav_chat, R.string.search_k_chat_effects),
    CatalogEntry("chat_gif", SearchSection.Features, SearchDestination.Chat, "🖼️", R.string.search_t_chat_gif, R.string.core_nav_chat, R.string.search_k_chat_gif),
    CatalogEntry("chat_edit", SearchSection.Features, SearchDestination.Chat, "✏️", R.string.search_t_chat_edit, R.string.core_nav_chat, R.string.search_k_chat_edit),
    CatalogEntry("chat_voicecall", SearchSection.Features, SearchDestination.Chat, "📞", R.string.search_t_chat_voicecall, R.string.core_nav_chat, R.string.search_k_chat_voicecall),
    CatalogEntry("chat_videocall", SearchSection.Features, SearchDestination.Chat, "📹", R.string.search_t_chat_videocall, R.string.core_nav_chat, R.string.search_k_chat_videocall),
    CatalogEntry("music_lib", SearchSection.Features, SearchDestination.Music, "📚", R.string.search_t_music_lib, R.string.home_chip_music, R.string.search_k_music_lib),
    CatalogEntry("music_now", SearchSection.Features, SearchDestination.Music, "▶️", R.string.search_t_music_now, R.string.home_chip_music, R.string.search_k_music_now),
    CatalogEntry("music_lyrics", SearchSection.Features, SearchDestination.Music, "📝", R.string.search_t_music_lyrics, R.string.home_chip_music, R.string.search_k_music_lyrics),
    CatalogEntry("music_sing", SearchSection.Features, SearchDestination.Music, "🎤", R.string.search_t_music_sing, R.string.home_chip_music, R.string.search_k_music_sing),
    CatalogEntry("music_rec", SearchSection.Features, SearchDestination.Music, "🎧", R.string.search_t_music_rec, R.string.home_chip_music, R.string.search_k_music_rec),
    CatalogEntry("music_mix", SearchSection.Features, SearchDestination.Music, "🔀", R.string.search_t_music_mix, R.string.home_chip_music, R.string.search_k_music_mix),
    CatalogEntry("music_offline", SearchSection.Features, SearchDestination.Music, "⬇️", R.string.search_t_music_offline, R.string.home_chip_music, R.string.search_k_music_offline),
    CatalogEntry("music_sound", SearchSection.Features, SearchDestination.Music, "🎚️", R.string.search_t_music_sound, R.string.home_chip_music, R.string.search_k_music_sound),
    CatalogEntry("movie_add", SearchSection.Features, SearchDestination.Movie, "➕", R.string.search_t_movie_add, R.string.search_t_movie, R.string.search_k_movie_add),
    CatalogEntry("movie_sub", SearchSection.Features, SearchDestination.Movie, "💬", R.string.search_t_movie_sub, R.string.search_t_movie, R.string.search_k_movie_sub),
    CatalogEntry("meet_plan", SearchSection.Features, SearchDestination.MeetPlanner, "🗓️", R.string.search_t_meet_plan, R.string.home_chip_meet_planner, R.string.search_k_meet_plan),
    CatalogEntry("meet_saved", SearchSection.Features, SearchDestination.MeetPlanner, "💾", R.string.search_t_meet_saved, R.string.home_chip_meet_planner, R.string.search_k_meet_saved),
    CatalogEntry("meet_route", SearchSection.Features, SearchDestination.MeetPlanner, "🛣️", R.string.search_t_meet_route, R.string.home_chip_meet_planner, R.string.search_k_meet_route),
    CatalogEntry("meet_search", SearchSection.Features, SearchDestination.MeetPlanner, "🔍", R.string.search_t_meet_search, R.string.home_chip_meet_planner, R.string.search_k_meet_search),
    CatalogEntry("map_map", SearchSection.Features, SearchDestination.LiveMap, "📍", R.string.search_t_map_map, R.string.search_t_livemap, R.string.search_k_map_map),
    CatalogEntry("map_track", SearchSection.Features, SearchDestination.LiveMap, "🧵", R.string.search_t_map_track, R.string.search_t_livemap, R.string.search_k_map_track),
    CatalogEntry("map_partner", SearchSection.Features, SearchDestination.LiveMap, "💑", R.string.search_t_map_partner, R.string.search_t_livemap, R.string.search_k_map_partner),
    CatalogEntry("map_places", SearchSection.Features, SearchDestination.LiveMap, "📌", R.string.search_t_map_places, R.string.search_t_livemap, R.string.search_k_map_places),
    CatalogEntry("map_share", SearchSection.Features, SearchDestination.LiveMap, "📡", R.string.search_t_map_share, R.string.search_t_livemap, R.string.search_k_map_share),
    CatalogEntry("map_nav", SearchSection.Features, SearchDestination.LiveMap, "🧭", R.string.search_t_map_nav, R.string.search_t_livemap, R.string.search_k_map_nav),
    CatalogEntry("game_ludo", SearchSection.Features, SearchDestination.Games, "🎲", R.string.search_t_game_ludo, R.string.home_page_games, R.string.search_k_game_ludo),
    CatalogEntry("game_ttt", SearchSection.Features, SearchDestination.Games, "❌", R.string.search_t_game_ttt, R.string.home_page_games, R.string.search_k_game_ttt),
    CatalogEntry("game_tod", SearchSection.Features, SearchDestination.Games, "🍾", R.string.search_t_game_tod, R.string.home_page_games, R.string.search_k_game_tod),
    CatalogEntry("game_memory", SearchSection.Features, SearchDestination.Games, "🃏", R.string.search_t_game_memory, R.string.home_page_games, R.string.search_k_game_memory),
    CatalogEntry("game_mind", SearchSection.Features, SearchDestination.Games, "🔮", R.string.search_t_game_mind, R.string.home_page_games, R.string.search_k_game_mind),
    CatalogEntry("game_puzzle", SearchSection.Features, SearchDestination.Games, "🧩", R.string.search_t_game_puzzle, R.string.home_page_games, R.string.search_k_game_puzzle),
    CatalogEntry("game_draw", SearchSection.Features, SearchDestination.Games, "🖍️", R.string.search_t_game_draw, R.string.home_page_games, R.string.search_k_game_draw),
    CatalogEntry("game_bomb", SearchSection.Features, SearchDestination.Games, "💣", R.string.search_t_game_bomb, R.string.home_page_games, R.string.search_k_game_bomb),
    CatalogEntry("cal_add", SearchSection.Features, SearchDestination.Calendar, "➕", R.string.search_t_cal_add, R.string.home_page_calendar, R.string.search_k_cal_add),
    CatalogEntry("money_add", SearchSection.Features, SearchDestination.Money, "💸", R.string.search_t_money_add, R.string.home_page_money, R.string.search_k_money_add),
    CatalogEntry("money_charts", SearchSection.Features, SearchDestination.Money, "📈", R.string.search_t_money_charts, R.string.home_page_money, R.string.search_k_money_charts),
    CatalogEntry("money_goals", SearchSection.Features, SearchDestination.Money, "🏦", R.string.search_t_money_goals, R.string.home_page_money, R.string.search_k_money_goals),
    CatalogEntry("bucket_add", SearchSection.Features, SearchDestination.BucketList, "➕", R.string.search_t_bucket_add, R.string.home_page_bucket_list, R.string.search_k_bucket_add),
    CatalogEntry("period_log", SearchSection.Features, SearchDestination.PeriodTracker, "📝", R.string.search_t_period_log, R.string.home_page_period, R.string.search_k_period_log),
    CatalogEntry("surprise_new", SearchSection.Features, SearchDestination.Surprises, "🎁", R.string.search_t_surprise_new, R.string.home_page_surprises, R.string.search_k_surprise_new),
    CatalogEntry("surprise_capsule", SearchSection.Features, SearchDestination.Surprises, "💌", R.string.search_t_surprise_capsule, R.string.home_page_surprises, R.string.search_k_surprise_capsule),
    CatalogEntry("routine_add", SearchSection.Features, SearchDestination.DailyRoutine, "➕", R.string.search_t_routine_add, R.string.home_chip_daily_routine, R.string.search_k_routine_add),
    CatalogEntry("space_notes", SearchSection.Features, SearchDestination.MySpace, "🗒️", R.string.search_t_space_notes, R.string.home_page_my_space, R.string.search_k_space_notes),
    CatalogEntry("space_journal", SearchSection.Features, SearchDestination.MySpace, "📔", R.string.search_t_space_journal, R.string.home_page_my_space, R.string.search_k_space_journal),
    CatalogEntry("space_habits", SearchSection.Features, SearchDestination.MySpace, "🔁", R.string.search_t_space_habits, R.string.home_page_my_space, R.string.search_k_space_habits),
    CatalogEntry("space_reminders", SearchSection.Features, SearchDestination.MySpace, "⏰", R.string.search_t_space_reminders, R.string.home_page_my_space, R.string.search_k_space_reminders),
    CatalogEntry("space_wellness", SearchSection.Features, SearchDestination.MySpace, "🧘", R.string.search_t_space_wellness, R.string.home_page_my_space, R.string.search_k_space_wellness),
    CatalogEntry("help_faq", SearchSection.Features, SearchDestination.Help, "❓", R.string.search_t_help_faq, R.string.home_page_help, R.string.search_k_help_faq),
    CatalogEntry("help_ai", SearchSection.Features, SearchDestination.Help, "🤖", R.string.search_t_help_ai, R.string.home_page_help, R.string.search_k_help_ai),
    CatalogEntry("help_agent", SearchSection.Features, SearchDestination.Help, "🎧", R.string.search_t_help_agent, R.string.home_page_help, R.string.search_k_help_agent),
    CatalogEntry("globe_timeline", SearchSection.Features, SearchDestination.Globe, "🕰️", R.string.search_t_globe_timeline, R.string.home_page_globe, R.string.search_k_globe_timeline),
    CatalogEntry("globe_stats", SearchSection.Features, SearchDestination.Globe, "📊", R.string.search_t_globe_stats, R.string.home_page_globe, R.string.search_k_globe_stats),
    CatalogEntry("globe_add", SearchSection.Features, SearchDestination.Globe, "➕", R.string.search_t_globe_add, R.string.home_page_globe, R.string.search_k_globe_add),
    CatalogEntry("fight_log", SearchSection.Features, SearchDestination.Fights, "📝", R.string.search_t_fight_log, R.string.home_page_fights, R.string.search_k_fight_log),
    CatalogEntry("memory_fav", SearchSection.Features, SearchDestination.Memories, "❤️", R.string.search_t_memory_fav, R.string.home_chip_memories, R.string.search_k_memory_fav),
    )
}
