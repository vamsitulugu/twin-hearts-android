package com.twinhearts.app.feature.storefx

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours of one Twin Store chat theme. One palette is used by BOTH the Chat page and the Music
 * page, so they always match. Same values as the web (public/twin-store-fx.js).
 * [dark] false = a light theme: text becomes dark and glass becomes a dark tint.
 */
@Immutable
data class StoreFxTheme(
    val id: String,
    val dark: Boolean,
    val bg1: Color,
    val bg2: Color,
    val me1: Color,
    val me2: Color,
    val partner: Color,
    val accent: Color,
    val text: Color,
)

/** Catalog data for the four Twin Store categories. Ids match the server catalog. */
object StoreFxCatalog {
    val themes: List<StoreFxTheme> = listOf(
        StoreFxTheme("theme_sunset", true, Color(0xFF2A1230), Color(0xFF4A1B3F), Color(0xFFFF7A59), Color(0xFFFF4F8B), Color(0xFF3B2142), Color(0xFFFFB36B), Color(0xFFFFF3EC)),
        StoreFxTheme("theme_midnight", true, Color(0xFF0B0F2A), Color(0xFF1E1550), Color(0xFF7C5CFF), Color(0xFFB65CFF), Color(0xFF1B2050), Color(0xFF8FB4FF), Color(0xFFEEF0FF)),
        StoreFxTheme("theme_rose", false, Color(0xFFFFF1F4), Color(0xFFFFE0E8), Color(0xFFF2557B), Color(0xFFE83E8C), Color(0xFFFFFFFF), Color(0xFFC2185B), Color(0xFF3A1B26)),
        StoreFxTheme("theme_emerald", true, Color(0xFF06201C), Color(0xFF0F3D3A), Color(0xFF1FD1A5), Color(0xFF3AA6FF), Color(0xFF12332F), Color(0xFF5CF2C7), Color(0xFFE8FFF8)),
        StoreFxTheme("theme_lavender", false, Color(0xFFF5F0FF), Color(0xFFE8DEFF), Color(0xFF8E6CF0), Color(0xFFB07CF5), Color(0xFFFFFFFF), Color(0xFF6A4CE0), Color(0xFF2A1F4A)),
        StoreFxTheme("theme_cotton", false, Color(0xFFFFF6F0), Color(0xFFEAF4FF), Color(0xFFFF8FA8), Color(0xFFFFB38A), Color(0xFFFFFFFF), Color(0xFF6FB6FF), Color(0xFF3A2A33)),
    )

    fun theme(id: String?): StoreFxTheme? = themes.firstOrNull { it.id == id }

    /** Profile frame ids -> ring colours (outer, inner) and the little badge emoji. */
    data class Frame(val id: String, val outer: Color, val inner: Color, val glow: Color, val badge: String, val pulse: Boolean)

    val frames: List<Frame> = listOf(
        Frame("frame_gold_heart", Color(0xFFF5B83D), Color(0xFFFFF3C4), Color(0xFFF5B83D), "💛", false),
        Frame("frame_neon_pulse", Color(0xFFA855F7), Color(0xFFD8B4FE), Color(0xFFA855F7), "⚡", true),
        Frame("frame_royal_crown", Color(0xFFEAB308), Color(0xFF7C3AED), Color(0xFFEAB308), "👑", false),
        Frame("frame_sparkle_ring", Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF38BDF8), "✨", true),
    )

    fun frame(id: String?): Frame? = frames.firstOrNull { it.id == id }

    /** Home "Cozy Corner" decor. */
    data class Decor(val id: String, val emoji: String, val title: String, val line: String, val glow: Color)

    val decor: List<Decor> = listOf(
        Decor("decor_fairy_lights", "💡", "Fairy String Lights", "Warm lights twinkling over your Virtual Home", Color(0xFFFEF08A)),
        Decor("decor_fireplace", "🔥", "Cozy Fireplace", "A crackling fire for warm evenings together", Color(0xFFEA580C)),
        Decor("decor_love_plant", "🪴", "Eternal Love Plant", "Blooms with every milestone you reach", Color(0xFF16A34A)),
        Decor("decor_teddy_corner", "🧸", "Giant Teddy Corner", "A jumbo cuddle teddy waiting for you both", Color(0xFFB45309)),
    )

    fun decor(id: String?): Decor? = decor.firstOrNull { it.id == id }

    /** Truth or Dare packs: same words as the web. */
    data class Pack(val id: String, val emoji: String, val name: String, val truths: List<String>, val dares: List<String>)

    val packs: List<Pack> = listOf(
        Pack(
            "pack_deep_love", "🔮", "Deep Intimacy Deck",
            truths = listOf(
                "What is a fear you have never fully told me about?", "When did you feel closest to me?",
                "What do you need more of from us right now?", "What is a memory of us you replay when you miss me?",
                "What part of yourself are you still learning to trust me with?", "What does feeling truly safe with me look like?",
                "What is something I do that makes you feel deeply understood?", "What did you think the first time you realised you loved me?",
            ),
            dares = listOf(
                "Hold eye contact for one full minute without talking", "Tell me three things you admire about me and why",
                "Share one dream you have never said out loud", "Write one honest line about how you feel tonight and read it to me",
                "Describe our relationship in three words and explain each", "Thank me for something small I did that you never mentioned",
                "Say what you want us to be in ten years", "Give me a slow, long hug and say what you are grateful for",
            ),
        ),
        Pack(
            "pack_spicy_dares", "🌶️", "Spicy Couple Dares",
            truths = listOf(
                "What outfit of mine can you not look away from?", "What is the most flirty thing you have thought about me today?",
                "Where is your favourite place for me to kiss you?", "What is a date night idea that would make your heart race?",
                "What is your boldest flirting moment with me?", "What nickname from me gives you butterflies?",
            ),
            dares = listOf(
                "Send me your most flirty voice note", "Give me a slow dance, even with no music",
                "Whisper something sweet and daring in my ear", "Wink at me three different ways",
                "Plan a surprise candlelit evening for us this week", "Feed me something sweet while looking into my eyes",
                "Give me a 30-second shoulder massage", "Write a tiny love poem about me, right now",
            ),
        ),
        Pack(
            "pack_future_dreams", "✈️", "Future & Dreams Deck",
            truths = listOf(
                "Where in the world do you want us to wake up together?", "What does our dream home look like?",
                "What is one goal you want us to chase together this year?", "What tradition do you want us to start?",
                "What adventure is first on our bucket list?", "How do you picture our ordinary Sunday in ten years?",
                "What skill would you love us to learn together?", "What would you want us to be remembered for?",
            ),
            dares = listOf(
                "Plan our dream holiday in 2 minutes and pitch it", "Add one new item to our bucket list right now",
                "Draw our dream house on paper and show me", "Pick a date for our next mini adventure and book it in",
                "Name the pet we will have one day, and why", "Write a one-line promise for our future and sign it",
                "Choose a song for our future first dance", "Describe our wedding or anniversary trip as a movie trailer",
            ),
        ),
    )

    fun pack(id: String?): Pack? = packs.firstOrNull { it.id == id }
}
