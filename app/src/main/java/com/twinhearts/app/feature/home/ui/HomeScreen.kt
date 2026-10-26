package com.twinhearts.app.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveInteraction
import com.twinhearts.app.feature.home.domain.model.LoveKind
import com.twinhearts.app.feature.home.domain.model.MoodKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

private const val POLL_MS = 8000L
private const val MESSAGE_MS = 3500L
private const val CLOCK_TICK_MS = 30_000L

/** Home (web: `#page-dashboard`). Connects the ViewModel to the stateless [HomeContent]. */
@Composable
fun HomeScreen(
    onShortcut: (HomeShortcut) -> Unit,
    onConnectPartner: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    // While Home is visible, look for new Hug / Touch / Miss You from my partner.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refresh()
            while (true) {
                delay(POLL_MS)
                viewModel.refresh()
            }
        }
    }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_MS)
            viewModel.clearMessage()
        }
    }
    HomeContent(
        state = state,
        onShortcut = onShortcut,
        onConnectPartner = onConnectPartner,
        onSend = viewModel::send,
        onAcceptHug = viewModel::acceptHug,
        onDismissHug = viewModel::dismissHug,
        onSetMood = viewModel::setMood,
    )
}

/** Ticks every 30 s so "5m ago" and the greeting stay fresh without a refresh. */
@Composable
private fun rememberNowMillis(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(CLOCK_TICK_MS)
        value = System.currentTimeMillis()
    }
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onShortcut: (HomeShortcut) -> Unit,
    onConnectPartner: () -> Unit,
    onSend: (LoveKind) -> Unit,
    onAcceptHug: () -> Unit,
    onDismissHug: () -> Unit,
    onSetMood: (LocalDate, MoodKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMoodCalendar by rememberSaveable { mutableStateOf(false) }
    val nowMillis by rememberNowMillis()
    val session = state.session
    val myName = session?.myName.orEmpty()
    val paired = session?.paired == true
    val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val partnerName = session?.partnerName.orEmpty()
    val pendingHug = state.loveCard.pendingIncomingHugId
    val showIncomingHug = paired && pendingHug != null && pendingHug != state.dismissedHugId

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        HomeHeader(myName = myName, onProfileClick = { onShortcut(HomeShortcut.OurProfile) })
        HomeHero(myName = myName, nowMillis = nowMillis)
        HomeSearchRow(onShortcut)
        HomeChips(onShortcut)

        Column(
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.home_stay_connected),
                style = MaterialTheme.typography.headlineSmall,
                color = TwinTheme.colors.ink,
            )
            if (session != null && !paired) ConnectPartnerCard(onClick = onConnectPartner)
            StayConnectedCard(
                last = state.loveCard.last,
                nowMillis = nowMillis,
                actionsEnabled = paired && !state.isSending,
                onSend = onSend,
            )
            HomeBanner(state.message)
            if (showIncomingHug) {
                IncomingHugCard(
                    partnerName = session?.partnerName.orEmpty(),
                    enabled = !state.isSending,
                    onAccept = onAcceptHug,
                    onLater = onDismissHug,
                )
            }
        }

        HomeComingUp(events = state.events, partnerName = partnerName, onShortcut = onShortcut)
        HomeMoodCard(
            mood = state.mood,
            today = today,
            partnerName = partnerName,
            paired = paired,
            onSetMood = onSetMood,
            onOpenCalendar = { showMoodCalendar = true },
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge),
        )
        Spacer(Modifier.height(TwinTheme.spacing.s))
        HomeAllPages(onShortcut)
        HomeFooterQuote(nowMillis)
        Spacer(Modifier.height(TwinTheme.spacing.xxl))
    }

    if (showMoodCalendar) {
        MoodCalendarDialog(
            mood = state.mood,
            today = today,
            partnerName = partnerName,
            paired = paired,
            onSetMood = onSetMood,
            onClose = { showMoodCalendar = false },
        )
    }
}

private val previewSession = Session(
    userId = "u1", coupleId = "c1", myName = "Vamsi", email = "a@b.com", phoneNumber = "123",
    partnerName = "Asha", anniversary = "", role = "user1", paired = true, gender = "male",
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 1200, widthDp = 360)
@Composable
private fun HomeIdlePreview() {
    TwinTheme { TwinBackground { HomeContent(HomeUiState(session = previewSession), {}, {}, {}, {}, {}, { _, _ -> }) } }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 1200, widthDp = 360)
@Composable
private fun HomeSignalAndHugPreview() {
    val last = LoveInteraction(LoveKind.Touch, fromMe = false, timestampMillis = System.currentTimeMillis() - 300_000)
    TwinTheme {
        TwinBackground {
            HomeContent(
                HomeUiState(session = previewSession, loveCard = LoveCardState(last = last, pendingIncomingHugId = 5L)),
                {}, {}, {}, {}, {}, { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 1200, widthDp = 360, fontScale = 1.3f)
@Composable
private fun HomeUnpairedLargeFontPreview() {
    TwinTheme { TwinBackground { HomeContent(HomeUiState(session = previewSession.copy(paired = false)), {}, {}, {}, {}, {}, { _, _ -> }) } }
}
