package com.twinhearts.app.feature.home.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.twinhearts.app.R

/**
 * Everything on Home that opens another page. Only the ones whose screen already exists
 * navigate; the rest are handled in HomeRoute (they get real screens in later steps).
 */
enum class HomeShortcut(@StringRes val labelRes: Int, val icon: ImageVector) {
    Search(R.string.home_search_hint, Icons.Rounded.Search),
    Menu(R.string.core_menu, Icons.Rounded.GridView),

    // Chips under the search bar
    Memories(R.string.home_chip_memories, Icons.Rounded.PhotoCamera),
    Music(R.string.home_chip_music, Icons.Rounded.MusicNote),
    Movie(R.string.home_chip_movie, Icons.Rounded.Movie),
    MeetPlanner(R.string.home_chip_meet_planner, Icons.Rounded.Navigation),
    DailyRoutine(R.string.home_chip_daily_routine, Icons.Rounded.WbSunny),

    // All pages: Us
    OurProfile(R.string.home_page_profile, Icons.Rounded.Group),
    LoveCounter(R.string.home_page_love_counter, Icons.Rounded.Favorite),
    Level(R.string.home_page_level, Icons.Rounded.EmojiEvents),
    FightLog(R.string.home_page_fights, Icons.Rounded.Gavel),
    DreamGoals(R.string.home_page_dream_goals, Icons.Rounded.TrackChanges),

    // Explore Together
    Globe(R.string.home_page_globe, Icons.Rounded.Public),
    Places(R.string.home_page_places, Icons.Rounded.Place),
    Collection(R.string.home_page_collection, Icons.Rounded.CardGiftcard),
    Games(R.string.home_page_games, Icons.Rounded.SportsEsports),
    Study(R.string.home_page_study, Icons.Rounded.MenuBook),

    // My Space
    MySpace(R.string.home_page_my_space, Icons.Rounded.LocalFlorist),
    PeriodTracker(R.string.home_page_period, Icons.Rounded.WaterDrop),
    Vault(R.string.home_page_vault, Icons.Rounded.Lock),
    Help(R.string.home_page_help, Icons.Rounded.Help),

    // Plans
    Surprises(R.string.home_page_surprises, Icons.Rounded.Celebration),
    Money(R.string.home_page_money, Icons.Rounded.AccountBalanceWallet),
    Calendar(R.string.home_page_calendar, Icons.Rounded.CalendarMonth),
    BucketList(R.string.home_page_bucket_list, Icons.Rounded.Checklist),
}

internal data class HomeGroup(@StringRes val titleRes: Int, val items: List<HomeShortcut>)

/** Same groups and order as the web "all pages" list. */
internal val homeGroups = listOf(
    HomeGroup(
        R.string.home_group_us,
        listOf(HomeShortcut.OurProfile, HomeShortcut.LoveCounter, HomeShortcut.Level, HomeShortcut.FightLog, HomeShortcut.DreamGoals),
    ),
    HomeGroup(
        R.string.home_group_explore,
        listOf(HomeShortcut.Globe, HomeShortcut.Places, HomeShortcut.Collection, HomeShortcut.Games, HomeShortcut.Study),
    ),
    HomeGroup(
        R.string.home_group_my_space,
        listOf(HomeShortcut.MySpace, HomeShortcut.PeriodTracker, HomeShortcut.Vault, HomeShortcut.Help),
    ),
    HomeGroup(
        R.string.home_group_plans,
        listOf(HomeShortcut.Surprises, HomeShortcut.Money, HomeShortcut.Calendar, HomeShortcut.BucketList),
    ),
)

internal val homeChips = listOf(
    HomeShortcut.Memories, HomeShortcut.Music, HomeShortcut.Movie, HomeShortcut.MeetPlanner, HomeShortcut.DailyRoutine,
)
