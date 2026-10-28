package com.twinhearts.app.feature.twinai.data.realtime

/**
 * Why this file exists: live BOTH updates need your Supabase project address and its PUBLIC
 * "anon" key (the same two values the web page uses as window.__SUPABASE_URL__ and
 * window.__SUPABASE_ANON_KEY__). Paste them below. While they are empty, BOTH keeps working by
 * asking the server every few seconds. Never paste the secret "service" key here.
 */
object RealtimeConfig {
    /** Example: "https://abcdxyz.supabase.co" */
    const val SUPABASE_URL = ""

    /** The public anon key (starts with "eyJ"). */
    const val ANON_KEY = ""

    val isConfigured: Boolean get() = SUPABASE_URL.isNotBlank() && ANON_KEY.isNotBlank()
}
