package com.twinhearts.app.feature.rewards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.rewards.data.RewardsRepository
import com.twinhearts.app.feature.rewards.domain.Gift
import com.twinhearts.app.feature.rewards.domain.GiftShop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** A short line under the gift list. The server's own text is shown as it is. */
sealed interface GiftNotice {
    data object NotEnough : GiftNotice
    data object Offline : GiftNotice
    data class Server(val text: String?) : GiftNotice
}

data class GiftShopState(
    val shop: GiftShop? = null,
    val loading: Boolean = true,
    val error: AppError? = null,
    val selectedId: String? = null,
    val sending: Boolean = false,
    val notice: GiftNotice? = null,
    /** Set once after a successful send; the sheet reads it, tells the chat, then calls [GiftShopViewModel.consumeSent]. */
    val sent: Gift? = null,
    val partnerName: String = "",
)

/**
 * Twin Rewards Step 2: the Gift Shop. The server owns every price and charges the coins; this class only
 * asks. [tapId] stays the same while a Send is being retried, so a lost answer can never charge twice.
 */
@HiltViewModel
class GiftShopViewModel @Inject constructor(
    private val repository: RewardsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GiftShopState())
    val state: StateFlow<GiftShopState> = _state.asStateFlow()

    private var tapId: String? = null

    /** Called every time the sheet opens: fresh balance and catalog, nothing selected. */
    fun open() {
        tapId = null
        _state.update { it.copy(selectedId = null, notice = null, sending = false, sent = null) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val partner = repository.partnerName()
            when (val r = repository.gifts()) {
                is AppResult.Success -> _state.update { it.copy(shop = r.data, loading = false, error = null, partnerName = partner) }
                // Keep an older list on screen if a refresh fails; show the error only when there is nothing to show.
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = if (it.shop == null) r.error else null, partnerName = partner) }
            }
        }
    }

    fun select(id: String) {
        if (_state.value.sending) return
        _state.update { it.copy(selectedId = if (it.selectedId == id) null else id, notice = null) }
        tapId = null
    }

    fun send() {
        val current = _state.value
        if (current.sending) return
        val gift = current.shop?.gifts?.firstOrNull { it.id == current.selectedId } ?: return
        val tap = tapId ?: ("gift_" + UUID.randomUUID().toString().replace("-", "").take(TAP_LENGTH)).also { tapId = it }
        _state.update { it.copy(sending = true, notice = null) }
        viewModelScope.launch {
            when (val r = repository.sendGift(gift.id, tap)) {
                is AppResult.Success -> {
                    tapId = null
                    _state.update {
                        it.copy(
                            sending = false,
                            selectedId = null,
                            shop = it.shop?.copy(balance = r.data.balance),
                            sent = r.data.gift ?: gift,
                        )
                    }
                }
                is AppResult.Failure -> {
                    val e = r.error
                    val notice: GiftNotice = when {
                        e is AppError.Server && e.code == HTTP_PAYMENT_REQUIRED -> GiftNotice.NotEnough
                        e is AppError.Server -> GiftNotice.Server(e.message)
                        e is AppError.Unauthorized -> GiftNotice.Server(e.message)
                        else -> GiftNotice.Offline // network or unreadable answer: keep the same tap id, a retry is safe
                    }
                    // A refusal from the server is final for this tap; only a network problem keeps the id for a safe retry.
                    if (e is AppError.Server || e is AppError.Unauthorized) tapId = null
                    _state.update { it.copy(sending = false, notice = notice) }
                    if (notice == GiftNotice.NotEnough) refresh()
                }
            }
        }
    }

    fun consumeSent() = _state.update { it.copy(sent = null) }

    private companion object {
        const val TAP_LENGTH = 16
        const val HTTP_PAYMENT_REQUIRED = 402
    }
}
