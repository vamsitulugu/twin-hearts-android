package com.twinhearts.app.feature.access.domain

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.core.sound.TwinSound
import com.twinhearts.app.feature.access.data.remote.AccessApi
import com.twinhearts.app.feature.access.data.remote.AccessPassBody
import com.twinhearts.app.feature.access.data.remote.AccessUseBody
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** The four things that get a few free uses a day, then one small coin pass. [key] is what the server expects. */
enum class AccessFeature(
    val key: String,
    val title: String,
    val freeNoun: String,
    val unlimited: String,
) {
    Movie("movie", "Movie night pass", "movie", "movies"),
    Songs("songs", "Sing Together pass", "song", "songs"),
    Pins("pins", "Globe pins pass", "pin", "pins"),
    Games("games", "Game night pass", "game", "games"),
}

/** What the "free ones are gone" sheet shows. Every number comes from the server. */
data class AccessAsk(
    val feature: AccessFeature,
    val price: Int,
    val hours: Int,
    val freeTotal: Int,
    val balance: Int,
    val emoji: String,
    val perk: String,
) {
    val coinsMissing: Int get() = (price - balance).coerceAtLeast(0)
}

/** One open question to the person; [answer] is true when they unlocked a pass. */
class PendingAsk(val info: AccessAsk) {
    val answer = CompletableDeferred<Boolean>()
}

sealed interface PassResult {
    data class Unlocked(val expiresText: String) : PassResult
    data object NotEnoughCoins : PassResult
    data class Failed(val message: String) : PassResult
}

/**
 * Free daily uses, then ONE small coin pass, for Movie night, Sing Together, Globe pins and Couple Games.
 *
 *     if (!accessGate.check(AccessFeature.Movie)) return   // the person chose "Maybe later"
 *
 * How it feels:
 *  - The first few uses every day are FREE. A tiny toast says how many are left. No pop-up.
 *  - Only when the free ones are gone, ONE clear sheet explains why and what a pass gives
 *    (for example 20 coins = unlimited movies for 12 hours, for both partners).
 *  - While a pass is running nobody is asked again: not for every song, pin or game.
 * If the server cannot be reached this lets the person through (an outage must never lock the app).
 * Pro couples are never asked (the server says "pro").
 */
@Singleton
class AccessGate @Inject constructor(
    private val api: AccessApi,
    private val sessionStore: SessionStore,
    @ApplicationContext private val context: Context,
) {
    private val main = Handler(Looper.getMainLooper())

    private val _pending = MutableStateFlow<PendingAsk?>(null)

    /** The sheet host shows this when it is not null. */
    val pending: StateFlow<PendingAsk?> = _pending.asStateFlow()

    private val _openRewards = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** "Get coins free": the host asks the navigation to open the Rewards page. */
    val openRewards: SharedFlow<Unit> = _openRewards.asSharedFlow()

    /** True when the person may go ahead (free, pass, Pro, or they just bought a pass). */
    suspend fun check(feature: AccessFeature): Boolean {
        val s = sessionStore.session.first() ?: return true
        val r = when (val res = safeApiCall { api.use(AccessUseBody(s.coupleId, s.userId, feature.key), s.userId) }) {
            is AppResult.Success -> res.data
            is AppResult.Failure -> return true // server down / old server: never lock people out
        }
        if (r.allowed) {
            if (r.via == "free") {
                val noun = feature.freeNoun
                toast(
                    if (r.freeLeft > 0) "🎁 Free today · ${r.freeLeft} free $noun${if (r.freeLeft == 1) "" else "s"} left"
                    else "🎁 That was your last free $noun today. Free again tomorrow",
                )
            }
            return true
        }
        if (!r.needsCoins) return true
        return ask(
            AccessAsk(
                feature = feature, price = r.price, hours = r.hours, freeTotal = r.freeTotal,
                balance = r.balance, emoji = r.emoji.ifBlank { "🎟️" }, perk = r.perk,
            ),
        )
    }

    private suspend fun ask(info: AccessAsk): Boolean {
        if (_pending.value != null) return false // already asking: do not stack sheets
        val p = PendingAsk(info)
        _pending.value = p
        TwinSound.tap()
        return try {
            p.answer.await()
        } finally {
            if (_pending.value === p) _pending.value = null
        }
    }

    /** "Maybe later". */
    fun dismiss(p: PendingAsk) {
        p.answer.complete(false)
        if (_pending.value === p) _pending.value = null
    }

    /** "Get coins free": closes the sheet and takes the person to where coins are earned. */
    fun goEarnCoins(p: PendingAsk) {
        dismiss(p)
        _openRewards.tryEmit(Unit)
    }

    /** Buys the pass (coins are taken once). On success the sheet closes and the original action goes ahead. */
    suspend fun unlock(p: PendingAsk): PassResult {
        val s = sessionStore.session.first() ?: return PassResult.Failed("Please sign in again.")
        val body = AccessPassBody(s.coupleId, s.userId, p.info.feature.key, "ag_" + UUID.randomUUID().toString().replace("-", ""))
        return when (val res = safeApiCall { api.pass(body, s.userId) }) {
            is AppResult.Success -> {
                val text = untilText(res.data.expiresAt)
                TwinSound.unlock()
                toast("🔓 Unlocked until $text. Enjoy!")
                p.answer.complete(true)
                if (_pending.value === p) _pending.value = null
                PassResult.Unlocked(text)
            }
            is AppResult.Failure -> {
                val e = res.error
                if (e is AppError.Server && e.code == 402) {
                    TwinSound.error()
                    dismiss(p)
                    _openRewards.tryEmit(Unit)
                    PassResult.NotEnoughCoins
                } else {
                    TwinSound.error()
                    PassResult.Failed("Could not unlock. You were not charged.")
                }
            }
        }
    }

    private fun untilText(iso: String): String = try {
        val zone = ZoneId.systemDefault()
        val at = Instant.parse(iso).atZone(zone)
        val time = at.format(DateTimeFormatter.ofPattern("h:mm a"))
        if (at.toLocalDate() == java.time.LocalDate.now(zone)) time else "tomorrow $time"
    } catch (_: Exception) {
        "later today"
    }

    private fun toast(text: String) {
        main.post { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    }
}
