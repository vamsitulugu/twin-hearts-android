package com.twinhearts.app.feature.games.ui.components

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.twinhearts.app.R
import com.twinhearts.app.feature.games.domain.engine.MindReaderEngine
import com.twinhearts.app.feature.games.domain.engine.MrRef

/** One Mind Reader question. [options] is 0 for a free-text question, else a string-array of choices. */
data class MrQuestion(
    val cat: String,
    @StringRes val text: Int,
    @ArrayRes val options: Int = 0,
) {
    val isChoice: Boolean get() = options != 0
}

/**
 * The question bank, same questions and order as the web app. The saved game only stores the
 * index, so both phones show the same text in their own language.
 */
object MindReaderCatalog {

    val questions: List<MrQuestion> = listOf(
        MrQuestion("love", R.string.games_mr_q_01),
        MrQuestion("love", R.string.games_mr_q_02),
        MrQuestion("love", R.string.games_mr_q_03),
        MrQuestion("love", R.string.games_mr_q_04),
        MrQuestion("love", R.string.games_mr_q_05, R.array.games_mr_opts_05),
        MrQuestion("relationship", R.string.games_mr_q_06),
        MrQuestion("relationship", R.string.games_mr_q_07),
        MrQuestion("relationship", R.string.games_mr_q_08),
        MrQuestion("relationship", R.string.games_mr_q_09),
        MrQuestion("relationship", R.string.games_mr_q_10, R.array.games_mr_opts_10),
        MrQuestion("movies", R.string.games_mr_q_11),
        MrQuestion("movies", R.string.games_mr_q_12),
        MrQuestion("movies", R.string.games_mr_q_13),
        MrQuestion("movies", R.string.games_mr_q_14, R.array.games_mr_opts_14),
        MrQuestion("music", R.string.games_mr_q_15),
        MrQuestion("music", R.string.games_mr_q_16),
        MrQuestion("music", R.string.games_mr_q_17),
        MrQuestion("music", R.string.games_mr_q_18, R.array.games_mr_opts_18),
        MrQuestion("travel", R.string.games_mr_q_19),
        MrQuestion("travel", R.string.games_mr_q_20),
        MrQuestion("travel", R.string.games_mr_q_21),
        MrQuestion("travel", R.string.games_mr_q_22, R.array.games_mr_opts_22),
        MrQuestion("dreams", R.string.games_mr_q_23),
        MrQuestion("dreams", R.string.games_mr_q_24),
        MrQuestion("dreams", R.string.games_mr_q_25),
        MrQuestion("food", R.string.games_mr_q_26),
        MrQuestion("food", R.string.games_mr_q_27),
        MrQuestion("food", R.string.games_mr_q_28),
        MrQuestion("food", R.string.games_mr_q_29, R.array.games_mr_opts_29),
        MrQuestion("future", R.string.games_mr_q_30),
        MrQuestion("future", R.string.games_mr_q_31),
        MrQuestion("future", R.string.games_mr_q_32),
        MrQuestion("habits", R.string.games_mr_q_33),
        MrQuestion("habits", R.string.games_mr_q_34),
        MrQuestion("habits", R.string.games_mr_q_35, R.array.games_mr_opts_35),
        MrQuestion("personality", R.string.games_mr_q_36),
        MrQuestion("personality", R.string.games_mr_q_37),
        MrQuestion("personality", R.string.games_mr_q_38),
        MrQuestion("funny", R.string.games_mr_q_39),
        MrQuestion("funny", R.string.games_mr_q_40),
        MrQuestion("funny", R.string.games_mr_q_41),
        MrQuestion("deep", R.string.games_mr_q_42),
        MrQuestion("deep", R.string.games_mr_q_43),
        MrQuestion("deep", R.string.games_mr_q_44),
        MrQuestion("memories", R.string.games_mr_q_45),
        MrQuestion("memories", R.string.games_mr_q_46),
        MrQuestion("memories", R.string.games_mr_q_47),
        MrQuestion("dating", R.string.games_mr_q_48),
        MrQuestion("dating", R.string.games_mr_q_49),
        MrQuestion("lifestyle", R.string.games_mr_q_50),
        MrQuestion("lifestyle", R.string.games_mr_q_51),
        MrQuestion("family", R.string.games_mr_q_52),
        MrQuestion("family", R.string.games_mr_q_53),
        MrQuestion("goals", R.string.games_mr_q_54),
        MrQuestion("goals", R.string.games_mr_q_55),
        MrQuestion("random", R.string.games_mr_q_56),
        MrQuestion("random", R.string.games_mr_q_57),
        MrQuestion("random", R.string.games_mr_q_58),
    )

    fun of(index: Int): MrQuestion? = questions.getOrNull(index)

    /** Questions the engine may pick from: everything, or one category. */
    fun pool(category: String): List<MrRef> = questions.mapIndexedNotNull { i, q ->
        if (category == MindReaderEngine.RANDOM || q.cat == category) MrRef(i, q.cat) else null
    }

    @StringRes
    fun categoryLabel(cat: String): Int = when (cat) {
        "love" -> R.string.games_mr_cat_love
        "relationship" -> R.string.games_mr_cat_relationship
        "movies" -> R.string.games_mr_cat_movies
        "music" -> R.string.games_mr_cat_music
        "travel" -> R.string.games_mr_cat_travel
        "dreams" -> R.string.games_mr_cat_dreams
        "food" -> R.string.games_mr_cat_food
        "future" -> R.string.games_mr_cat_future
        "habits" -> R.string.games_mr_cat_habits
        "personality" -> R.string.games_mr_cat_personality
        "funny" -> R.string.games_mr_cat_funny
        "deep" -> R.string.games_mr_cat_deep
        "memories" -> R.string.games_mr_cat_memories
        "dating" -> R.string.games_mr_cat_dating
        "lifestyle" -> R.string.games_mr_cat_lifestyle
        "family" -> R.string.games_mr_cat_family
        "goals" -> R.string.games_mr_cat_goals
        else -> R.string.games_mr_cat_random
    }

    @StringRes
    fun achievementLabel(id: String): Int = when (id) {
        MindReaderEngine.ACH_SOULMATES -> R.string.games_mr_ach_soulmates
        MindReaderEngine.ACH_STREAK5 -> R.string.games_mr_ach_streak5
        MindReaderEngine.ACH_TEN_ROUNDS -> R.string.games_mr_ach_ten_rounds
        MindReaderEngine.ACH_CATEGORY_MASTER -> R.string.games_mr_ach_category_master
        else -> R.string.games_mr_ach_funny_bunch
    }
}
