package com.twinhearts.app.feature.chat.domain

/**
 * Why this file exists: the fixed lists the web chat shows in its pickers (emoji, stickers, gifts,
 * quick reactions, wallpaper colours), so the Android pickers offer exactly the same things.
 */

/** Row that appears above a long-pressed message (web: QUICK_REACTIONS / REACTION_EMOJIS). */
val QuickReactions: List<String> = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")

/** The emoji tabs, in the order of the web. "Recent" is filled from what the person used. */
val EmojiCategories: List<Pair<String, List<String>>> = listOf(
    "Recent" to emptyList(),
    "Smileys" to listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊", "😇", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗",
        "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬",
        "😴", "😪", "😷", "🤒", "🤕",
    ),
    "Emotions" to listOf(
        "😢", "😭", "😤", "😠", "😡", "🥺", "😨", "😰", "😥", "😓", "🤯", "😳", "🥵", "🥶", "😱", "😖", "😣", "😞",
        "😔", "😟", "😕", "🙁", "☹️", "😩", "😫", "😵", "🤐", "🥴", "🤢", "🤮",
    ),
    "Love" to listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "💔",
        "❣️", "💌", "😻", "😽", "💑", "💏", "👩‍❤️‍👨", "👩‍❤️‍💋‍👨",
    ),
    "Gestures" to listOf(
        "👍", "👎", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘", "👌", "🤏", "👈", "👉", "👆", "👇", "☝️", "✋",
        "🤚", "🖐️", "🖖", "👋", "🤙", "💪", "🙏", "👏", "🙌", "🤝", "🫶",
    ),
    "Celebration" to listOf(
        "🎉", "🎊", "🎈", "🎁", "🎂", "🍾", "🥂", "✨", "🌟", "💫", "🔥", "💯", "🏆", "🥳", "🎆", "🎇", "🪅", "🎀",
    ),
)

/** Words the emoji search understands ("love", "fire", "cake" ...). */
val EmojiKeywords: Map<String, List<String>> = mapOf(
    "love" to listOf("❤️", "😍", "🥰", "💕", "💖", "😘"),
    "heart" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔"),
    "happy" to listOf("😀", "😄", "😊", "🥳"),
    "sad" to listOf("😢", "😭", "😞", "😔"),
    "laugh" to listOf("😂", "🤣", "😆"),
    "angry" to listOf("😠", "😡", "😤"),
    "kiss" to listOf("😘", "😙", "😚", "💋"),
    "hug" to listOf("🤗", "🫂"),
    "fire" to listOf("🔥"),
    "party" to listOf("🎉", "🎊", "🥳", "🎈"),
    "cake" to listOf("🎂", "🍰"),
    "ring" to listOf("💍"),
    "star" to listOf("🌟", "✨", "⭐"),
    "thumbsup" to listOf("👍"),
    "clap" to listOf("👏"),
    "pray" to listOf("🙏"),
    "think" to listOf("🤔"),
    "cry" to listOf("😭", "😢"),
    "cool" to listOf("😎"),
    "wink" to listOf("😉"),
    "tired" to listOf("😴", "😪"),
    "sick" to listOf("🤒", "🤢"),
    "flower" to listOf("🌹", "💐", "🌸"),
)

/** Emoji for a search word: every keyword that contains it, without duplicates. */
fun searchEmoji(query: String): List<String> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return EmojiKeywords.filterKeys { it.contains(q) }.values.flatten().distinct()
}

/** An emoji plus its name: used for both stickers and gifts. */
data class NamedEmoji(val emoji: String, val name: String)

val Stickers: List<NamedEmoji> = listOf(
    NamedEmoji("😍", "Adore"), NamedEmoji("🥰", "In Love"), NamedEmoji("😘", "Kiss"),
    NamedEmoji("🤗", "Hug"), NamedEmoji("😂", "LOL"), NamedEmoji("🥺", "Puppy Eyes"),
    NamedEmoji("😴", "Sleepy"), NamedEmoji("🙈", "Shy"), NamedEmoji("💃", "Dance"),
    NamedEmoji("🎉", "Yay!"), NamedEmoji("😤", "Grumpy"), NamedEmoji("🥳", "Party"),
)

val CoupleGifts: List<NamedEmoji> = listOf(
    NamedEmoji("🌹", "Rose"), NamedEmoji("💐", "Bouquet"), NamedEmoji("🍫", "Chocolate"),
    NamedEmoji("🧸", "Teddy"), NamedEmoji("💍", "Ring"), NamedEmoji("🎂", "Cake"),
    NamedEmoji("🎈", "Balloon"), NamedEmoji("🍰", "Slice"), NamedEmoji("🎁", "Gift"),
)

/** How many recent emoji the picker remembers. */
const val MAX_RECENT_EMOJI = 24

/** Newest first, no duplicates, at most [MAX_RECENT_EMOJI]. */
fun pushRecentEmoji(current: List<String>, emoji: String): List<String> =
    (listOf(emoji) + current.filter { it != emoji }).take(MAX_RECENT_EMOJI)

/** The four ready-made backgrounds of the wallpaper screen. Colours are in the UI (tokens). */
val WallpaperSwatchIds: List<String> = listOf("midnight", "noir", "wine", "slate")

/** The "Disappearing messages" choices, in seconds (0 = off). */
val DisappearPresets: List<Int> = listOf(0, 3600, 86_400, 604_800)
