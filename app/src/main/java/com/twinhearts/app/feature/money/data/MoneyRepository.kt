package com.twinhearts.app.feature.money.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.data.StateCache
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.money.data.remote.MoneyApi
import com.twinhearts.app.feature.money.data.remote.MoneySaveBody
import com.twinhearts.app.feature.money.domain.model.MoneyData
import com.twinhearts.app.feature.money.domain.model.NewTransaction
import com.twinhearts.app.feature.money.domain.model.Transaction
import com.twinhearts.app.feature.money.domain.model.TxType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject

interface MoneyRepository {
    /** The last copy saved on this phone, or null. Used to paint at once and to work offline. */
    suspend fun cachedData(): MoneyData?

    suspend fun load(): AppResult<MoneyData>

    /** Adds a transaction at the top of the list (like the web) and returns the new page data. */
    suspend fun addTransaction(tx: NewTransaction): AppResult<MoneyData>

    suspend fun deleteTransaction(id: String): AppResult<MoneyData>

    /** Saves my monthly spending goal (0 clears it). */
    suspend fun saveGoal(goal: Double): AppResult<MoneyData>
}

class MoneyRepositoryImpl @Inject constructor(
    private val api: MoneyApi,
    private val sessionStore: SessionStore,
    private val cache: StateCache,
) : MoneyRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun cachedData(): MoneyData? {
        val session = sessionStore.session.first() ?: return null
        val json = cache.read(cacheKey(session)) ?: return null
        return (runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject)?.toMoneyData(session)
    }

    override suspend fun load(): AppResult<MoneyData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> {
                remember(session, state.data)
                AppResult.Success(state.data.toMoneyData(session))
            }
            is AppResult.Failure -> state
        }
    }

    override suspend fun addTransaction(tx: NewTransaction): AppResult<MoneyData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val item = buildJsonObject {
            put("id", System.currentTimeMillis())
            put("type", tx.type.key)
            put("desc", tx.description)
            putAmount("amt", tx.amount)
            put("by", session.role)
            put("category", tx.category.ifBlank { "Other" })
            put("date", tx.date.toString())
        }
        // Web does `unshift`: newest first. Old items are kept exactly as they were saved.
        val next = JsonArray(listOf<JsonElement>(item) + state.rawTransactions())
        return saveAndReturn(session, state, "transactions", next)
    }

    override suspend fun deleteTransaction(id: String): AppResult<MoneyData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val kept = JsonArray(state.rawTransactions().filterNot { (it as? JsonObject)?.get("id").text() == id })
        return saveAndReturn(session, state, "transactions", kept)
    }

    override suspend fun saveGoal(goal: Double): AppResult<MoneyData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        // The server replaces the whole `finGoals` key, so keep my partner's goal in it.
        val goals = (state["finGoals"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        goals[session.role] = JsonPrimitive(goal)
        return saveAndReturn(session, state, "finGoals", JsonObject(goals))
    }

    /** Saves one top-level key, then returns the page data with that key swapped in. */
    private suspend fun saveAndReturn(
        session: Session,
        state: JsonObject,
        key: String,
        value: JsonElement,
    ): AppResult<MoneyData> {
        val patch = buildJsonObject { put(key, value) }
        val body = MoneySaveBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> {
                val next = JsonObject(state + (key to value))
                remember(session, next)
                AppResult.Success(next.toMoneyData(session))
            }
            is AppResult.Failure -> result
        }
    }

    private fun cacheKey(session: Session) = "money:${session.coupleId}"

    /** Saves only this page's two keys (never the whole state, which can hold large photos). */
    private suspend fun remember(session: Session, state: JsonObject) {
        val slice = JsonObject(state.filterKeys { it == "transactions" || it == "finGoals" })
        cache.write(cacheKey(session), slice.toString())
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private fun JsonObject.rawTransactions(): JsonArray = this["transactions"] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.toMoneyData(session: Session): MoneyData {
        val goal = ((this["finGoals"] as? JsonObject)?.get(session.role) as? JsonPrimitive)
            ?.contentOrNull?.toDoubleOrNull() ?: 0.0
        return MoneyData(
            transactions = rawTransactions().toTransactions(),
            myGoal = goal.coerceAtLeast(0.0),
            myRole = session.role,
            myName = session.myName,
            partnerName = session.partnerName,
        )
    }

    /** Skips items without a readable amount, so one bad item never hides the rest. */
    private fun JsonArray.toTransactions(): List<Transaction> = mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val amount = obj["amt"].text().toDoubleOrNull()?.takeIf { it.isFinite() } ?: return@mapNotNull null
        // An unreadable date no longer hides the item: it is listed as "No date" and left out of month maths.
        val date = runCatching { LocalDate.parse(obj["date"].text().take(ISO_DATE_LENGTH)) }.getOrNull()
        val description = obj["desc"].text().ifBlank { obj["description"].text() }
        Transaction(
            id = obj["id"].text().ifBlank { "$description|$date|$amount" },
            type = TxType.fromKey(obj["type"].text()),
            description = description,
            amount = amount,
            category = obj["category"].text(),
            date = date,
            by = obj["by"].text(),
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putAmount(key: String, amount: Double) {
        if (amount % 1.0 == 0.0) put(key, amount.toLong()) else put(key, amount)
    }

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    private companion object {
        const val ISO_DATE_LENGTH = 10 // "2026-10-28"; the web may also store a longer timestamp
    }
}
