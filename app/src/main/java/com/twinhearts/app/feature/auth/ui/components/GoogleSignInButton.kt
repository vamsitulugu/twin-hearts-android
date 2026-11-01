package com.twinhearts.app.feature.auth.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.feature.auth.data.google.GoogleIdTokenProvider
import com.twinhearts.app.feature.auth.data.google.GoogleTokenResult
import kotlinx.coroutines.launch

/**
 * Remembers a function that opens Google's account chooser. [onResult] gets the token (or why there is none).
 * Needs the Activity context, which is what [LocalContext] is inside the app's single Activity.
 */
@Composable
internal fun rememberGoogleSignIn(onResult: (GoogleTokenResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { scope.launch { onResult(GoogleIdTokenProvider.requestIdToken(context)) } }
}

/** "Continue with Google". Draws nothing when GOOGLE_WEB_CLIENT_ID is not configured ([visible] false). */
@Composable
internal fun GoogleSignInButton(
    onClick: () -> Unit,
    visible: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    GlassButton(
        text = stringResource(R.string.auth_continue_google),
        onClick = onClick,
        style = GlassButtonStyle.Ghost,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}
