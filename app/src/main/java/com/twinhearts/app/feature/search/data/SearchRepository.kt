package com.twinhearts.app.feature.search.data

import android.content.Context
import androidx.annotation.StringRes
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.core.network.unwrapState
import com.twinhearts.app.feature.search.data.remote.SearchApi
import com.twinhearts.app.feature.search.domain.SearchCatalog
import com.twinhearts.app.feature.search.domain.SearchMatcher
import com.twinhearts.app.feature.search.domain.model.SearchDestination
import com.twinhearts.app.feature.search.domain.model.SearchDoc
import com.twinhearts.app.feature.search.domain.model.SearchResult
import com.twinhearts.app.feature.search.domain.model.SearchSection
import com.twinhearts.app.feature.search.domain.stateSources
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: everything Search can look in.
 *  1. The catalog of pages and features: on the phone, instant, works offline.
 *  2. Chat messages: the same server search the Chat page uses.
 *  3. Lists inside the couple's saved document (events, bucket list, notes, fights, money...).
 */
interface SearchRepository {
    /** Pages and features that match [query]. No network. */
    fun catalogResults(query: String): List<SearchResult>

    /** Reads the saved document ahead of time so the first typed letters are answered at once. */
    suspend fun warmUp()

    /** Messages and page content that match [query]. Fails only when both lookups fail. */
    suspend fun contentResults(query: String): AppResult<List<SearchResult>>

    /** The last few searches, newest first (kept on this phone only). */
    val recents: Flow<List<String>>

