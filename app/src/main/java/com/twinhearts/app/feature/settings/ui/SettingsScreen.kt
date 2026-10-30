package com.twinhearts.app.feature.settings.ui

import com.twinhearts.app.core.design.twinPage
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.settings.ui.components.NeutralSettingsColors
import com.twinhearts.app.feature.settings.ui.components.SettingsStyle
import com.twinhearts.app.feature.settings.domain.model.SettingsPage
import com.twinhearts.app.feature.settings.ui.components.SettingsConfirmDialog
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val POLL_MS = 5000L

/**
 * Settings (web `#page-settings`). Connects the ViewModel to the pages and owns everything that
 * needs the phone: file pickers, the notification permission, the browser and the toasts.
 */
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val privacyUrl = stringResource(R.string.settings_privacy_url)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Show -> Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT).show()
                SettingsEvent.SignedOut -> onSignedOut()
            }
        }
    }

    // While the Partner page is open, look for news every 5 s (partner accepted / new request). R-66.
    LaunchedEffect(state.page, lifecycleOwner) {
        if (state.page == SettingsPage.Partner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(POLL_MS)
                    viewModel.refreshPartner()
                }
            }
        }
    }

    // ───────────── notification permission ─────────────
    var resumeCount by remember { mutableIntStateOf(0) }
    var askedAndDenied by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        // The person may change the switch in the phone's own settings and come back.
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeCount++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsOn = remember(resumeCount) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val notification = when {
        notificationsOn -> NotificationStatus.Enabled
        askedAndDenied || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> NotificationStatus.Blocked
        else -> NotificationStatus.CanAsk
    }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        resumeCount++
        if (granted) {
            Toast.makeText(context, R.string.settings_notif_enabled_toast, Toast.LENGTH_SHORT).show()
        } else {
            askedAndDenied = true
            Toast.makeText(context, R.string.settings_notif_not_enabled, Toast.LENGTH_SHORT).show()
        }
    }

    // ───────────── pickers ─────────────
    var pendingRestore by rememberSaveable { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onAvatarPicked(uri)
    }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.onExportBackup(uri)
    }
    val chooseBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.onExportPhotos(uri)
    }

    val launchers = SettingsLaunchers(
        pickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        createBackupFile = { createBackup.launch("twin-hearts-backup-${LocalDate.now()}.json") },
        chooseBackupFile = { chooseBackup.launch(arrayOf("*/*")) },
        choosePhotoFolder = { chooseFolder.launch(null) },
        toggleNotifications = {
            when (notification) {
                NotificationStatus.Enabled -> Toast.makeText(context, R.string.settings_notif_on_toast, Toast.LENGTH_SHORT).show()
                NotificationStatus.CanAsk -> showRationale = true
                NotificationStatus.Blocked -> openNotificationSettings(context)
            }
        },
        openPrivacyPolicy = { uriHandler.openUri(privacyUrl) },
        openHelp = onOpenHelp,
    )

    // ───────────── layout ─────────────
    var askLogOut by rememberSaveable { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val reduceMotion = rememberReduceMotion()

    BackHandler(enabled = state.page != null, onBack = viewModel::onBack)

    NeutralSettingsColors {
    Column(modifier = Modifier.twinPage()) {
        TwinTopBar(
            title = stringResource(R.string.settings_title),
            showSidebarButton = true,
            onSearchClick = if (state.page == null) {
                { searchFocus.requestFocus() }
            } else {
                null
            },
            onSyncClick = viewModel::onSync,
        )
        AnimatedContent(
            targetState = state.page,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(FADE_IN_MS)) togetherWith fadeOut(tween(FADE_OUT_MS))
                }
            },
            label = "settingsPage",
        ) { page ->
            when (page) {
                null -> SettingsHome(state, notification, viewModel, launchers, onAskLogOut = { askLogOut = true }, searchFocus = searchFocus)
                SettingsPage.Profile -> ProfilePage(state, viewModel, launchers)
                SettingsPage.Partner -> PartnerPage(state, viewModel)
                SettingsPage.Notifications -> NotificationsPage(state, notification, viewModel, launchers)
                SettingsPage.Appearance -> AppearancePage(state, viewModel)
                SettingsPage.ChatMedia -> ChatMediaPage(state, viewModel)
                SettingsPage.TwinAi -> TwinAiPage(state, viewModel)
                SettingsPage.Privacy -> PrivacyPage(state, viewModel)
                SettingsPage.Storage -> StoragePage(
                    state = state,
                    actions = viewModel,
                    launchers = launchers,
                    pendingRestore = pendingRestore,
                    onRestoreHandled = { pendingRestore = null },
                )
                SettingsPage.About -> AboutPage(viewModel, launchers)
            }
        }
    }

    if (askLogOut) {
        SettingsConfirmDialog(
            title = null,
            body = stringResource(R.string.settings_logout_confirm),
            confirmLabel = stringResource(R.string.settings_logout),
            onConfirm = {
                askLogOut = false
                viewModel.onLogOut()
            },
            onDismiss = { askLogOut = false },
        )
    }

    if (showRationale) {
        SettingsConfirmDialog(
            title = stringResource(R.string.settings_notif_rationale_title),
            body = stringResource(R.string.settings_notif_rationale_body),
            confirmLabel = stringResource(R.string.settings_allow),
            danger = false,
            onConfirm = {
                showRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { showRationale = false },
        )
    }
    }
}

private const val FADE_IN_MS = 180
private const val FADE_OUT_MS = 120

/** Opens this app's page in the phone's notification settings (when the permission is blocked). */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

private fun SettingsMessage.asString(context: Context): String = when (this) {
    is SettingsMessage.Res -> context.getString(id, *args.toTypedArray())
    is SettingsMessage.Failure -> error.asString(context)
}

private fun AppError.asString(context: Context): String = when (this) {
    AppError.Network -> context.getString(R.string.core_error_network)
    is AppError.Unauthorized -> message ?: context.getString(R.string.core_error_unauthorized)
    is AppError.Server -> message ?: context.getString(R.string.core_error_server)
    AppError.Unknown -> context.getString(R.string.core_error_unknown)
}
