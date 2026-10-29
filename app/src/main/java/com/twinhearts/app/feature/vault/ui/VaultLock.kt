package com.twinhearts.app.feature.vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.vault.domain.VaultLogic

/** The web's yellow check key. */
private val VaultGold = Color(0xFFFBBF24)

/** Web `#vaultLocked`: a big lock, the title, four dots and a 3x4 PIN pad. */
@Composable
internal fun VaultLockContent(
    state: VaultUiState,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
) {
    val colors = TwinTheme.colors
    val (title, subtitle) = when (state.stage) {
        VaultStage.Setup -> R.string.vault_setup_title to R.string.vault_setup_sub
        VaultStage.Confirm -> R.string.vault_confirm_title to R.string.vault_confirm_sub
        else -> R.string.vault_title to R.string.vault_sub
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        // The web shows a big 🔐 emoji above the title.
        Text("🔐", fontSize = 64.sp, modifier = Modifier.padding(top = TwinTheme.spacing.xl))
        Text(
            stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = SerifFamily,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
        Text(
            stringResource(subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.xl),
        )
        PinDots(filled = state.pin.length, error = state.problem != null)
        Spacer(Modifier.height(TwinTheme.spacing.xs))
        PinPad(enabled = !state.busy, onKey = onKey, onDelete = onDelete, onSubmit = onSubmit)

        // Reserve the line so the pad does not jump when an error appears.
        Text(
            text = state.problem?.let { problemText(it) }.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 20.dp)
                .padding(top = TwinTheme.spacing.s),
        )
        if (state.stage == VaultStage.Enter) {
            TextButton(onClick = onForgot) {
                Text(
                    stringResource(R.string.vault_forgot),
                    color = colors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

@Composable
internal fun problemText(problem: VaultProblem): String = when (problem) {
    VaultProblem.WrongPin -> stringResource(R.string.vault_wrong_pin)
    is VaultProblem.Rule -> stringResource(problem.textRes)
    is VaultProblem.Server -> problem.error.asText()
}

@Composable
private fun PinDots(filled: Int, error: Boolean) {
    val colors = TwinTheme.colors
    val description = stringResource(R.string.vault_pin_progress, filled, VaultLogic.PIN_LENGTH)
    Row(
        modifier = Modifier
            .padding(vertical = TwinTheme.spacing.m)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        repeat(VaultLogic.PIN_LENGTH) { index ->
            val on = index < filled
            val tint = if (error) colors.error else colors.accent
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(TwinTheme.shapes.circle)
                    .background(if (on || error) tint else Color.Transparent)
                    .border(1.5.dp, if (on || error) tint else colors.inkFaint, TwinTheme.shapes.circle),
            )
        }
    }
}

@Composable
private fun PinPad(enabled: Boolean, onKey: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                row.forEach { digit -> PinKey(enabled = enabled, description = digit.toString(), onClick = { onKey(digit) }) { PinLabel(digit.toString()) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            PinKey(enabled = enabled, description = stringResource(R.string.vault_key_delete), onClick = onDelete) {
                Icon(Icons.Rounded.Backspace, contentDescription = null, tint = TwinTheme.colors.inkMuted, modifier = Modifier.size(24.dp))
            }
            PinKey(enabled = enabled, description = "0", onClick = { onKey('0') }) { PinLabel("0") }
            PinKey(enabled = enabled, description = stringResource(R.string.vault_key_ok), onClick = onSubmit) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = VaultGold, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun PinLabel(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, color = TwinTheme.colors.ink)
}

@Composable
private fun PinKey(enabled: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 54.dp)
            .clip(TwinTheme.shapes.field)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.field)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Web "Reset Vault PIN": prove it is you with the sign-in password, then choose a new PIN. */
@Composable
internal fun VaultResetDialog(
    busy: Boolean,
    problem: VaultProblem?,
    onSubmit: (password: String, pin: String, pin2: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var pin2 by rememberSaveable { mutableStateOf("") }
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.vault_reset_title), color = colors.ink) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(stringResource(R.string.vault_reset_intro), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                TwinTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.vault_reset_password),
                    isPassword = true,
                )
                TwinTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit) },
                    label = stringResource(R.string.vault_reset_new_pin),
                    isPassword = true,
                    keyboardType = KeyboardType.NumberPassword,
                    maxLength = VaultLogic.PIN_LENGTH,
                )
                TwinTextField(
                    value = pin2,
                    onValueChange = { pin2 = it.filter(Char::isDigit) },
                    label = stringResource(R.string.vault_reset_confirm_pin),
                    isPassword = true,
                    keyboardType = KeyboardType.NumberPassword,
                    maxLength = VaultLogic.PIN_LENGTH,
                )
                if (problem != null) {
                    Text(problemText(problem), style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { onSubmit(password, pin, pin2) }) {
                Text(stringResource(R.string.vault_reset_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    )
}
