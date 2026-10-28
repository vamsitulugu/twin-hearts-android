package com.twinhearts.app.feature.fights.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HearingDisabled
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.twinhearts.app.R
import com.twinhearts.app.feature.fights.domain.model.ActivityFilter
import com.twinhearts.app.feature.fights.domain.model.FightApproach
import com.twinhearts.app.feature.fights.domain.model.FightCategory
import com.twinhearts.app.feature.fights.domain.model.FightFeeling
import com.twinhearts.app.feature.fights.domain.model.FightRange
import com.twinhearts.app.feature.fights.domain.model.FightStarter
import com.twinhearts.app.feature.fights.domain.model.ReconnectActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Why this file exists: one place that connects the plain Fight Log choices to their icons, colours
 * and strings, so the screens stay short.
 */

/** A tile/tone colour from the 0xRRGGBB value stored with a category or activity. */
fun toneColor(tone: Long): Color = Color(0xFF000000L or tone)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("en", "IN"))

/** Same look as the web's fmtDate ("28 Oct 2026"), or a dash. */
fun formatFightDate(raw: String?): String =
    runCatching { dateFormat.format(LocalDate.parse(raw)) }.getOrDefault("—")

fun categoryIcon(c: FightCategory): ImageVector = when (c) {
    FightCategory.Communication -> Icons.Rounded.ChatBubbleOutline
    FightCategory.Time -> Icons.Rounded.Schedule
    FightCategory.Plans -> Icons.Rounded.CalendarMonth
    FightCategory.Jealousy -> Icons.Rounded.HeartBroken
    FightCategory.Family -> Icons.Rounded.Group
    FightCategory.Money -> Icons.Rounded.AccountBalanceWallet
    FightCategory.Habits -> Icons.Rounded.Repeat
    FightCategory.Other -> Icons.Rounded.MoreHoriz
}

@StringRes
fun categoryLabel(c: FightCategory): Int = when (c) {
    FightCategory.Communication -> R.string.fights_cat_communication
    FightCategory.Time -> R.string.fights_cat_time
    FightCategory.Plans -> R.string.fights_cat_plans
    FightCategory.Jealousy -> R.string.fights_cat_jealousy
    FightCategory.Family -> R.string.fights_cat_family
    FightCategory.Money -> R.string.fights_cat_money
    FightCategory.Habits -> R.string.fights_cat_habits
    FightCategory.Other -> R.string.fights_cat_other
}

fun starterIcon(s: FightStarter): ImageVector = when (s) {
    FightStarter.Me, FightStarter.Partner -> Icons.Rounded.Person
    FightStarter.Both -> Icons.Rounded.Group
    FightStarter.Unsure -> Icons.Rounded.Help
}

@StringRes
fun starterLabel(s: FightStarter): Int = when (s) {
    FightStarter.Me -> R.string.fights_starter_me
    FightStarter.Partner -> R.string.fights_starter_partner
    FightStarter.Both -> R.string.fights_starter_both
    FightStarter.Unsure -> R.string.fights_starter_unsure
}

fun feelingIcon(f: FightFeeling): ImageVector = when (f) {
    FightFeeling.Angry -> Icons.Rounded.SentimentVeryDissatisfied
    FightFeeling.Hurt -> Icons.Rounded.HeartBroken
    FightFeeling.Sad -> Icons.Rounded.SentimentDissatisfied
    FightFeeling.Frustrated -> Icons.Rounded.SentimentNeutral
    FightFeeling.Anxious -> Icons.Rounded.Bolt
    FightFeeling.Misunderstood -> Icons.Rounded.HearingDisabled
    FightFeeling.Ignored -> Icons.Rounded.VisibilityOff
    FightFeeling.Tired -> Icons.Rounded.Bedtime
    FightFeeling.Confused -> Icons.Rounded.Help
}

@StringRes
fun feelingLabel(f: FightFeeling): Int = when (f) {
    FightFeeling.Angry -> R.string.fights_feel_angry
    FightFeeling.Hurt -> R.string.fights_feel_hurt
    FightFeeling.Sad -> R.string.fights_feel_sad
    FightFeeling.Frustrated -> R.string.fights_feel_frustrated
    FightFeeling.Anxious -> R.string.fights_feel_anxious
    FightFeeling.Misunderstood -> R.string.fights_feel_misunderstood
    FightFeeling.Ignored -> R.string.fights_feel_ignored
    FightFeeling.Tired -> R.string.fights_feel_tired
    FightFeeling.Confused -> R.string.fights_feel_confused
}

