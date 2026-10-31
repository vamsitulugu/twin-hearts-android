package com.twinhearts.app.feature.games.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.twinhearts.app.R
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
    /** The 16:9 card picture (copied from the web app's images/games). */
    @DrawableRes val art: Int,
)

object GameCatalog {
    /** Same order and wording as the web app's hub. */
    val all: List<GameInfo> = listOf(
        info(GameId.TruthOrDare, R.string.games_tod_title, R.string.games_tod_emoji, R.string.games_tod_desc,
            listOf(R.string.games_tod_tag1, R.string.games_tod_tag2, R.string.games_tod_tag3), GameBadge.Trending, R.drawable.games_tod),
        info(GameId.MindReader, R.string.games_mindreader_title, R.string.games_mindreader_emoji, R.string.games_mindreader_desc,
            listOf(R.string.games_mindreader_tag1, R.string.games_mindreader_tag2, R.string.games_mindreader_tag3), GameBadge.New, R.drawable.games_mindreader),
        info(GameId.Ludo, R.string.games_ludo_title, R.string.games_ludo_emoji, R.string.games_ludo_desc,
            listOf(R.string.games_ludo_tag1, R.string.games_ludo_tag2, R.string.games_ludo_tag3), GameBadge.New, R.drawable.games_ludo),
        info(GameId.TicTacToe, R.string.games_ttt_title, R.string.games_ttt_emoji, R.string.games_ttt_desc,
            listOf(R.string.games_ttt_tag1, R.string.games_ttt_tag2, R.string.games_ttt_tag3), GameBadge.None, R.drawable.games_ttt),
        info(GameId.Draw, R.string.games_draw_title, R.string.games_draw_emoji, R.string.games_draw_desc,
            listOf(R.string.games_draw_tag1, R.string.games_draw_tag2, R.string.games_draw_tag3), GameBadge.None, R.drawable.games_draw),
        info(GameId.Memory, R.string.games_memory_title, R.string.games_memory_emoji, R.string.games_memory_desc,
            listOf(R.string.games_memory_tag1, R.string.games_memory_tag2, R.string.games_memory_tag3), GameBadge.None, R.drawable.games_memory),
        info(GameId.Bomb, R.string.games_bomb_title, R.string.games_bomb_emoji, R.string.games_bomb_desc,
            listOf(R.string.games_bomb_tag1, R.string.games_bomb_tag2, R.string.games_bomb_tag3), GameBadge.New, R.drawable.games_bomb),
        info(GameId.Puzzle, R.string.games_puzzle_title, R.string.games_puzzle_emoji, R.string.games_puzzle_desc,
            listOf(R.string.games_puzzle_tag1, R.string.games_puzzle_tag2, R.string.games_puzzle_tag3), GameBadge.New, R.drawable.games_puzzle),
    )

    fun of(id: GameId): GameInfo = all.first { it.id == id }

    private fun info(
        id: GameId, title: Int, emoji: Int, desc: Int, tags: List<Int>, badge: GameBadge, art: Int,
    ) = GameInfo(id, title, emoji, desc, tags, badge, art)
}

/**
 * Card gradients, the same two colours per game as the web hub (games.html GAMES[].grad). They sit
 * behind the picture and colour the "Play Now" button.
 */
@Composable
fun gameBrush(id: GameId): Brush {
    val (from, to) = when (id) {
        GameId.TruthOrDare -> Color(0xFF2A0620) to Color(0xFF6B0A4A)
        GameId.MindReader -> Color(0xFF2E0D22) to Color(0xFF5C0D4A)
        GameId.Ludo -> Color(0xFF2A0D1D) to Color(0xFF5C0D36)
        GameId.TicTacToe -> Color(0xFF2E1A26) to Color(0xFF53374F)
        GameId.Draw -> Color(0xFF2E0D22) to Color(0xFF3A0425)
        GameId.Memory -> Color(0xFF0D2A1A) to Color(0xFF0D5C3A)
        GameId.Bomb -> Color(0xFF1A0212) to Color(0xFF6B0A4A)
        GameId.Puzzle -> Color(0xFF2E0D22) to Color(0xFF3A0425)
    }
    return Brush.linearGradient(listOf(from, to))
}
