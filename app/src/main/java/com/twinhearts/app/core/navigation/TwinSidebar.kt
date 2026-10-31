package com.twinhearts.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.twinGlass
import com.twinhearts.app.core.design.twinPage
import com.twinhearts.app.core.design.twinWine

/** One row of the side menu. [key] is the route the row opens. */
@Immutable
internal data class SidebarEntry(
    val key: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

@Immutable
internal data class SidebarGroup(@StringRes val titleRes: Int, val entries: List<SidebarEntry>)

/** Same order and names as the web sidebar + the "all pages" groups on Home. */
internal val sidebarGroups = listOf(
    SidebarGroup(
        R.string.side_group_home,
        listOf(SidebarEntry(Routes.Home, R.string.core_nav_home, Icons.Outlined.Home)),
    ),
    SidebarGroup(
        R.string.side_group_main,
        listOf(
            SidebarEntry(Routes.Chat, R.string.core_nav_chat, Icons.Outlined.ChatBubbleOutline),
            SidebarEntry(Routes.LiveMap, R.string.side_live_map, Icons.Outlined.Place),
            SidebarEntry(Routes.TwinAi, R.string.core_nav_twin, Icons.Rounded.AutoAwesome),
        ),
    ),
    SidebarGroup(
        R.string.side_group_together,
        listOf(
            SidebarEntry(Routes.Music, R.string.home_chip_music, Icons.Outlined.MusicNote),
            SidebarEntry(Routes.Movie, R.string.home_chip_movie, Icons.Outlined.MovieCreation),
            SidebarEntry(Routes.MeetPlanner, R.string.home_chip_meet_planner, Icons.Outlined.Navigation),
            SidebarEntry(Routes.DailyRoutine, R.string.home_chip_daily_routine, Icons.Outlined.WbSunny),
        ),
    ),
    SidebarGroup(
        R.string.home_group_us,
        listOf(
            SidebarEntry(Routes.Profile, R.string.home_page_profile, Icons.Outlined.Group),
            SidebarEntry(Routes.Us(tab = 0), R.string.home_page_love_counter, Icons.Outlined.FavoriteBorder),
            SidebarEntry(Routes.Level, R.string.home_page_level, Icons.Outlined.EmojiEvents),
            SidebarEntry(Routes.Fights, R.string.home_page_fights, Icons.Outlined.Gavel),
            SidebarEntry(Routes.Us(tab = 2), R.string.home_page_dream_goals, Icons.Outlined.TrackChanges),
        ),
    ),
    SidebarGroup(
        R.string.home_group_explore,
        listOf(
            SidebarEntry(Routes.Globe, R.string.home_page_globe, Icons.Outlined.Public),
            SidebarEntry(Routes.Places, R.string.home_page_places, Icons.Outlined.Place),
            SidebarEntry(Routes.Collection, R.string.home_page_collection, Icons.Outlined.CardGiftcard),
            SidebarEntry(Routes.Games, R.string.home_page_games, Icons.Outlined.SportsEsports),
            SidebarEntry(Routes.Study, R.string.home_page_study, Icons.Outlined.MenuBook),
        ),
    ),
    SidebarGroup(
        R.string.home_group_my_space,
        listOf(
            SidebarEntry(Routes.MySpace, R.string.home_page_my_space, Icons.Outlined.LocalFlorist),
            SidebarEntry(Routes.PeriodTracker, R.string.home_page_period, Icons.Outlined.WaterDrop),
            SidebarEntry(Routes.Vault, R.string.home_page_vault, Icons.Outlined.Lock),
            SidebarEntry(Routes.Help, R.string.home_page_help, Icons.Outlined.HelpOutline),
        ),
    ),
    SidebarGroup(
        R.string.home_group_plans,
        listOf(
            SidebarEntry(Routes.Surprises, R.string.home_page_surprises, Icons.Outlined.Celebration),
            SidebarEntry(Routes.Money, R.string.home_page_money, Icons.Outlined.AccountBalanceWallet),
            SidebarEntry(Routes.Calendar, R.string.home_page_calendar, Icons.Outlined.CalendarMonth),
            SidebarEntry(Routes.BucketList, R.string.home_page_bucket_list, Icons.Outlined.Checklist),
        ),
    ),
    SidebarGroup(
        R.string.side_group_pro,
        listOf(SidebarEntry(Routes.Pro(), R.string.side_pro, Icons.Outlined.WorkspacePremium)),
    ),
)

private val settingsEntry = SidebarEntry(Routes.Settings, R.string.core_nav_settings, Icons.Outlined.Settings)

/** Every route that has a row in the side menu, so TwinApp can tell which row is active. */
internal val sidebarRouteKeys: List<Any> =
    (sidebarGroups.flatMap { it.entries } + settingsEntry).map { it.key }

/**
 * The slide-in side menu (web: `aside.sidebar`): logo, couple widget with sync state, grouped page
 * rows, Settings pinned at the bottom. Dark burgundy gradient like `dashboard-burgundy.css`.
 */
@Composable
internal fun TwinSidebar(
    session: Session?,
    selectedKey: Any?,
    onNavigate: (SidebarEntry) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    // Several rows can share one route (Love Counter / Dream Goals -> Us): light up only the first.
    val activeLabel = remember(selectedKey) {
        (sidebarGroups.flatMap { it.entries } + settingsEntry).firstOrNull { it.key == selectedKey }?.labelRes
    }
    // Same page look as Movie, Music and Home: near-black with the soft wine glow at the top.
    Column(
        modifier = modifier
            .twinPage()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            SidebarHeader()
            Spacer(Modifier.height(14.dp))
            CoupleWidget(session = session, onClick = onOpenProfile)
            sidebarGroups.forEach { group ->
                Text(
                    text = stringResource(group.titleRes).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 6.dp),
                )
                group.entries.forEach { entry ->
                    SidebarRow(entry, active = entry.labelRes == activeLabel, onClick = { onNavigate(entry) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.lineSoft),
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
        ) {
            SidebarRow(settingsEntry, active = settingsEntry.labelRes == activeLabel, onClick = { onNavigate(settingsEntry) })
        }
    }
}

@Composable
private fun SidebarHeader() {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.padding(start = 2.dp, top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Image(
                painter = painterResource(R.drawable.core_logo),
                contentDescription = stringResource(R.string.core_logo),
                modifier = Modifier
                    .size(38.dp),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifFamily),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.side_tagline).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 2.sp),
            fontWeight = FontWeight.Bold,
            color = colors.inkFaint,
            modifier = Modifier.padding(start = 2.dp, top = 4.dp),
        )
    }
}

@Composable
private fun CoupleWidget(session: Session?, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val me = session?.myName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.side_you)
    val partner = session?.partnerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.side_partner)
    val paired = session?.paired == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .twinGlass(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Avatar(me)
            Text("♥", color = colors.inkFaint, fontSize = 13.sp)
            Avatar(partner)
            Text(
                text = "$me & $partner",
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val dot = if (paired) colors.success else colors.error
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Text(
                text = stringResource(if (paired) R.string.side_synced else R.string.side_waiting_partner),
                style = MaterialTheme.typography.labelMedium,
                color = dot,
            )
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .twinWine(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun SidebarRow(entry: SidebarEntry, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TwinTheme.shapes.minTouch)
            // Active page = burgundy glass (like the active tab on Movie and Music); the rest stay clear.
            .then(if (active) Modifier.twinWine(shape) else Modifier.clip(shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Same icon circle as the Home "all pages" tiles: neutral glass, white icon (never pink).
        Box(
            modifier = Modifier
                .size(34.dp)
                .twinGlass(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = stringResource(entry.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) Color.White else RowText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Page names in the menu: soft white, like the labels under the Home page tiles. */
private val RowText = Color.White.copy(alpha = 0.78f)
