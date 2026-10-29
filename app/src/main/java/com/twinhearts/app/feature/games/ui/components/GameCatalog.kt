package com.twinhearts.app.feature.games.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.model.GameId

enum class GameBadge { None, New, Trending }

/** What the hub shows for one game card. Texts are string resources (R-53). */
data class GameInfo(
    val id: GameId,
    @StringRes val title: Int,
    @StringRes val emoji: Int,
    @StringRes val description: Int,
    val tags: List<Int>,
    val badge: GameBadge,
    /** False for games that arrive in the next update. */
    val playable: Boolean,
)

object GameCatalog {
    /** Same order and wording as the web app's hub. */
    val all: List<GameInfo> = listOf(
        info(GameId.TruthOrDare, R.string.games_tod_title, R.string.games_tod_emoji, R.string.games_tod_desc,
            listOf(R.string.games_tod_tag1, R.string.games_tod_tag2, R.string.games_tod_tag3), GameBadge.Trending, true),
        info(GameId.MindReader, R.string.games_mindreader_title, R.string.games_mindreader_emoji, R.string.games_mindreader_desc,
            listOf(R.string.games_mindreader_tag1, R.string.games_mindreader_tag2, R.string.games_mindreader_tag3), GameBadge.New, false),
        info(GameId.Ludo, R.string.games_ludo_title, R.string.games_ludo_emoji, R.string.games_ludo_desc,
            listOf(R.string.games_ludo_tag1, R.string.games_ludo_tag2, R.string.games_ludo_tag3), GameBadge.New, true),
        info(GameId.TicTacToe, R.string.games_ttt_title, R.string.games_ttt_emoji, R.string.games_ttt_desc,
            listOf(R.string.games_ttt_tag1, R.string.games_ttt_tag2, R.string.games_ttt_tag3), GameBadge.None, true),
        info(GameId.Draw, R.string.games_draw_title, R.string.games_draw_emoji, R.string.games_draw_desc,
            listOf(R.string.games_draw_tag1, R.string.games_draw_tag2, R.string.games_draw_tag3), GameBadge.None, false),
        info(GameId.Memory, R.string.games_memory_title, R.string.games_memory_emoji, R.string.games_memory_desc,
            listOf(R.string.games_memory_tag1, R.string.games_memory_tag2, R.string.games_memory_tag3), GameBadge.None, true),
        info(GameId.Bomb, R.string.games_bomb_title, R.string.games_bomb_emoji, R.string.games_bomb_desc,
            listOf(R.string.games_bomb_tag1, R.string.games_bomb_tag2, R.string.games_bomb_tag3), GameBadge.New, false),
        info(GameId.Puzzle, R.string.games_puzzle_title, R.string.games_puzzle_emoji, R.string.games_puzzle_desc,
            listOf(R.string.games_puzzle_tag1, R.string.games_puzzle_tag2, R.string.games_puzzle_tag3), GameBadge.New, false),
    )

    fun of(id: GameId): GameInfo = all.first { it.id == id }

    private fun info(
        id: GameId, title: Int, emoji: Int, desc: Int, tags: List<Int>, badge: GameBadge, playable: Boolean,
    ) = GameInfo(id, title, emoji, desc, tags, badge, playable)
}

/** Card art colours, built only from theme colours so they follow the app theme. */
@Composable
fun gameBrush(id: GameId): Brush {
    val c = TwinTheme.colors
    val pair: Pair<Color, Color> = when (id) {
        GameId.TruthOrDare -> c.primaryStart to c.primaryEnd
        GameId.MindReader -> c.orchid.copy(alpha = 0.7f) to c.card
        GameId.Ludo -> c.primaryEnd to c.card
        GameId.TicTacToe -> c.neutralGlass to c.card
        GameId.Draw -> c.accent.copy(alpha = 0.7f) to c.card
        GameId.Memory -> c.success.copy(alpha = 0.55f) to c.card
        GameId.Bomb -> c.error.copy(alpha = 0.7f) to c.background
        GameId.Puzzle -> c.pinkSoft.copy(alpha = 0.6f) to c.card
    }
    return Brush.linearGradient(listOf(pair.first, pair.second))
}
