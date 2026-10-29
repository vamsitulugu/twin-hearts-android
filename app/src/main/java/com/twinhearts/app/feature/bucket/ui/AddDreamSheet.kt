package com.twinhearts.app.feature.bucket.ui

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
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.bucket.domain.model.BucketCategory
import com.twinhearts.app.feature.bucket.domain.model.BucketVisibility
import com.twinhearts.app.feature.bucket.domain.model.NewDream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Why this file exists: the web "Add a Dream" form (dream, category, optional target date,
 * who can see it). Saving writes the same `bucket` item the web writes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDreamSheet(
    saving: Boolean,
    onSave: (NewDream) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(BucketCategory.Travel) }
    var visibility by rememberSaveable { mutableStateOf(BucketVisibility.Both) }
    var dateText by rememberSaveable { mutableStateOf("") } // yyyy-MM-dd or empty
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val date = dateText.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

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
                text = stringResource(R.string.bucket_form_title),
                color = colors.ink,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
            )
            TwinTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.bucket_form_name),
                placeholder = stringResource(R.string.bucket_form_name_hint),
                imeAction = ImeAction.Done,
                maxLength = MAX_TITLE,
            )
            FieldLabel(stringResource(R.string.bucket_form_category))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                BucketCategory.entries.forEach { option ->
                    TwinFilterChip(
                        label = "${option.emoji} ${stringResource(option.labelRes)}",
                        selected = category == option,
                        onClick = { category = option },
                    )
                }
            }
            Column {
                FieldLabel(stringResource(R.string.bucket_form_date))
                val shape = TwinTheme.shapes.field
                Row(
                    modifier = Modifier
                        .padding(top = TwinTheme.spacing.xs)
                        .fillMaxWidth()
                        .heightIn(min = TwinTheme.shapes.fieldHeight)
                        .clip(shape)
                        .border(1.dp, colors.line, shape)
                        .clickable(role = Role.Button) { showPicker = true }
                        .padding(horizontal = TwinTheme.spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            ?: stringResource(R.string.bucket_form_date_none),
                        color = if (date == null) colors.inkFaint else colors.ink,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (date != null) {
                        Text(
                            text = stringResource(R.string.bucket_form_date_clear),
                            color = colors.accent,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .clickable(role = Role.Button) { dateText = "" }
                                .padding(vertical = TwinTheme.spacing.s),
                        )
                    }
                }
            }
            FieldLabel(stringResource(R.string.bucket_form_visibility))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                // Same two choices as the web form; "Partner Only" is shown but not offered.
                listOf(BucketVisibility.Both, BucketVisibility.Self).forEach { option ->
                    TwinFilterChip(
                        label = stringResource(option.formRes),
                        selected = visibility == option,
                        onClick = { visibility = option },
                    )
                }
            }
            GlassButton(
                text = stringResource(R.string.bucket_form_save),
                onClick = {
                    onSave(NewDream(title = title.trim(), date = date, category = category, visibility = visibility))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.s),
                enabled = title.isNotBlank(),
                loading = saving,
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            dateText = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.bucket_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.core_cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.labelMedium)
}

private const val MAX_TITLE = 120
