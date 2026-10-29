package com.twinhearts.app.feature.games.ui.play

import androidx.annotation.StringRes

/** Short sound/haptic cues, like the web app's little beeps. */
enum class GameCue { Tap, Success, Fail, Dice, Place, Coin, LevelUp }

/** One line inside a prize pop-up. The screen turns it into text. */
sealed interface PrizeLine {
    data class Xp(val amount: Int) : PrizeLine
    data class Coins(val amount: Int) : PrizeLine
    data class Level(val level: Int) : PrizeLine
    data class Text(@StringRes val res: Int) : PrizeLine

    /** "Final: 5 - 3" */
    data class Final(val scoreOne: Int, val scoreTwo: Int) : PrizeLine

    /** Index into the penalty list in strings (Truth or Dare skip). */
    data class Penalty(val index: Int) : PrizeLine
}

/** The "You won!" / "Level Up!" pop-up. */
data class Prize(
    val emoji: String,
    @StringRes val titleRes: Int,
    val lines: List<PrizeLine>,
)

/** One-off things a game tells its screen. They are consumed once and never kept in state. */
sealed interface GameEvent {
    data class Cue(val cue: GameCue) : GameEvent
    data class ShowPrize(val prize: Prize) : GameEvent

    /** A short toast. Args are inserted into the string (for example the partner's name). */
    data class Message(@StringRes val res: Int, val args: List<String> = emptyList()) : GameEvent
}