    suspend fun remember(query: String)
    suspend fun forget(query: String)
    suspend fun clearRecents()
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val api: SearchApi,
    private val sessionStore: SessionStore,
    private val history: SearchHistory,
    @ApplicationContext private val context: Context,
) : SearchRepository {

    override val recents: Flow<List<String>> = history.recents
    override suspend fun remember(query: String) = history.remember(query)
    override suspend fun forget(query: String) = history.forget(query)
    override suspend fun clearRecents() = history.clear()

    private var docsLocale: Locale? = null
    private var docs: List<SearchDoc> = emptyList()

    private val stateLock = Mutex()
    private var stateDoc: JsonObject? = null
    private var stateAt = 0L

    override fun catalogResults(query: String): List<SearchResult> {
        val locale = context.resources.configuration.locales[0]
        if (docsLocale != locale) {
            docs = SearchCatalog.docs(context)
            docsLocale = locale
        }
        return SearchMatcher.rank(query, docs, limit = MAX_CATALOG)
    }

    override suspend fun warmUp() {
        val session = sessionStore.session.first() ?: return
        loadState(session)
    }

    override suspend fun contentResults(query: String): AppResult<List<SearchResult>> {
        val session = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        val words = SearchMatcher.tokens(query)
        if (words.isEmpty()) return AppResult.Success(emptyList())
        return coroutineScope {
            val messages = async { messages(session, query) }
            val inside = async { inside(session, query, words) }
            val m = messages.await()
            val s = inside.await()
            if (m is AppResult.Failure && s is AppResult.Failure) {
                m
            } else {
                AppResult.Success(okList(s) + okList(m))
            }
        }
    }

    private fun okList(r: AppResult<List<SearchResult>>): List<SearchResult> = (r as? AppResult.Success)?.data.orEmpty()

    // ── Chat messages ──────────────────────────────────────────────────────────────────────

    private suspend fun messages(session: Session, query: String): AppResult<List<SearchResult>> {
        val answer = safeApiCall { api.chat(session.coupleId, query.trim()) }
        if (answer is AppResult.Failure) return answer
        val rows = (answer as AppResult.Success).data.toFoundMessages(session.role)
        val words = SearchMatcher.tokens(query)
        val chatTitle = context.getString(R.string.core_nav_chat)
        val me = context.getString(R.string.search_from_me)
        val results = rows.sortedByDescending { it.createdAt }.take(MAX_MESSAGES).map { m ->
            val who = if (m.senderRole == session.role) me else session.partnerName
            val date = formatTimestamp(m.createdAt)
            SearchResult(
                key = "msg:${m.id}",
                section = SearchSection.Messages,
                title = SearchMatcher.snippet(m.text, words),
                detail = listOf(who, date).filter { it.isNotBlank() }.joinToString(" · "),
                where = chatTitle,
                emoji = "💬",
                destination = SearchDestination.Chat,
                score = 0,
            )
        }
        return AppResult.Success(results)
    }

    // ── Lists inside the saved document ───────────────────────────────────────────────────

    private suspend fun inside(session: Session, query: String, words: List<String>): AppResult<List<SearchResult>> {
        val doc = when (val r = loadState(session)) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val bodies = HashMap<String, String>()
        val rows = ArrayList<SearchDoc>()
        for (source in stateSources) {
            val where = context.getString(pageTitleRes(source.destination))
            val label = context.getString(source.label)
            for (item in doc.itemsOf(source, session.role)) {
                val key = "state:${source.key}:${item.id}"
                bodies[key] = item.body
                val date = formatDate(item.date)
                rows += SearchDoc(
                    key = key,
                    section = SearchSection.Inside,
                    title = item.title,
                    detail = listOf(label, date).filter { it.isNotBlank() }.joinToString(" · "),
                    where = where,
                    emoji = source.emoji,
                    destination = source.destination,
                    keywords = item.body,
                )
            }
        }
        val ranked = SearchMatcher.rank(query, rows, limit = MAX_INSIDE).map { r ->
            // When only the body matched, show the matching part so the person sees why it was found.
            val body = bodies[r.key].orEmpty()
            val titleHit = words.any { SearchMatcher.normalize(r.title).contains(it) }
            if (!titleHit && body.isNotBlank()) {
                r.copy(detail = SearchMatcher.snippet(body, words, radius = 40))
            } else {
                r
            }
        }
        return AppResult.Success(ranked)
    }

    private suspend fun loadState(session: Session): AppResult<JsonObject> {
        return stateLock.withLock {
            val cached = stateDoc
            if (cached != null && System.currentTimeMillis() - stateAt < STATE_TTL_MS) {
                AppResult.Success(cached)
            } else {
                when (val r = safeApiCall { api.state(session.coupleId) }) {
                    is AppResult.Failure -> if (cached != null) AppResult.Success(cached) else r
                    is AppResult.Success -> {
                        val doc = unwrapState(r.data) as? JsonObject ?: JsonObject(emptyMap())
                        stateDoc = doc
                        stateAt = System.currentTimeMillis()
                        AppResult.Success(doc)
                    }
                }
            }
        }
    }

    // ── Small helpers ─────────────────────────────────────────────────────────────────────

    @StringRes
    private fun pageTitleRes(destination: SearchDestination): Int = when (destination) {
        SearchDestination.Calendar -> R.string.home_page_calendar
        SearchDestination.BucketList -> R.string.home_page_bucket_list
        SearchDestination.Profile -> R.string.home_page_profile
        SearchDestination.MySpace -> R.string.home_page_my_space
        SearchDestination.Money -> R.string.home_page_money
        SearchDestination.Fights -> R.string.home_page_fights
        SearchDestination.Surprises -> R.string.home_page_surprises
        SearchDestination.Memories -> R.string.home_chip_memories
        else -> R.string.core_search
    }

    private fun formatDate(raw: String): String =
        runCatching {
            LocalDate.parse(raw.take(DATE_LENGTH)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }.getOrDefault("")

    private fun formatTimestamp(raw: String): String =
        runCatching {
            Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }.getOrElse { formatDate(raw) }

    private companion object {
        const val MAX_CATALOG = 30
        const val MAX_MESSAGES = 30
        const val MAX_INSIDE = 30
        const val STATE_TTL_MS = 60_000L
        const val DATE_LENGTH = 10
    }
}
