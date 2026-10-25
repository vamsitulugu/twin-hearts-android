package com.twinhearts.app.feature.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.auth.ui.components.AuthBanner
import com.twinhearts.app.feature.auth.ui.components.AuthDividerText
import com.twinhearts.app.feature.auth.ui.components.AuthScaffold
import com.twinhearts.app.feature.auth.ui.components.AuthTabs
import com.twinhearts.app.feature.auth.ui.components.AuthTermsFooter
import com.twinhearts.app.feature.auth.ui.components.AuthTextLink
import com.twinhearts.app.feature.auth.ui.components.SlowRequestHint

/** Sign In (web: `#loginView`). Connects the ViewModel to the stateless [SignInContent]. */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onOpenSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == AuthEvent.SignedIn) onSignedIn()
        }
    }
    SignInContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onForgotPassword = onForgotPassword,
        onOpenSignUp = onOpenSignUp,
        onBack = onBack,
    )
}

@Composable
internal fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onOpenSignUp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(onBack = onBack, modifier = modifier) {
        AuthTabs(selectedIndex = 0, onSignIn = {}, onSignUp = onOpenSignUp)
        Text(
            text = stringResource(R.string.auth_welcome_back),
            style = MaterialTheme.typography.headlineSmall,
            color = TwinTheme.colors.ink,
        )
        AuthBanner(state.message)
        TwinTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email_label),
            placeholder = stringResource(R.string.auth_email_placeholder),
            keyboardType = KeyboardType.Email,
            enabled = !state.isLoading,
        )
        TwinTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password_label),
            placeholder = stringResource(R.string.auth_password_placeholder),
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            enabled = !state.isLoading,
        )
        AuthTextLink(
            text = stringResource(R.string.auth_forgot_link),
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.End),
        )
        GlassButton(
            text = stringResource(if (state.isLoading) R.string.auth_signing_in else R.string.auth_sign_in_button),
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        SlowRequestHint(isLoading = state.isLoading)
        AuthDividerText(stringResource(R.string.auth_or))
        GlassButton(
            text = stringResource(R.string.auth_create_new_account),
            onClick = onOpenSignUp,
            style = GlassButtonStyle.Ghost,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        AuthTermsFooter()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun SignInPreview() {
    TwinTheme { TwinBackground { SignInContent(SignInUiState(), {}, {}, {}, {}, {}, {}) } }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun SignInErrorPreview() {
    TwinTheme {
        TwinBackground {
            SignInContent(
                SignInUiState(email = "you@mail.com", message = AuthMessage.Failure(AppError.Unauthorized("Incorrect password."))),
                {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360, fontScale = 1.3f)
@Composable
private fun SignInLoadingLargeFontPreview() {
    TwinTheme { TwinBackground { SignInContent(SignInUiState(email = "you@mail.com", isLoading = true), {}, {}, {}, {}, {}, {}) } }
}
