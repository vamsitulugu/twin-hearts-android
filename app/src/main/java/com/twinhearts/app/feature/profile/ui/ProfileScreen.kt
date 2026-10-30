package com.twinhearts.app.feature.profile.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.profile.ui.screens.AddDateSheet
import com.twinhearts.app.feature.profile.ui.screens.AddMilestoneSheet
import com.twinhearts.app.feature.profile.ui.screens.ConfirmDialog
import com.twinhearts.app.feature.profile.ui.screens.EditPartnerSheet
import com.twinhearts.app.feature.profile.ui.screens.EditProfileSheet
import com.twinhearts.app.feature.profile.ui.screens.LovePage
import com.twinhearts.app.feature.profile.ui.screens.MainPage
import com.twinhearts.app.feature.profile.ui.screens.MilestonesPage
import com.twinhearts.app.feature.profile.ui.screens.PartnerPage
import com.twinhearts.app.feature.profile.ui.screens.PhotoViewer
import com.twinhearts.app.feature.profile.ui.screens.SettingsPage
import com.twinhearts.app.feature.profile.ui.screens.StoryPage

/** Which editor or question is open on top of the page. Saved, so rotation does not close it. */
private enum class Overlay { None, EditProfile, EditPartner, AddDate, AddMilestone, RemoveFirst, RemoveSecond, ViewPhoto }

/** Where the page can send the person. Real screens are opened by the graph through Routes (R-83). */
data class ProfileNavigation(
    val onBack: () -> Unit,
    val onOpenCalendar: () -> Unit,
    val onOpenMemories: () -> Unit,
    val onOpenGoals: () -> Unit,
    val onOpenChat: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onSignedOut: () -> Unit,
)

/**
 * The "Our Profile" page. It keeps its own back stack of six sub-screens (Main, Partner, Settings,
 * Milestones, Love languages, Our Story): Back goes to the screen before, and from Main to the previous page.
 */
