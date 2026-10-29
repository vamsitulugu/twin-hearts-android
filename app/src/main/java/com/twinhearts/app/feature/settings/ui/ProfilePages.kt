package com.twinhearts.app.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.settings.domain.SentAgo
import com.twinhearts.app.feature.settings.domain.model.IncomingRequest
import com.twinhearts.app.feature.settings.domain.model.PartnerOverview
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.sentAgo
import com.twinhearts.app.feature.settings.ui.components.SettingsConfirmDialog
import com.twinhearts.app.feature.settings.ui.components.SettingsDatePickerDialog
import com.twinhearts.app.feature.settings.ui.components.SettingsRow
import com.twinhearts.app.feature.settings.ui.components.SettingsSection
import com.twinhearts.app.feature.settings.ui.components.SettingsValueRow
import com.twinhearts.app.feature.settings.ui.components.SubPageHeader
import com.twinhearts.app.feature.settings.ui.components.asSettingsText
import java.time.Instant
import java.time.LocalDate

/** Why this file exists: the Profile and Partner Connection sub-pages (web `#setSub-profile`, `#setSub-partner`). */

private val ProfileAvatarSize = 88.dp
private val RequestAvatarSize = 44.dp

private enum class DateField { Anniversary, MyBirthday, PartnerBirthday }

@Composable
internal fun ProfilePage(
    state: SettingsUiState,
    actions: SettingsActions,
    launchers: SettingsLaunchers,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    var editing by rememberSaveable { mutableStateOf<DateField?>(null) }
    val info = (state.profile as? UiState.Content)?.data
    val form = state.form
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        SubPageHeader(stringResource(R.string.settings_profile), stringResource(R.string.settings_profile_head), actions::onBack)
        when (val profile = state.profile) {
            UiState.Loading, UiState.Empty -> LoadingSkeleton(lines = 5)
            is UiState.Error -> ErrorState(profile.error.settingsText(), actions::onRetryProfile)
            is UiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = launchers.pickPhoto),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Avatar(name = form.myName, size = ProfileAvatarSize, imageUrl = info?.myAvatarUrl)
                        if (state.uploadingAvatar) CircularProgressIndicator(modifier = Modifier.size(32.dp), color = colors.ink, strokeWidth = 3.dp)
                    }
                    Text(stringResource(R.string.settings_change_photo), style = MaterialTheme.typography.titleSmall, color = colors.accent)
                }

                SettingsSection(title = stringResource(R.string.settings_names)) {
                    Column(
                        modifier = Modifier.padding(TwinTheme.spacing.l),
                        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                    ) {
                        TwinTextField(
                            value = form.myName,
                            onValueChange = actions::onMyName,
                            label = stringResource(R.string.settings_your_name),
                            placeholder = stringResource(R.string.settings_your_name),
                            maxLength = NAME_MAX,
                        )
                        TwinTextField(
                            value = form.partnerName,
                            onValueChange = actions::onPartnerName,
                            label = stringResource(R.string.settings_partner_name),
                            placeholder = stringResource(R.string.settings_partner_name_hint),
                            maxLength = NAME_MAX,
                            imeAction = ImeAction.Done,
                        )
                        Column {
                            Text(
                                stringResource(R.string.settings_i_am),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.inkMuted,
                                modifier = Modifier.padding(bottom = TwinTheme.spacing.xs),
                            )
                            SegmentedTabs(
                                options = listOf(stringResource(R.string.settings_girl), stringResource(R.string.settings_boy)),
                                selectedIndex = form.gender?.let { if (it == ProfileGender.Female) 0 else 1 } ?: -1,
                                onSelect = { index -> actions.onGender(if (index == 0) ProfileGender.Female else ProfileGender.Male) },
                            )
                        }
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_important_dates)) {
                    SettingsValueRow(stringResource(R.string.settings_together_since), form.anniversary.asText(), onClick = { editing = DateField.Anniversary })
                    SettingsValueRow(stringResource(R.string.settings_your_birthday), form.myBirthday.asText(), onClick = { editing = DateField.MyBirthday })
                    SettingsValueRow(stringResource(R.string.settings_partner_birthday), form.partnerBirthday.asText(), onClick = { editing = DateField.PartnerBirthday })
                }

                GlassButton(
                    text = stringResource(R.string.settings_save_changes),
                    onClick = actions::onSaveProfile,
                    loading = state.savingProfile,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    editing?.let { field ->
        val initial = when (field) {
            DateField.Anniversary -> form.anniversary
            DateField.MyBirthday -> form.myBirthday
            DateField.PartnerBirthday -> form.partnerBirthday
        }
        SettingsDatePickerDialog(
            initial = initial,
            onDismiss = { editing = null },
            onPicked = { date ->
                when (field) {
                    DateField.Anniversary -> actions.onAnniversary(date)
                    DateField.MyBirthday -> actions.onMyBirthday(date)
                    DateField.PartnerBirthday -> actions.onPartnerBirthday(date)
                }
                editing = null
            },
        )
    }
}

