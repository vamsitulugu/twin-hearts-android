package com.twinhearts.app.feature.help.domain.model

import java.time.Instant

/*
 * Plain Kotlin models the Help screens use. They know nothing about Retrofit or Compose, so the
 * UI and the tests can build them directly (README section 5).
 */

/** Who answers a support chat: the AI helper, or a person on the admin dashboard. */
enum class SupportMode(val wire: String) {
    Ai("ai"),
    Human("human"),
    ;

    companion object {
        /** The server sends "ai" or "human". Anything else is treated as AI, like the web. */
        fun fromWire(value: String?): SupportMode = if (value == "human") Human else Ai
    }
}

/** Who wrote a chat message. */
enum class SenderKind {
    User,
    Ai,
    Admin,
    ;

    companion object {
        fun fromWire(value: String?): SenderKind = when (value) {
            "ai" -> Ai
            "admin" -> Admin
            else -> User
        }
    }
}

/**
 * The topic chips above the question list. [serverName] is the `category` text the server puts on
 * each question (the chip label is shorter than the server's text); null means "show all".
 */
enum class HelpCategory(val serverName: String?) {
    All(null),
    Pairing("Pairing & Account"),
    Privacy("Privacy & Security"),
    Globe("Memory Globe & Map"),
    Music("Music & Entertainment"),
    Notifications("Notifications & Alerts"),
    Sync("Troubleshooting & Sync"),
}

/** One question and answer from `GET /api/support/faqs`. */
data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String,
    val tags: List<String>,
)

data class SupportMessage(
    val id: String,
    val sender: SenderKind,
    /** The name the server stored. May be empty; the UI then shows a default label. */
    val senderName: String,
    val text: String,
    val createdAt: Instant?,
)

/** A support conversation: its id, who is answering, and every message so far. */
data class SupportTicket(
    val id: String,
    val mode: SupportMode,
    val messages: List<SupportMessage>,
)

/** What the server answers after a message is sent. Every part can be missing. */
data class SendResult(
    val ticket: SupportTicket?,
    val userMessage: SupportMessage?,
    val aiMessage: SupportMessage?,
)