@Composable
fun ProfileScreen(
    navigation: ProfileNavigation,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var overlay by rememberSaveable { mutableStateOf(Overlay.None) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onAvatarPicked(uri.toString())
    }
    val exportSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri != null && json != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
            }.getOrDefault(false)
            Toast.makeText(context, context.getString(if (saved) R.string.profile_msg_export_saved else R.string.profile_msg_could_not_save), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ProfileEvent.Message -> {
                    val text = if (event.arg == null) context.getString(event.res) else context.getString(event.res, event.arg)
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    // A finished save closes its editor.
                    if (overlay in setOf(Overlay.EditProfile, Overlay.EditPartner, Overlay.AddDate, Overlay.AddMilestone) && event.res in SAVED_MESSAGES) overlay = Overlay.None
                }
                is ProfileEvent.ExportReady -> {
                    pendingExport = event.json
                    exportSaver.launch(context.getString(R.string.profile_export_file_name, java.time.LocalDate.now().toString()))
                }
                ProfileEvent.SignedOut -> navigation.onSignedOut()
            }
        }
    }

    BackHandler(enabled = state.stack.size > 1) { vm.back() }

    val screen = state.screen
    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(
                title = stringResource(
                    when (screen) {
                        ProfileScreenKind.Main -> R.string.profile_title
                        ProfileScreenKind.Partner -> R.string.profile_partner_details
                        ProfileScreenKind.Settings -> R.string.profile_settings_title
                        ProfileScreenKind.Milestones -> R.string.profile_stat_milestones
                        ProfileScreenKind.Love -> R.string.profile_love_languages
                        ProfileScreenKind.Story -> R.string.profile_our_story
                    },
                ),
                onBackClick = if (state.stack.size > 1) ({ if (!vm.back()) navigation.onBack() }) else null,
                showSidebarButton = state.stack.size <= 1,
                onSyncClick = vm::load,
            )
            Box(Modifier.weight(1f).fillMaxSize()) {
                when (val phase = state.phase) {
                    ProfilePhase.Loading -> LoadingSkeleton(Modifier.padding(TwinTheme.spacing.screenEdge), lines = 6)
                    is ProfilePhase.Failed -> ErrorState(phase.error.asText(), vm::load)
                    is ProfilePhase.Content -> {
                        val data = phase.data
                        when (screen) {
                            ProfileScreenKind.Main -> MainPage(
                                data = data,
                                todayEpochDay = state.todayEpochDay,
                                nowMs = state.nowMs,
                                uploadingAvatar = state.uploadingAvatar,
                                onMore = { vm.open(ProfileScreenKind.Settings) },
                                onPickAvatar = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onViewPartnerAvatar = { overlay = Overlay.ViewPhoto },
                                onEditProfile = { overlay = Overlay.EditProfile },
                                onPartnerDetails = { vm.open(ProfileScreenKind.Partner) },
                                onAddDate = { overlay = Overlay.AddDate },
                                onMemories = navigation.onOpenMemories,
                                onMilestones = { vm.open(ProfileScreenKind.Milestones) },
                                onDates = navigation.onOpenCalendar,
                                onDreams = navigation.onOpenGoals,
                                onStory = { vm.open(ProfileScreenKind.Story) },
                            )
                            ProfileScreenKind.Partner -> PartnerPage(
                                data = data,
                                nowMs = state.nowMs,
                                busy = state.busy,
                                onViewAvatar = { overlay = Overlay.ViewPhoto },
                                onMessage = navigation.onOpenChat,
                                onEditDetails = { overlay = Overlay.EditPartner },
                                onLoveLanguage = { vm.open(ProfileScreenKind.Love) },
                                onSendRequest = vm::sendPartnerRequest,
                                onRemove = { overlay = Overlay.RemoveFirst },
                            )
                            ProfileScreenKind.Settings -> SettingsPage(
                                onEditProfile = { overlay = Overlay.EditProfile },
                                onPartner = { vm.open(ProfileScreenKind.Partner) },
                                onSpecialDates = navigation.onOpenCalendar,
                                onMilestones = { vm.open(ProfileScreenKind.Milestones) },
                                onMemories = navigation.onOpenMemories,
                                onStory = { vm.open(ProfileScreenKind.Story) },
                                onLove = { vm.open(ProfileScreenKind.Love) },
                                onGoals = navigation.onOpenGoals,
                                onAppSettings = navigation.onOpenAppSettings,
                                onExport = vm::exportData,
                            )
                            ProfileScreenKind.Milestones -> MilestonesPage(data, onAdd = { overlay = Overlay.AddMilestone }, onDelete = { pendingDelete = it })
                            ProfileScreenKind.Love -> LovePage(data, onPick = vm::toggleLoveLanguage)
                            ProfileScreenKind.Story -> StoryPage(data, state.busy, vm::saveStory)
                        }

                        when (overlay) {
                            Overlay.EditProfile -> EditProfileSheet(data, state.busy, { overlay = Overlay.None }, vm::saveMyProfile)
                            Overlay.EditPartner -> EditPartnerSheet(data, state.busy, { overlay = Overlay.None }, vm::savePartnerDetails)
                            Overlay.AddDate -> AddDateSheet(state.busy, { overlay = Overlay.None }, vm::addDate)
                            Overlay.AddMilestone -> AddMilestoneSheet(state.busy, { overlay = Overlay.None }, vm::addMilestone)
                            Overlay.RemoveFirst -> ConfirmDialog(
                                title = stringResource(R.string.profile_remove_partner),
                                message = stringResource(R.string.profile_remove_confirm_1, data.partnerName),
                                confirmLabel = stringResource(R.string.profile_continue),
                                onConfirm = { overlay = Overlay.RemoveSecond },
                                onDismiss = { overlay = Overlay.None },
                            )
                            Overlay.RemoveSecond -> ConfirmDialog(
                                title = stringResource(R.string.profile_remove_partner),
                                message = stringResource(R.string.profile_remove_confirm_2),
                                confirmLabel = stringResource(R.string.profile_remove),
                                onConfirm = { overlay = Overlay.None; vm.removePartner() },
                                onDismiss = { overlay = Overlay.None },
                            )
                            Overlay.ViewPhoto -> data.partnerAvatarUrl?.let { PhotoViewer(it) { overlay = Overlay.None } }
                            Overlay.None -> Unit
                        }
                        val toDelete = pendingDelete
                        if (toDelete != null) {
                            ConfirmDialog(
                                title = stringResource(R.string.profile_delete_milestone),
                                message = stringResource(R.string.profile_delete_milestone_confirm),
                                confirmLabel = stringResource(R.string.profile_delete),
                                onConfirm = { vm.deleteMilestone(toDelete); pendingDelete = null },
                                onDismiss = { pendingDelete = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val SAVED_MESSAGES = setOf(
    R.string.profile_msg_profile_saved,
    R.string.profile_msg_saved,
    R.string.profile_msg_date_added,
    R.string.profile_msg_milestone_saved,
)
