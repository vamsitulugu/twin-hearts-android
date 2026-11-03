package com.twinhearts.app.feature.rewards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.rewards.data.RewardsRepository
import com.twinhearts.app.feature.storefx.StoreFxState
import com.twinhearts.app.feature.rewards.domain.AdClaim
import com.twinhearts.app.feature.rewards.domain.CoinTx
import com.twinhearts.app.feature.rewards.domain.EarnItem
import com.twinhearts.app.feature.rewards.domain.PendingAdReward
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import com.twinhearts.app.feature.rewards.domain.SpinResult
import com.twinhearts.app.feature.rewards.domain.StoreItem
import com.twinhearts.app.feature.rewards.domain.StorePurchaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RewardsUiState(
    val summary: RewardsSummary? = null,
    val history: List<CoinTx>? = null,
    val loading: Boolean = true,
    val claiming: Boolean = false,
    val spinning: Boolean = false,
    val scratching: Boolean = false,
    val error: AppError? = null,
    /** Coins just won; the screen shows it once, then calls [RewardsViewModel.consumeEarned]. */
    val justEarned: Int = 0,
    val partnerName: String = "",
    val lastSpinResult: SpinResult? = null,
    val lastScratchResult: ScratchResult? = null,
    /** Phase 1: Earn more coins list */
    val earnItems: List<EarnItem> = emptyList(),
    /** Phase 3: true while a rewarded ad is being requested / played / cashed in. */
    val adBusy: Boolean = false,
    /** An ad was watched and its scratch card is on screen; the coins are paid when it is scratched. */
    val adReward: PendingAdReward? = null,
    /** Step 4: Twin Store */
    val storeItems: List<StoreItem> = emptyList(),
    val unlockedItemIds: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap(),
    val storeFilter: String = "all",
    val purchasingItemId: String? = null,
    val equippingItemId: String? = null,
)

