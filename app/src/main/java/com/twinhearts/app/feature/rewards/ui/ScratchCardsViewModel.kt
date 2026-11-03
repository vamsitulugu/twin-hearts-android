package com.twinhearts.app.feature.rewards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.rewards.data.RewardsRepository
import com.twinhearts.app.feature.rewards.domain.CardPrize
import com.twinhearts.app.feature.rewards.domain.GameCard
import com.twinhearts.app.feature.rewards.domain.RecentCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * My Scratch Cards: the unscratched cards won by finishing couple games.
 * The prize is picked by the server when a card is opened (scratched or Skip); this screen only shows it.
 */
@HiltViewModel
class ScratchCardsViewModel @Inject constructor(
    private val repository: RewardsRepository,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val failed: Boolean = false,
        val cards: List<GameCard> = emptyList(),
        val recent: List<RecentCard> = emptyList(),
        val balance: Int = 0,
        /** The card on screen (being scratched or just opened). Null = the list of cards is shown. */
        val openCard: GameCard? = null,
        val prize: CardPrize? = null,
        val opening: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.opening) return
        viewModelScope.launch {
            _state.update { it.copy(loading = it.cards.isEmpty() && it.recent.isEmpty()) }
            val cards = repository.gameCards()
            val summary = repository.summary()
            _state.update { s ->
                var next = s.copy(loading = false)
                if (cards is AppResult.Success) {
                    next = next.copy(cards = cards.data.cards, recent = cards.data.recent, failed = false)
                } else {
                    next = next.copy(failed = s.cards.isEmpty())
                }
                if (summary is AppResult.Success) next = next.copy(balance = summary.data.balance)
                next
            }
        }
    }

    fun openTile(card: GameCard) {
        if (_state.value.opening) return
        _state.update { it.copy(openCard = card, prize = null) }
    }

    fun closeCard() {
        if (_state.value.opening) return
        _state.update { it.copy(openCard = null, prize = null) }
        load()
    }

    /** Moves on to the next unscratched card (or back to the list when there is none). */
    fun next() {
        if (_state.value.opening) return
        _state.update { s ->
            val following = s.cards.firstOrNull { it.id != s.openCard?.id }
            s.copy(openCard = following, prize = null)
        }
    }

    /** Scratch or Skip: asks the server to open the card. Returns the prize, or null if it could not be opened. */
    suspend fun open(skip: Boolean): CardPrize? {
        val card = _state.value.openCard ?: return null
        _state.value.prize?.let { return it }
        _state.update { it.copy(opening = true) }
        val result = repository.openGameCard(card.id, skip)
        return if (result is AppResult.Success) {
            val prize = result.data
            _state.update { s ->
                s.copy(opening = false, prize = prize, balance = prize.balance, cards = s.cards.filter { it.id != card.id })
            }
            prize
        } else {
            _state.update { it.copy(opening = false) }
            null
        }
    }
}
