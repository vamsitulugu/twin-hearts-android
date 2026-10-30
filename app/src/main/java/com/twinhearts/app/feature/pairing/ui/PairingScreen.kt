package com.twinhearts.app.feature.pairing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.CoupleAvatars
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.pairing.domain.elapsedMinutes
import com.twinhearts.app.feature.pairing.domain.model.IncomingRequest
import com.twinhearts.app.feature.pairing.domain.model.PairingStatus
import kotlinx.coroutines.delay

private const val POLL_MS = 5000L
private val ConnectedAvatarSize = 64.dp
private val RequestAvatarSize = 48.dp

/** Pairing (web: Settings partner cards + the Connection Request popup). Connects the ViewModel to [PairingContent]. */
@Composable
fun PairingScreen(
    onContinue: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    // While the screen is visible, look for news every 5 s (partner accepted / new request).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(POLL_MS)
                viewModel.refresh()
            }
        }
    }
    PairingContent(
        state = state,
        onPhoneChange = viewModel::onPhoneChange,
        onSend = viewModel::sendRequest,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
        onContinue = onContinue,
    )
}

@Composable
internal fun PairingContent(
    state: PairingUiState,
    onPhoneChange: (String) -> Unit,
    onSend: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TwinTopBar(title = stringResource(R.string.pairing_title))
        when (val status = state.status) {
            UiState.Loading -> LoadingSkeleton(
                modifier = Modifier.padding(TwinTheme.spacing.screenEdge),
                lines = 4,
            )
            is UiState.Error -> ErrorState(
                message = status.error.asText(),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            UiState.Empty -> Unit
            is UiState.Content -> Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TwinTheme.spacing.screenEdge)
                    .padding(bottom = TwinTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
            ) {
                PairingBanner(state.message)
                when (val s = status.data) {
                    is PairingStatus.Connected -> ConnectedCard(s.partnerName, onContinue)
                    is PairingStatus.Waiting -> WaitingCard(s, state.isWorking, onCancel, onContinue)
                    else -> {
                        ConnectForm(state, onPhoneChange, onSend)
                        IncomingSection(state.incoming, state.isWorking, onAccept, onDecline)
                        GlassButton(
                            text = stringResource(R.string.pairing_skip),
                            onClick = onContinue,
                            style = GlassButtonStyle.Ghost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingBanner(message: PairingMessage?) {
    if (message == null) return
    val colors = TwinTheme.colors
    val tint = if (message is PairingMessage.Info) colors.success else colors.error
    Text(
        text = message.asText(),
        style = MaterialTheme.typography.bodySmall,
        color = tint,
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.small)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), TwinTheme.shapes.small)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Phone box + "Send Connection Request ❤️". */
@Composable
private fun ConnectForm(
    state: PairingUiState,
    onPhoneChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.xl) {
        Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            TwinTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.pairing_phone_label),
                placeholder = stringResource(R.string.pairing_phone_placeholder),
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onImeAction = onSend,
                enabled = !state.isWorking,
            )
            GlassButton(
                text = stringResource(if (state.isWorking) R.string.pairing_sending else R.string.pairing_send_button),
                onClick = onSend,
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Invitations other people sent me ("Partner Requests" in the web Settings). */
@Composable
private fun IncomingSection(
    requests: List<IncomingRequest>,
    isWorking: Boolean,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        SectionHeader(stringResource(R.string.pairing_requests_title))
        if (requests.isEmpty()) {
            Text(
                text = stringResource(R.string.pairing_requests_empty),
                style = MaterialTheme.typography.bodySmall,
                color = TwinTheme.colors.inkFaint,
            )
        } else {
            requests.forEach { request ->
                RequestCard(request, isWorking, onAccept, onDecline)
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: IncomingRequest,
    isWorking: Boolean,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(
                text = stringResource(R.string.pairing_request_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(name = request.senderName, size = RequestAvatarSize, imageUrl = request.senderAvatar)
                Column(modifier = Modifier.weight(1f)) {
                    Text("👤 ${request.senderName}", style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    if (request.senderPhone.isNotBlank()) {
                        Text("📱 ${request.senderPhone}", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    }
                    sentAgoText(request.createdAt)?.let {
                        Text("🕒 $it", style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
                    }
                }
            }
            Text(
                text = stringResource(R.string.pairing_wants_to_connect, request.senderName),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(
                    text = stringResource(R.string.pairing_accept),
                    onClick = { onAccept(request.id) },
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.pairing_decline),
                    onClick = { onDecline(request.id) },
                    style = GlassButtonStyle.Secondary,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** I sent an invitation and am waiting ("Waiting for Partner ❤️" + "Cancel Request"). */
@Composable
private fun WaitingCard(
    status: PairingStatus.Waiting,
    isWorking: Boolean,
    onCancel: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.xl) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.pairing_waiting_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.pairing_waiting_body, status.receiverName),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
            if (status.receiverPhone.isNotBlank()) {
                Text("📱 ${status.receiverPhone}", style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
            sentAgoText(status.createdAt)?.let {
                Text("🕒 $it", style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
            GlassButton(
                text = stringResource(R.string.pairing_cancel),
                onClick = { onCancel(status.requestId) },
                style = GlassButtonStyle.Secondary,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.pairing_skip),
                onClick = onContinue,
                style = GlassButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConnectedCard(partnerName: String, onContinue: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.xl) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            CoupleAvatars(firstName = "❤", secondName = partnerName, size = ConnectedAvatarSize)
            Text(
                text = stringResource(R.string.pairing_connected_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.pairing_connected_body, partnerName),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
            GlassButton(
                text = stringResource(R.string.pairing_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "Sent 5 mins ago" style text, as in the web popup. Null when the date is missing. */
@Composable
private fun sentAgoText(createdAt: String?): String? {
    val minutes = elapsedMinutes(createdAt, System.currentTimeMillis()) ?: return null
    val count = minutes.toInt()
    return when {
        minutes < 1 -> stringResource(R.string.pairing_sent_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.pairing_sent_minutes, count, count)
        minutes < 60 * 24 -> (count / 60).let { pluralStringResource(R.plurals.pairing_sent_hours, it, it) }
        else -> (count / (60 * 24)).let { pluralStringResource(R.plurals.pairing_sent_days, it, it) }
    }
}

private val previewRequest = IncomingRequest("r1", "Asha", "9876543210", null, "2026-10-25T10:00:00+00:00")

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun PairingNotConnectedPreview() {
    TwinTheme {
        TwinBackground {
            PairingContent(
                PairingUiState(status = UiState.Content(PairingStatus.NotConnected), incoming = listOf(previewRequest)),
                {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun PairingWaitingPreview() {
    TwinTheme {
        TwinBackground {
            PairingContent(
                PairingUiState(status = UiState.Content(PairingStatus.Waiting("r2", "Ravi", "9876543210", null))),
                {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360, fontScale = 1.3f)
@Composable
private fun PairingConnectedLargeFontPreview() {
    TwinTheme {
        TwinBackground {
            PairingContent(
                PairingUiState(status = UiState.Content(PairingStatus.Connected("Ravi"))),
                {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun PairingErrorPreview() {
    TwinTheme {
        TwinBackground {
            PairingContent(PairingUiState(status = UiState.Error(AppError.Network)), {}, {}, {}, {}, {}, {}, {})
        }
    }
}
