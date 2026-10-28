package com.twinhearts.app.feature.twinai.domain.model

/** Why this file exists: the Pro plans and the form the "Go Pro" payment steps collect (same as the web's pro-card.js). */
enum class ProPlan(val key: String, val amount: Int) {
    Monthly("monthly", 99),
    Lifetime("lifetime", 499),
}

/** Where payments go; these are the same UPI details the web app shows. */
object ProPayment {
    const val UPI_ID = "7077745442@upi"
    const val UPI_NAME = "Tulugu Vamsi"

    /** A UPI deep link; any UPI app on the phone can open it. */
    fun upiLink(plan: ProPlan, coupleId: String, label: String): String =
        "upi://pay?pa=${android.net.Uri.encode(UPI_ID)}&pn=${android.net.Uri.encode(UPI_NAME)}" +
            "&am=${plan.amount}&cu=INR&tn=${android.net.Uri.encode("$label · $coupleId")}"
}

data class ProUpgradeForm(
    val plan: ProPlan,
    val paidName: String = "",
    val paidPhone: String = "",
    val partnerName: String = "",
    val partnerPhone: String = "",
    val transactionId: String = "",
    val note: String = "",
) {
    /** Everything the web form marks as required. */
    val isComplete: Boolean
        get() = paidName.isNotBlank() && paidPhone.isNotBlank() && partnerName.isNotBlank() &&
            partnerPhone.isNotBlank() && transactionId.isNotBlank()
}
