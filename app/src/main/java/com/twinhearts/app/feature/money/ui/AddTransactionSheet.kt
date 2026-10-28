package com.twinhearts.app.feature.money.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.money.domain.moneyCategories
import com.twinhearts.app.feature.money.domain.model.NewTransaction
import com.twinhearts.app.feature.money.domain.model.TxType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Why this file exists: the "Add Transaction" form of the web Money page
 * (type, description, amount, date, category). Saving writes the same item the web writes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    saving: Boolean,
    onSave: (NewTransaction) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var type by rememberSaveable { mutableStateOf(TxType.Expense) }
    var description by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val date = LocalDate.parse(dateText)
    val amount = amountText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.money_form_title),
                color = colors.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinFilterChip(stringResource(R.string.money_type_expense), type == TxType.Expense, { type = TxType.Expense })
                TwinFilterChip(stringResource(R.string.money_type_income), type == TxType.Income, { type = TxType.Income })
            }
            TwinTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.money_form_desc),
                placeholder = stringResource(R.string.money_form_desc_hint),
                maxLength = MAX_DESC,
            )
            TwinTextField(
                value = amountText,
                onValueChange = { amountText = cleanAmount(it) },
                label = stringResource(R.string.money_form_amount),
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
                maxLength = MAX_AMOUNT_CHARS,
            )
            DateField(
                label = stringResource(R.string.money_form_date),
                value = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                onClick = { showDatePicker = true },
            )
            TwinTextField(
                value = category,
                onValueChange = { category = it },
                label = stringResource(R.string.money_form_category),
                placeholder = stringResource(R.string.money_form_category_hint),
                imeAction = ImeAction.Done,
                maxLength = MAX_CATEGORY,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                moneyCategories.forEach { name ->
                    TwinFilterChip(name, category == name, { category = name })
                }
            }
            GlassButton(
                text = stringResource(R.string.money_form_save),
                onClick = {
                    onSave(
                        NewTransaction(
                            type = type,
                            description = description.trim(),
                            amount = amount,
                            category = category.trim().ifBlank { "Other" },
                            date = date,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.s),
                enabled = description.isNotBlank() && amount > 0.0,
                loading = saving,
            )
        }
    }

    if (showDatePicker) {
        DatePickDialog(
            initial = date,
            onPick = {
                dateText = it.toString()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** Keeps digits and at most one dot, with at most two decimals (rupees and paise). */
private fun cleanAmount(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    if (dot < 0) return filtered
    val whole = filtered.substring(0, dot)
    val decimals = filtered.substring(dot + 1).filter { it.isDigit() }.take(2)
    return "$whole.$decimals"
}

@Composable
private fun DateField(label: String, value: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Column {
        Text(label, color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .padding(top = TwinTheme.spacing.xs)
                .fillMaxWidth()
                .heightIn(min = TwinTheme.shapes.fieldHeight)
                .clip(shape)
                .border(1.dp, colors.line, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, color = colors.ink, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The date picker works in UTC midnight milliseconds.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } else {
                        onDismiss()
                    }
                },
            ) { Text(stringResource(R.string.money_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

private const val MAX_DESC = 120
private const val MAX_CATEGORY = 40
private const val MAX_AMOUNT_CHARS = 12