/** One ViewModel for the Rewards page and the Home card. The server owns every number. */
@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RewardsUiState())
    val state: StateFlow<RewardsUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { _state.update { it.copy(partnerName = repository.partnerName()) } }
    }

    fun refresh(withHistory: Boolean = false) {
        viewModelScope.launch {
            when (val r = repository.summary()) {
                is AppResult.Success -> {
                    // Twin Store: chat, music, avatars, Home and games follow what the server says is equipped.
                    StoreFxState.apply(r.data.equipped, r.data.partnerFrame)
                    _state.update {
                    it.copy(
                        summary = r.data,
                        unlockedItemIds = if (r.data.unlockedItemIds.isNotEmpty()) r.data.unlockedItemIds else it.unlockedItemIds,
                        equipped = r.data.equipped,
                        loading = false,
                        error = null,
                    )
                    }
                }
                // Keep an older result on screen if a refresh fails; only show the error when there is nothing to show.
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = if (it.summary == null) r.error else null) }
            }
            if (withHistory) {
                loadHistory()
                loadStore()
            }
        }
    }

    /**
     * Phase 3: watch a rewarded ad for coins. [show] plays the ad (the screen supplies it, so this class needs no
     * Android types) and calls exactly one of its two callbacks. The server pays; nothing is added here by guess.
     */
    fun watchAdForCoins(show: (onEarned: () -> Unit, onFail: () -> Unit) -> Unit) {
        val summary = _state.value.summary ?: return
        if (_state.value.adBusy || summary.adCoinsLeft <= 0) return
        _state.update { it.copy(adBusy = true) }
        viewModelScope.launch {
            when (val t = repository.adStart("coins")) {
                is AppResult.Failure -> _state.update { it.copy(adBusy = false, error = t.error) }
                is AppResult.Success -> show(
                    {
                        // The ad is done. The coins are NOT added yet: a scratch card opens first, and the
                        // server is asked to pay only when the person has scratched it (claimAdReward).
                        _state.update { it.copy(adReward = PendingAdReward(t.data.token, t.data.coins)) }
                    },
                    { _state.update { it.copy(adBusy = false) } },
                )
            }
        }
    }

    private var heldAdClaim: AdClaim? = null

    /** Called by the scratch card once it is scratched. Returns the coins the server paid, or null if it could not. */
    suspend fun claimAdReward(): Int? {
        val reward = _state.value.adReward ?: return null
        return when (val c = repository.adClaim(reward.token)) {
            is AppResult.Success -> {
                // Keep the answer; the balance on the page changes only when the card is closed, so the person sees it count up.
                heldAdClaim = c.data
                c.data.earned
            }
            is AppResult.Failure -> {
                _state.update { it.copy(error = c.error) }
                null
            }
        }
    }

    /** The scratch card was closed: now the balance on the page counts up to what the server said. */
    fun finishAdReward() {
        val claim = heldAdClaim
        heldAdClaim = null
        _state.update {
            it.copy(
                adBusy = false,
                adReward = null,
                summary = if (claim != null) it.summary?.copy(balance = claim.balance, adCoinsLeft = claim.remaining) else it.summary,
            )
        }
    }

    /** Phase 3: watch a rewarded ad for one free extra spin. The wheel screen sees `spinning` turn on and starts turning. */
    fun watchAdForSpin(show: (onEarned: () -> Unit, onFail: () -> Unit) -> Unit) {
        val summary = _state.value.summary ?: return
        if (_state.value.adBusy || _state.value.spinning || !summary.spunToday || summary.adSpinsLeft <= 0) return
        _state.update { it.copy(adBusy = true) }
        viewModelScope.launch {
            when (val t = repository.adStart("spin")) {
                is AppResult.Failure -> _state.update { it.copy(adBusy = false, error = t.error) }
                is AppResult.Success -> show(
                    {
                        _state.update { it.copy(adBusy = false) }
                        spin(adToken = t.data.token)
                    },
                    { _state.update { it.copy(adBusy = false) } },
                )
            }
        }
    }

    fun loadEarn() {
        viewModelScope.launch {
            val r = repository.earnStatus()
            if (r is AppResult.Success) _state.update { it.copy(earnItems = r.data) }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val r = repository.history()
            if (r is AppResult.Success) _state.update { it.copy(history = r.data) }
        }
    }

    fun claim() {
        val current = _state.value
        if (current.claiming || current.summary?.checkedInToday != false) return
        _state.update { it.copy(claiming = true) }
        viewModelScope.launch {
            when (val r = repository.checkIn()) {
                is AppResult.Success -> {
                    _state.update { it.copy(summary = r.data, claiming = false, error = null, justEarned = if (r.data.claimed) r.data.earned else 0) }
                    loadHistory()
                }
                is AppResult.Failure -> _state.update { it.copy(claiming = false, error = if (it.summary == null) r.error else it.error) }
            }
        }
    }

    fun spin(adToken: String? = null, onSuccess: (SpinResult) -> Unit = {}) {
        val current = _state.value
        val summary = current.summary ?: return
        if (current.spinning) return
        // Phase 2: after the free daily spin an extra spin costs coins (price and daily cap come from the server).
        val viaAd = adToken != null
        val paid = summary.spunToday && !viaAd
        if (paid && (summary.paidSpinsLeft <= 0 || summary.balance < summary.paidSpinPrice)) return
        _state.update { it.copy(spinning = true, lastSpinResult = null) }
        viewModelScope.launch {
            val tapId = "spin_" + UUID.randomUUID().toString()
            when (val r = repository.spin(tapId, paid, adToken)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            spinning = false,
                            lastSpinResult = r.data,
                            summary = it.summary?.copy(
                                balance = r.data.balance,
                                spunToday = true,
                                spinsAvailable = 0,
                                paidSpinsLeft = if (paid) (summary.paidSpinsLeft - 1).coerceAtLeast(0) else summary.paidSpinsLeft,
                                adSpinsLeft = if (viaAd) (summary.adSpinsLeft - 1).coerceAtLeast(0) else summary.adSpinsLeft,
                            ),
                            justEarned = r.data.coins,
                        )
                    }
                    loadHistory()
                    onSuccess(r.data)
                }
                is AppResult.Failure -> _state.update { it.copy(spinning = false, error = r.error) }
            }
        }
    }

    fun scratch(onSuccess: (ScratchResult) -> Unit = {}) {
        val current = _state.value
        if (current.scratching || current.summary?.scratchedToday != false) return
        _state.update { it.copy(scratching = true) }
        viewModelScope.launch {
            val tapId = "scratch_" + UUID.randomUUID().toString()
            when (val r = repository.scratch(tapId)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            scratching = false,
                            lastScratchResult = r.data,
                            summary = it.summary?.copy(
                                balance = r.data.balance,
                                scratchedToday = true,
                                scratchesAvailable = 0,
                            ),
                            justEarned = r.data.coins,
                        )
                    }
                    loadHistory()
                    onSuccess(r.data)
                }
                is AppResult.Failure -> _state.update { it.copy(scratching = false, error = r.error) }
            }
        }
    }

    fun consumeEarned() = _state.update { it.copy(justEarned = 0) }

    fun loadStore() {
        viewModelScope.launch {
            when (val r = repository.store()) {
                is AppResult.Success -> {
                    StoreFxState.apply(r.data.equipped, r.data.partnerFrame)
                    _state.update {
                        it.copy(
                            storeItems = r.data.items,
                            unlockedItemIds = r.data.unlockedItemIds,
                            equipped = r.data.equipped,
                        )
                    }
                }
                is AppResult.Failure -> { /* Keep current on failure */ }
            }
        }
    }

    fun setStoreFilter(filter: String) {
        _state.update { it.copy(storeFilter = filter) }
    }

    fun buyItem(item: StoreItem, onSuccess: (StorePurchaseResult) -> Unit = {}) {
        val current = _state.value
        if (current.purchasingItemId != null) return
        val currentBalance = current.summary?.balance ?: 0
        if (currentBalance < item.price) return

        _state.update { it.copy(purchasingItemId = item.id) }
        viewModelScope.launch {
            val tapId = "buy_" + UUID.randomUUID().toString()
            when (val r = repository.buyItem(item.id, tapId)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            purchasingItemId = null,
                            unlockedItemIds = r.data.unlockedItemIds,
                            summary = it.summary?.copy(balance = r.data.balance),
                        )
                    }
                    loadHistory()
                    onSuccess(r.data)
                }
                is AppResult.Failure -> _state.update { it.copy(purchasingItemId = null, error = r.error) }
            }
        }
    }

    fun equipItem(category: String, itemId: String) {
        val current = _state.value
        if (current.equippingItemId != null) return
        _state.update { it.copy(equippingItemId = itemId) }
        viewModelScope.launch {
            when (val r = repository.equipItem(category, itemId)) {
                is AppResult.Success -> {
                    // The server answers only {category, itemId}; merge it (blank = unequipped).
                    StoreFxState.applyOne(r.data.category, r.data.itemId)
                    _state.update {
                        it.copy(
                            equippingItemId = null,
                            equipped = if (r.data.itemId.isBlank()) it.equipped - r.data.category else it.equipped + (r.data.category to r.data.itemId),
                        )
                    }
                }
                is AppResult.Failure -> _state.update { it.copy(equippingItemId = null, error = r.error) }
            }
        }
    }
}

