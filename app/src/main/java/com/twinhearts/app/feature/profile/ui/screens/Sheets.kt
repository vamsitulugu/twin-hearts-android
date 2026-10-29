package com.twinhearts.app.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.domain.model.DateAlarm
import com.twinhearts.app.feature.profile.domain.model.DateCategory
import com.twinhearts.app.feature.profile.domain.model.Gender
import com.twinhearts.app.feature.profile.domain.model.MilestoneType
import com.twinhearts.app.feature.profile.domain.model.MyProfileEdit
import com.twinhearts.app.feature.profile.domain.model.NewMilestone
import com.twinhearts.app.feature.profile.domain.model.NewSpecialDate
import com.twinhearts.app.feature.profile.domain.model.PartnerEdit
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import com.twinhearts.app.feature.profile.ui.components.ProfileChoice
import com.twinhearts.app.feature.profile.ui.components.ProfileDateField
import com.twinhearts.app.feature.profile.ui.components.ProfileSheet
import com.twinhearts.app.feature.profile.ui.components.ProfileTextArea
import com.twinhearts.app.feature.profile.ui.components.ProfileTimeField
import java.time.LocalDate

/** "Edit Profile": name, birthday, together since, "I am" and bio. */
@Composable
fun EditProfileSheet(data: ProfileData, busy: Boolean, onDismiss: () -> Unit, onSave: (MyProfileEdit) -> Unit) {
    var name by rememberSaveable { mutableStateOf(data.myName) }
    var birthday by rememberSaveable { mutableStateOf(data.myBirthday?.toString()) }
    var since by rememberSaveable { mutableStateOf(data.anniversary?.toString()) }
    var gender by rememberSaveable { mutableStateOf(data.myGender?.wire) }
    var bio by rememberSaveable { mutableStateOf(data.myBio) }
    ProfileSheet(
        title = stringResource(R.string.profile_edit_profile),
        onDismiss = onDismiss,
        onSave = {
            onSave(MyProfileEdit(name, birthday?.let(LocalDate::parse), since?.let(LocalDate::parse), Gender.fromWire(gender), bio))
        },
        saveEnabled = true,
        saving = busy,
    ) {
        TwinTextField(name, { name = it }, stringResource(R.string.profile_your_name), placeholder = stringResource(R.string.profile_your_name), maxLength = 40)
        ProfileDateField(stringResource(R.string.profile_your_birthday_label), birthday?.let(LocalDate::parse), { birthday = it?.toString() }, placeholder = stringResource(R.string.profile_pick_date))
        ProfileDateField(stringResource(R.string.profile_together_since), since?.let(LocalDate::parse), { since = it?.toString() }, placeholder = stringResource(R.string.profile_pick_date))
        ProfileChoice(
            label = stringResource(R.string.profile_i_am),
            options = Gender.entries,
            selected = Gender.fromWire(gender),
            optionLabel = { stringResource(if (it == Gender.Female) R.string.profile_girl else R.string.profile_boy) },
            onSelect = { gender = it.wire },
        )
        ProfileTextArea(bio, { bio = it }, stringResource(R.string.profile_bio), placeholder = stringResource(R.string.profile_write_sweet))
    }
}

/** "Edit Details" for the partner. While connected only the notes are editable, like the web. */
@Composable
fun EditPartnerSheet(data: ProfileData, busy: Boolean, onDismiss: () -> Unit, onSave: (PartnerEdit) -> Unit) {
    val ownIdentity = !data.paired
    var name by rememberSaveable { mutableStateOf(data.partnerName.takeIf { it != "Partner" }.orEmpty()) }
    var birthday by rememberSaveable { mutableStateOf(data.partnerBirthday?.toString()) }
    var bio by rememberSaveable { mutableStateOf(data.partnerBio) }
    ProfileSheet(
        title = stringResource(R.string.profile_edit_details),
        onDismiss = onDismiss,
        onSave = { onSave(PartnerEdit(ownIdentity, name, birthday?.let(LocalDate::parse), bio)) },
        saveEnabled = true,
        saving = busy,
    ) {
        if (ownIdentity) {
            TwinTextField(name, { name = it }, stringResource(R.string.profile_partner_name), placeholder = stringResource(R.string.profile_partner_name), maxLength = 40)
            ProfileDateField(stringResource(R.string.profile_birthday), birthday?.let(LocalDate::parse), { birthday = it?.toString() }, placeholder = stringResource(R.string.profile_pick_date))
        }
        ProfileTextArea(bio, { bio = it }, stringResource(R.string.profile_about_partner, data.partnerName), placeholder = stringResource(R.string.profile_about_partner_hint))
    }
}

