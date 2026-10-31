package com.twinhearts.app.core.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.razorpay.Checkout
import org.json.JSONObject

/** Opens Razorpay's native payment window. The result arrives in MainActivity → [PaymentBridge]. */
fun openRazorpayCheckout(activity: Activity, order: ProOrder): Boolean = try {
    val checkout = Checkout()
    checkout.setKeyID(order.keyId)
    val prefill = JSONObject().apply {
        if (order.name.isNotBlank()) put("name", order.name)
        if (order.contact.isNotBlank()) put("contact", order.contact)
        if (order.email.isNotBlank()) put("email", order.email)
    }
    val options = JSONObject().apply {
        put("name", "Twin Hearts")
        put("description", "Pro · ${order.planLabel}")
        put("order_id", order.orderId)
        put("currency", order.currency)
        put("amount", order.amountPaise)
        put("prefill", prefill)
        put("theme", JSONObject().put("color", "#7A0B44"))
        put("retry", JSONObject().put("enabled", true).put("max_count", 3))
    }
    checkout.open(activity, options)
    true
} catch (e: Exception) {
    false
}

/** Compose gives a wrapped Context; Razorpay needs the real Activity under it. */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
