package com.twinhearts.app.core.pro

import java.time.Instant

/** The three Pro plans. Prices here are for display only: the server decides what is charged. */
enum class ProPlan(val id: String, val rupees: Int, val days: Int?) {
    Monthly("monthly", 99, 30),
    Yearly("yearly", 499, 365),
    Lifetime("lifetime", 999, null);

    companion object {
        fun fromId(id: String?): ProPlan? = entries.firstOrNull { it.id == id }
    }
}

/** Whether this couple has Pro right now. Pro belongs to the couple: either partner paying unlocks both. */
data class ProStatus(
    val isPro: Boolean = false,
    val plan: ProPlan? = null,
    /** ISO time the plan ends; null for Lifetime or when not Pro. */
    val expiresAt: String? = null,
) {
    /** True only while the plan has not run out, even if the phone was offline since the last check. */
    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isPro) return false
        if (plan == ProPlan.Lifetime || expiresAt == null) return true
        val end = runCatching { Instant.parse(expiresAt).toEpochMilli() }.getOrNull() ?: return false
        return end > nowMillis
    }

    companion object {
        val Free = ProStatus()
    }
}

/** What Razorpay checkout needs to open, created by the server for one payment. */
data class ProOrder(
    val keyId: String,
    val orderId: String,
    val amountPaise: Long,
    val currency: String,
    val plan: ProPlan,
    val planLabel: String,
    val name: String,
    val contact: String,
    val email: String,
)

data class ProPayment(val plan: ProPlan?, val rupees: Int, val paymentId: String?, val paidAt: String?)

/** What Razorpay's checkout reported back (see PaymentBridge). */
sealed interface PaymentResult {
    data class Success(val orderId: String, val paymentId: String, val signature: String) : PaymentResult
    data object Cancelled : PaymentResult
    data class Failed(val message: String) : PaymentResult
}