fun approachIcon(a: FightApproach): ImageVector = when (a) {
    FightApproach.Talk -> Icons.Rounded.Forum
    FightApproach.Apology -> Icons.Rounded.VolunteerActivism
    FightApproach.Space -> Icons.Rounded.Bedtime
    FightApproach.Compromise -> Icons.Rounded.Balance
    FightApproach.Activity -> Icons.Rounded.AutoAwesome
    FightApproach.Time -> Icons.Rounded.HourglassEmpty
}

/** [resolved] picks the past-tense wording ("Talked it out") over the plan wording ("Talk it out"). */
@StringRes
fun approachLabel(a: FightApproach, resolved: Boolean): Int = when (a) {
    FightApproach.Talk -> if (resolved) R.string.fights_app_talk_done else R.string.fights_app_talk_next
    FightApproach.Apology -> if (resolved) R.string.fights_app_apology_done else R.string.fights_app_apology_next
    FightApproach.Space -> if (resolved) R.string.fights_app_space_done else R.string.fights_app_space_next
    FightApproach.Compromise -> if (resolved) R.string.fights_app_compromise_done else R.string.fights_app_compromise_next
    FightApproach.Activity -> if (resolved) R.string.fights_app_activity_done else R.string.fights_app_activity_next
    FightApproach.Time -> if (resolved) R.string.fights_app_time_done else R.string.fights_app_time_next
}

@StringRes
fun rangeLabel(r: FightRange): Int = when (r) {
    FightRange.Days30 -> R.string.fights_range_30
    FightRange.Days90 -> R.string.fights_range_90
    FightRange.Days180 -> R.string.fights_range_180
    FightRange.All -> R.string.fights_range_all
}

@StringRes
fun filterLabel(f: ActivityFilter): Int = when (f) {
    ActivityFilter.All -> R.string.fights_filter_all
    ActivityFilter.Communication -> R.string.fights_filter_communication
    ActivityFilter.Fun -> R.string.fights_filter_fun
    ActivityFilter.Calm -> R.string.fights_filter_calm
    ActivityFilter.Growth -> R.string.fights_filter_growth
}

fun activityIcon(a: ReconnectActivity): ImageVector = when (a) {
    ReconnectActivity.Mirror -> Icons.Rounded.ChatBubbleOutline
    ReconnectActivity.Letter -> Icons.Rounded.Edit
    ReconnectActivity.Walk -> Icons.Rounded.DirectionsWalk
    ReconnectActivity.Appreciation -> Icons.Rounded.Favorite
    ReconnectActivity.Hug -> Icons.Rounded.Group
    ReconnectActivity.Play -> Icons.Rounded.SportsEsports
    ReconnectActivity.Cook -> Icons.Rounded.Restaurant
    ReconnectActivity.WhyDive -> Icons.Rounded.Psychology
}

@StringRes
fun activityTitle(a: ReconnectActivity): Int = when (a) {
    ReconnectActivity.Mirror -> R.string.fights_act_mirror_title
    ReconnectActivity.Letter -> R.string.fights_act_letter_title
    ReconnectActivity.Walk -> R.string.fights_act_walk_title
    ReconnectActivity.Appreciation -> R.string.fights_act_appreciation_title
    ReconnectActivity.Hug -> R.string.fights_act_hug_title
    ReconnectActivity.Play -> R.string.fights_act_play_title
    ReconnectActivity.Cook -> R.string.fights_act_cook_title
    ReconnectActivity.WhyDive -> R.string.fights_act_whydive_title
}

@StringRes
fun activityShort(a: ReconnectActivity): Int = when (a) {
    ReconnectActivity.Mirror -> R.string.fights_act_mirror_short
    ReconnectActivity.Letter -> R.string.fights_act_letter_short
    ReconnectActivity.Walk -> R.string.fights_act_walk_short
    ReconnectActivity.Appreciation -> R.string.fights_act_appreciation_short
    ReconnectActivity.Hug -> R.string.fights_act_hug_short
    ReconnectActivity.Play -> R.string.fights_act_play_short
    ReconnectActivity.Cook -> R.string.fights_act_cook_short
    ReconnectActivity.WhyDive -> R.string.fights_act_whydive_short
}

@StringRes
fun activityHow(a: ReconnectActivity): Int = when (a) {
    ReconnectActivity.Mirror -> R.string.fights_act_mirror_how
    ReconnectActivity.Letter -> R.string.fights_act_letter_how
    ReconnectActivity.Walk -> R.string.fights_act_walk_how
    ReconnectActivity.Appreciation -> R.string.fights_act_appreciation_how
    ReconnectActivity.Hug -> R.string.fights_act_hug_how
    ReconnectActivity.Play -> R.string.fights_act_play_how
    ReconnectActivity.Cook -> R.string.fights_act_cook_how
    ReconnectActivity.WhyDive -> R.string.fights_act_whydive_how
}
