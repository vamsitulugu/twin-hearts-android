package com.twinhearts.app.core.pro

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Razorpay reports its result to the Activity (MainActivity implements the listener).
 * The Activity hands it to this bridge; ProViewModel listens. Keeps Razorpay out of the ViewModel.
 */
@Singleton
class PaymentBridge @Inject constructor() {
    private val _results = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 4)
    val results: SharedFlow<PaymentResult> = _results.asSharedFlow()

    fun success(orderId: String?, paymentId: String?, signature: String?) {
        _results.tryEmit(
            if (orderId.isNullOrBlank() || paymentId.isNullOrBlank() || signature.isNullOrBlank()) {
                PaymentResult.Failed("Payment details were incomplete.")
            } else {
                PaymentResult.Success(orderId, paymentId, signature)
            },
        )
    }

    /** Razorpay code 0 means the person closed the payment window. */
    fun error(code: Int, message: String?) {
        _results.tryEmit(
            if (code == CODE_CANCELLED) PaymentResult.Cancelled else PaymentResult.Failed(readableMessage(message)),
        )
    }

    companion object {
        const val CODE_CANCELLED = 0

        /** Razorpay sends the reason as JSON text like {"error":{"description":"..."}}; pull the sentence out. */
        internal fun readableMessage(raw: String?): String {
            if (raw.isNullOrBlank()) return DEFAULT_FAILURE
            val d = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1)
            return d ?: raw.takeIf { !it.trimStart().startsWith("{") } ?: DEFAULT_FAILURE
        }

        const val DEFAULT_FAILURE = "Payment failed. You were not charged. Please try again."
    }
}
