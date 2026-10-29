package com.twinhearts.app.feature.help.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.ui.components.HelpChatView
import com.twinhearts.app.feature.help.ui.components.HelpFaqView
import java.time.Instant

/* Previews for Android Studio only: question list (content, empty, error, large font) and chat (welcome, thread, error). */

private val sampleFaqs = listOf(
    FaqItem(
        id = "faq-pair",
        category = "Pairing & Account",
        question = "How do I connect / pair with my partner?",
        answer = "Go to Settings > Partner Connection, enter your partner's phone number and send the request.",
        tags = listOf("pairing"),
    ),
    FaqItem(
        id = "faq-pin",
        category = "Privacy & Security",
        question = "How does the Secret Vault and PIN work?",
        answer = "Your Vault is a private space protected by a 4-digit PIN.",
        tags = listOf("vault"),
    ),
)

private val sampleMessages = listOf(
    SupportMessage("1", SenderKind.User, "Vamsi", "How do I pair with my partner?", Instant.parse("2026-10-29T10:15:00Z")),
    SupportMessage("2", SenderKind.Ai, "Twin Care AI", "Open Settings > Partner Connection and send a request.", Instant.parse("2026-10-29T10:15:05Z")),
    SupportMessage("3", SenderKind.Admin, "Support Agent", "Hi! Anything else I can help with?", Instant.parse("2026-10-29T10:20:00Z")),
)

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    TwinTheme { TwinBackground { content() } }
}

@Composable
private fun FaqPreview(state: HelpUiState) = PreviewFrame {
    HelpFaqView(
        state = state,
        onQueryChange = {},
        onCategory = {},
        onToggle = {},
        onAskAi = {},
        onMessageSupport = {},
        onStartChat = {},
        onRetry = {},
    )
}

@Composable
private fun ChatPreview(chat: ChatState) = PreviewFrame {
    HelpChatView(
        chat = chat,
        myName = "Vamsi",
        onModeSelect = {},
        onDraftChange = {},
        onSend = {},
        onQuick = {},
        onRetry = {},
    )
}

@Preview(name = "Questions", showBackground = true, heightDp = 800)
@Composable
private fun FaqContentPreview() = FaqPreview(
    HelpUiState(faqs = UiState.Content(sampleFaqs), expandedIds = setOf("faq-pair")),
)

@Preview(name = "Questions - large font", showBackground = true, heightDp = 800, fontScale = 1.3f)
@Composable
private fun FaqLargeFontPreview() = FaqPreview(
    HelpUiState(faqs = UiState.Content(sampleFaqs), expandedIds = setOf("faq-pair")),
)

@Preview(name = "Questions - no match", showBackground = true, heightDp = 800)
@Composable
private fun FaqEmptyPreview() = FaqPreview(
    HelpUiState(faqs = UiState.Content(sampleFaqs), query = "zebra"),
)

@Preview(name = "Questions - loading", showBackground = true, heightDp = 800)
@Composable
private fun FaqLoadingPreview() = FaqPreview(HelpUiState(faqs = UiState.Loading))

@Preview(name = "Questions - error", showBackground = true, heightDp = 800)
@Composable
private fun FaqErrorPreview() = FaqPreview(HelpUiState(faqs = UiState.Error(AppError.Network)))

@Preview(name = "Chat - welcome", showBackground = true, heightDp = 800)
@Composable
private fun ChatWelcomePreview() = ChatPreview(ChatState(link = ChatLink.Connected))

@Preview(name = "Chat - thread", showBackground = true, heightDp = 800)
@Composable
private fun ChatThreadPreview() = ChatPreview(
    ChatState(link = ChatLink.Connected, messages = sampleMessages, sending = true, draft = "Thanks!"),
)

@Preview(name = "Chat - live agent, large font", showBackground = true, heightDp = 800, fontScale = 1.3f)
@Composable
private fun ChatLargeFontPreview() = ChatPreview(
    ChatState(link = ChatLink.Connected, mode = SupportMode.Human, messages = sampleMessages),
)

@Preview(name = "Chat - connecting", showBackground = true, heightDp = 800)
@Composable
private fun ChatConnectingPreview() = ChatPreview(ChatState(link = ChatLink.Connecting))

@Preview(name = "Chat - error", showBackground = true, heightDp = 800)
@Composable
private fun ChatErrorPreview() = ChatPreview(ChatState(link = ChatLink.Failed(AppError.Network)))
