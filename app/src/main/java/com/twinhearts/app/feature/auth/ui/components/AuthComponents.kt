package com.twinhearts.app.feature.auth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.auth.ui.AuthMessage
import com.twinhearts.app.feature.auth.ui.asText
import kotlinx.coroutines.delay

private val BrandLogoSize = 64.dp
private val BrandLogoCorner = 18.dp
private val CardMaxWidth = 460.dp
private const val SLOW_HINT_AFTER_MS = 6000L

/**
 * Shared page for Sign In, Sign Up and Forgot Password: back button, logo + name,
 * then a glass card holding the form. Scrolls when the keyboard is open.
 */
@Composable
internal fun AuthScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TwinTopBar(onBackClick = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge)
                .padding(bottom = TwinTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xl),
        ) {
            AuthBrandHeader()
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = CardMaxWidth),
                contentPadding = TwinTheme.spacing.xl,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), content = content)
            }
        }
    }
}

@Composable
private fun AuthBrandHeader() {
    val colors = TwinTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.core_logo),
            contentDescription = stringResource(R.string.core_logo),
            modifier = Modifier
                .size(BrandLogoSize),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.ink,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
        Text(
            text = stringResource(R.string.auth_logo_tagline).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkSoft,
        )
    }
}

/** "Sign In | Sign Up" switch at the top of the card. Tapping the tab you are on does nothing. */
@Composable
internal fun AuthTabs(
    selectedIndex: Int,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    SegmentedTabs(
        options = listOf(stringResource(R.string.auth_tab_sign_in), stringResource(R.string.auth_tab_sign_up)),
        selectedIndex = selectedIndex,
        onSelect = { index ->
            if (index != selectedIndex) {
                if (index == 0) onSignIn() else onSignUp()
            }
        },
    )
}

/** Red box for problems, green box for success. Screen readers announce it when it appears. */
@Composable
internal fun AuthBanner(message: AuthMessage?, modifier: Modifier = Modifier) {
    if (message == null) return
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    val tint = if (message is AuthMessage.Success) colors.success else colors.error
    Text(
        text = message.asText(),
        style = MaterialTheme.typography.bodySmall,
        color = tint,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), shape)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Small centred divider text, e.g. "— or —". */
@Composable
internal fun AuthDividerText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TwinTheme.colors.inkFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Small grey help text under a field. */
@Composable
internal fun AuthHelpText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkFaint)
}

/** A tappable text link, at least 48 dp tall so it is easy to hit. */
@Composable
internal fun AuthTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TwinTheme.colors.inkMuted,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = TwinTheme.spacing.m),
    )
}

/** "By continuing, you agree to our Privacy Policy." The last two words open the policy in the browser. */
@Composable
internal fun AuthTermsFooter() {
    val colors = TwinTheme.colors
    val prefix = stringResource(R.string.auth_terms_prefix)
    val policy = stringResource(R.string.auth_privacy_policy)
    val suffix = stringResource(R.string.auth_terms_suffix)
    val url = stringResource(R.string.auth_privacy_url)
    val text = buildAnnotatedString {
        append(prefix)
        withLink(
            LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(SpanStyle(color = colors.pinkSoft, textDecoration = TextDecoration.Underline)),
            ),
        ) { append(policy) }
        append(suffix)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.inkFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The free Render server sleeps, so the first request can take up to a minute (R-24).
 * After a few seconds of waiting this explains why, instead of leaving a silent spinner.
 */
@Composable
internal fun SlowRequestHint(isLoading: Boolean) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        slow = false
        if (isLoading) {
            delay(SLOW_HINT_AFTER_MS)
            slow = true
        }
    }
    if (slow) {
        Text(
            text = stringResource(R.string.auth_slow_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TwinTheme.colors.pinkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