/** "Add Date": same fields as the web "Add Event" form. Title and date are required. */
@Composable
fun AddDateSheet(busy: Boolean, onDismiss: () -> Unit, onSave: (NewSpecialDate) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf<String?>(null) }
    var time by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(DateCategory.Anniversary.key) }
    var alarm by rememberSaveable { mutableStateOf(DateAlarm.None.key) }
    ProfileSheet(
        title = stringResource(R.string.profile_add_event),
        onDismiss = onDismiss,
        onSave = {
            val picked = date?.let(LocalDate::parse) ?: return@ProfileSheet
            onSave(
                NewSpecialDate(
                    title = title.trim(),
                    date = picked,
                    time = time,
                    note = note.trim(),
                    category = DateCategory.entries.first { it.key == category },
                    alarm = DateAlarm.entries.first { it.key == alarm },
                ),
            )
        },
        saveEnabled = title.isNotBlank() && date != null,
        saving = busy,
        saveLabel = stringResource(R.string.profile_add_event_button),
    ) {
        TwinTextField(title, { title = it }, stringResource(R.string.profile_event_name), placeholder = stringResource(R.string.profile_event_name_hint), maxLength = 80)
        ProfileDateField(stringResource(R.string.profile_date), date?.let(LocalDate::parse), { date = it?.toString() }, placeholder = stringResource(R.string.profile_pick_date), clearable = false)
        ProfileTimeField(stringResource(R.string.profile_time), time, { time = it }, placeholder = stringResource(R.string.profile_optional))
        TwinTextField(note, { note = it }, stringResource(R.string.profile_note_optional), placeholder = stringResource(R.string.profile_event_note_hint), maxLength = 120)
        ProfileChoice(
            label = stringResource(R.string.profile_category),
            options = DateCategory.entries,
            selected = DateCategory.entries.first { it.key == category },
            optionLabel = { "${it.emoji} ${stringResource(categoryLabel(it))}" },
            onSelect = { category = it.key },
        )
        ProfileChoice(
            label = stringResource(R.string.profile_set_alarm),
            options = DateAlarm.entries,
            selected = DateAlarm.entries.first { it.key == alarm },
            optionLabel = { stringResource(alarmLabel(it)) },
            onSelect = { alarm = it.key },
        )
    }
}

private fun categoryLabel(c: DateCategory): Int = when (c) {
    DateCategory.Anniversary -> R.string.profile_cat_anniversary
    DateCategory.DateNight -> R.string.profile_cat_date
    DateCategory.Birthday -> R.string.profile_cat_birthday
    DateCategory.Trip -> R.string.profile_cat_trip
    DateCategory.Special -> R.string.profile_cat_special
    DateCategory.Study -> R.string.profile_cat_study
    DateCategory.Other -> R.string.profile_cat_other
}

private fun alarmLabel(a: DateAlarm): Int = when (a) {
    DateAlarm.None -> R.string.profile_alarm_none
    DateAlarm.Self -> R.string.profile_alarm_self
    DateAlarm.Both -> R.string.profile_alarm_both
}

private fun milestoneLabel(t: MilestoneType): Int = when (t) {
    MilestoneType.FirstMessage -> R.string.profile_ms_first_message
    MilestoneType.FirstCall -> R.string.profile_ms_first_call
    MilestoneType.FirstMeet -> R.string.profile_ms_first_meet
    MilestoneType.Proposal -> R.string.profile_ms_proposal
    MilestoneType.Trip -> R.string.profile_ms_trip
    MilestoneType.Anniversary -> R.string.profile_ms_anniversary
    MilestoneType.Custom -> R.string.profile_ms_custom
}

/** "Add Milestone": title, type, date and a memory note. */
@Composable
fun AddMilestoneSheet(busy: Boolean, onDismiss: () -> Unit, onSave: (NewMilestone) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(MilestoneType.FirstMessage.name) }
    var date by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    ProfileSheet(
        title = stringResource(R.string.profile_add_milestone),
        onDismiss = onDismiss,
        onSave = {
            val picked = date?.let(LocalDate::parse) ?: return@ProfileSheet
            onSave(NewMilestone(title.trim(), picked, note.trim(), MilestoneType.valueOf(type)))
        },
        saveEnabled = title.isNotBlank() && date != null,
        saving = busy,
        saveLabel = stringResource(R.string.profile_save_memory),
    ) {
        TwinTextField(title, { title = it }, stringResource(R.string.profile_milestone_label), placeholder = stringResource(R.string.profile_milestone_hint), maxLength = 80)
        ProfileChoice(
            label = stringResource(R.string.profile_type),
            options = MilestoneType.entries,
            selected = MilestoneType.valueOf(type),
            optionLabel = { "${it.emoji} ${stringResource(milestoneLabel(it))}" },
            onSelect = { type = it.name },
        )
        ProfileDateField(stringResource(R.string.profile_date), date?.let(LocalDate::parse), { date = it?.toString() }, placeholder = stringResource(R.string.profile_pick_date), clearable = false)
        ProfileTextArea(note, { note = it }, stringResource(R.string.profile_memory_note), placeholder = stringResource(R.string.profile_memory_note_hint))
    }
}

/** Full-screen photo viewer for the partner's picture. */
@Composable
fun PhotoViewer(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.l)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.profile_close), tint = Color.White)
            }
        }
    }
}

/** A plain yes/no question. */
@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwinTheme.colors.card,
        title = { Text(title, color = TwinTheme.colors.ink) },
        text = { Text(message, color = TwinTheme.colors.inkMuted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = TwinTheme.colors.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
    )
}
