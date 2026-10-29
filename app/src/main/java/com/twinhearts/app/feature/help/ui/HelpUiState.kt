package com.twinhearts.app.feature.help.ui

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.HelpCategory
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode

/** The two tabs of the page: the question list and the chat. */
enum class HelpTab { Faq, Chat }

/** Is the chat connected to a support conversation on the server? */
sealed interface ChatLink {
    /** The chat tab was not opened yet. */
    data object Idle : ChatLink
    data object Connecting : ChatLink
    data object Connected : ChatLink
    data class Failed(val error: AppError) : ChatLink
}

data class ChatState(
    val link: ChatLink = ChatLink.Idle,
    val ticketId: String? = null,
    /** The mode the toggle shows. It changes at once when tapped, before the server agrees. */
    val mode: SupportMode = SupportMode.Ai,
    /** The mode the server has for this conversation. Null until connected. */
    val ticketMode: SupportMode? = null,
    val messages: List<SupportMessage> = emptyList(),
    val draft: String = "",
    /** True while my message is on its way (shows the typing line). */
    val sending: Boolean = false,
)

data class HelpUiState(
    val tab: HelpTab = HelpTab.Faq,
    val faqs: UiState<List<FaqItem>> = UiState.Loading,
    val query: String = "",
    val category: HelpCategory = HelpCategory.All,
    /** Questions that are open. The web lets several be open together. */
    val expandedIds: Set<String> = emptySet(),
    val chat: ChatState = ChatState(),
    /** Empty when the account has no name; the screen then shows a default. */
    val myName: String = "",
)

/** One-off things shown once as a toast (R-42: not part of the long-lived state). */
sealed interface HelpEvent {
    /** A message reached the care team (Live Agent mode). */
    data object MessageSent : HelpEvent
    data object ConnectFailed : HelpEvent
    data object SendFailed : HelpEvent

    /** I asked the AI but the server did not write an answer. */
    data object AiUnavailable : HelpEvent
    data class Failed(val error: AppError) : HelpEvent
}