private const val NAME_MAX = 40

@Composable
private fun LocalDate?.asText(): String = this?.asSettingsText() ?: stringResource(R.string.settings_date_not_set)

// ───────────── Partner Connection ─────────────

private enum class PartnerDialog { RemoveFirst, RemoveSecond }

@Composable
internal fun PartnerPage(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    var dialog by rememberSaveable { mutableStateOf<PartnerDialog?>(null) }
    val partner = state.partner
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        SubPageHeader(stringResource(R.string.settings_partner), stringResource(R.string.settings_partner_head), actions::onBack)
        when (val overview = partner.overview) {
            UiState.Loading, UiState.Empty -> LoadingSkeleton(lines = 4)
            is UiState.Error -> ErrorState(overview.error.settingsText(), actions::onRetryPartner)
            is UiState.Content -> PartnerContent(overview.data, state, actions, onAskRemove = { dialog = PartnerDialog.RemoveFirst })
        }
    }

    val name = ((partner.overview as? UiState.Content)?.data?.status as? PartnerStatus.Connected)?.partnerName
        ?: state.form.partnerName.ifBlank { stringResource(R.string.settings_partner_fallback) }
    when (dialog) {
        PartnerDialog.RemoveFirst -> SettingsConfirmDialog(
            title = null,
            body = stringResource(R.string.settings_remove_confirm_1, name),
            confirmLabel = stringResource(R.string.settings_remove_partner),
            onConfirm = { dialog = PartnerDialog.RemoveSecond },
            onDismiss = { dialog = null },
        )
        PartnerDialog.RemoveSecond -> SettingsConfirmDialog(
            title = null,
            body = stringResource(R.string.settings_remove_confirm_2),
            confirmLabel = stringResource(R.string.settings_remove_partner),
            onConfirm = {
                dialog = null
                actions.onRemovePartner()
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun PartnerContent(
    overview: PartnerOverview,
    state: SettingsUiState,
    actions: SettingsActions,
    onAskRemove: () -> Unit,
) {
    val colors = TwinTheme.colors
    val partner = state.partner
    val status = overview.status
    val statusText = when (status) {
        is PartnerStatus.Connected -> stringResource(R.string.settings_status_connected) + " — " + status.partnerName
        is PartnerStatus.Waiting -> stringResource(R.string.settings_status_waiting)
        else -> stringResource(R.string.settings_status_not_connected)
    }
    SettingsSection {
        SettingsRow(
            icon = Icons.Rounded.Link,
            iconTint = colorResource(R.color.settings_ico_partner),
            title = stringResource(R.string.settings_status),
            subtitle = statusText,
        )
        if (status is PartnerStatus.Waiting) {
            WaitingBlock(status, partner.working, onCancel = { actions.onCancelRequest(status.requestId) })
        } else if (status !is PartnerStatus.Connected) {
            Column(
                modifier = Modifier.padding(TwinTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                TwinTextField(
                    value = partner.phone,
                    onValueChange = actions::onPhone,
                    label = stringResource(R.string.settings_partner_phone),
                    placeholder = stringResource(R.string.settings_phone_hint),
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    onImeAction = actions::onSendRequest,
                    error = partner.problem?.let { messageText(it) },
                )
                GlassButton(
                    text = stringResource(R.string.settings_send_request),
                    onClick = actions::onSendRequest,
                    loading = partner.working,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (status is PartnerStatus.Connected) {
            SettingsRow(
                icon = Icons.Rounded.FavoriteBorder,
                iconTint = colorResource(R.color.settings_ico_logout),
                title = stringResource(R.string.settings_remove_partner),
                subtitle = stringResource(R.string.settings_remove_partner_sub),
                danger = true,
                showChevron = false,
                onClick = onAskRemove,
            )
        }
    }

    // The web lists invitations only while there is no partner.
    if (status !is PartnerStatus.Connected) {
        SettingsSection(title = stringResource(R.string.settings_requests_received)) {
            if (overview.incoming.isEmpty()) {
                Text(
                    stringResource(R.string.settings_requests_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(TwinTheme.spacing.l),
                )
            } else {
                overview.incoming.forEach { request ->
                    RequestRow(request, partner.working, onAccept = { actions.onAccept(request.id) }, onDecline = { actions.onDecline(request.id) })
                }
            }
        }
    }
}

@Composable
private fun WaitingBlock(status: PartnerStatus.Waiting, working: Boolean, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    Column(
        modifier = Modifier.padding(TwinTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text(stringResource(R.string.settings_waiting_title), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        Text(stringResource(R.string.settings_request_sent_to), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Avatar(name = status.receiverName, size = RequestAvatarSize)
            Column {
                Text(status.receiverName, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                if (status.receiverPhone.isNotBlank()) {
                    Text(status.receiverPhone, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
                Text(sentAgoText(status.sentAt), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
        }
        GlassButton(
            text = stringResource(R.string.settings_cancel_request),
            onClick = onCancel,
            style = GlassButtonStyle.Secondary,
            enabled = !working,
        )
    }
}

@Composable
private fun RequestRow(request: IncomingRequest, working: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth().padding(TwinTheme.spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Avatar(name = request.senderName, size = RequestAvatarSize, imageUrl = request.senderAvatarUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(request.senderName, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
                if (request.senderPhone.isNotBlank()) {
                    Text(request.senderPhone, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
                Text(sentAgoText(request.sentAt), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            GlassButton(stringResource(R.string.settings_accept), onAccept, modifier = Modifier.weight(1f), enabled = !working)
            GlassButton(stringResource(R.string.settings_decline), onDecline, modifier = Modifier.weight(1f), style = GlassButtonStyle.Secondary, enabled = !working)
        }
    }
}

@Composable
private fun sentAgoText(sentAt: Instant?): String = when (val ago = sentAgo(sentAt, Instant.now())) {
    SentAgo.Unknown -> ""
    SentAgo.JustNow -> stringResource(R.string.settings_sent_just_now)
    is SentAgo.Minutes -> pluralStringResource(R.plurals.settings_sent_minutes, ago.count, ago.count)
    is SentAgo.Hours -> pluralStringResource(R.plurals.settings_sent_hours, ago.count, ago.count)
    is SentAgo.Days -> pluralStringResource(R.plurals.settings_sent_days, ago.count, ago.count)
}

/** Resolves a [SettingsMessage] for a Composable (a line under a field). */
@Composable
internal fun messageText(message: SettingsMessage): String = when (message) {
    is SettingsMessage.Res -> stringResource(message.id, *message.args.toTypedArray())
    is SettingsMessage.Failure -> message.error.settingsText()
}