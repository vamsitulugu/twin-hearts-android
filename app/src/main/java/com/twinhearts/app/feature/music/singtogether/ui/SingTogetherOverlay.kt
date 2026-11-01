package com.twinhearts.app.feature.music.singtogether.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.feature.music.singtogether.domain.SingNotice
import com.twinhearts.app.feature.music.singtogether.domain.SingPhase

/**
 * Why this file exists: draws whatever Sing Together needs on top of the Music page: the invite
 * card, the "waiting" card, short messages, and the room. The Music page only adds this one call.
 * [onAccept] goes back to the page because accepting may first ask for camera and microphone.
 */
@Composable
internal fun SingTogetherOverlay(viewModel: SingTogetherViewModel, onAccept: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.notice) {
        state.notice?.let { notice ->
            Toast.makeText(context, context.getString(noticeText(notice)), Toast.LENGTH_LONG).show()
            viewModel.consumeNotice()
        }
    }

    when (state.phase) {
        SingPhase.Idle -> Unit
        SingPhase.Invited -> SingInviteDialog(
            partnerName = state.partnerName.ifBlank { stringResource(R.string.music_together_partner_fallback) },
            songTitle = state.songTitle,
            onAccept = onAccept,
            onDecline = viewModel::decline,
        )
        SingPhase.Inviting -> SingWaitingDialog(onCancel = viewModel::cancelInvite)
        SingPhase.Connecting, SingPhase.InRoom -> {
            // Back leaves the room first (the Music page's own back handler is registered earlier).
            BackHandler(onBack = viewModel::leave)
            KeepScreenOn()
            SingRoomScreen(
                state = state,
                player = player,
                actions = SingRoomActions(
                    onLeave = viewModel::leave,
                    onPrevious = viewModel::previous,
                    onTogglePlay = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onSeek = viewModel::seekTo,
                    onMic = viewModel::toggleMic,
                    onCamera = viewModel::toggleCamera,
                    onFlip = viewModel::switchCamera,
                    onSpeaker = viewModel::toggleSpeaker,
                    onReaction = viewModel::sendReaction,
                ),
                // The room covers the page: taps must not reach what is underneath.
                modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
            )
        }
    }
}

/** A singing room should not dim or lock while two people are singing. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

private fun noticeText(notice: SingNotice): Int = when (notice) {
    SingNotice.Declined -> R.string.music_together_notice_declined
    SingNotice.NoAnswer -> R.string.music_together_notice_no_answer
    SingNotice.PartnerLeft -> R.string.music_together_notice_left
    SingNotice.OnlyHostControls -> R.string.music_together_notice_host_only
    SingNotice.SongUnavailable -> R.string.music_together_notice_song_missing
    SingNotice.NoCameraOrMic -> R.string.music_together_notice_no_cam_mic
    SingNotice.SwitchCameraFailed -> R.string.music_together_notice_flip_failed
    SingNotice.CouldNotConnect -> R.string.music_together_notice_not_connected
}
