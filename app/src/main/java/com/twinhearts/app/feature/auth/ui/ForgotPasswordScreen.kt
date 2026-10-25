package com.twinhearts.app.feature.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.auth.ui.components.AuthBanner
import com.twinhearts.app.feature.auth.ui.components.AuthDividerText
import com.twinhearts.app.feature.auth.ui.components.AuthHelpText
import com.twinhearts.app.feature.auth.ui.components.AuthScaffold
import com.twinhearts.app.feature.auth.ui.components.SlowRequestHint

/** Forgot Password (web: `#forgotView` then `#changePasswordView`). */
@Composable
fun ForgotPasswordScreen(
    onBackToSignIn: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == AuthEvent.PasswordUpdated) onBackToSignIn()
        }
    }
    ForgotPasswordContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onVerify = viewModel::verifyIdentity,
        onUpdatePassword = viewModel::updatePassword,
        onBackToSignIn = onBackToSignIn,
    )
}

@Composable
internal fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onVerify: () -> Unit,
    onUpdatePassword: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(onBack = onBackToSignIn, modifier = modifier) {
        val title = if (state.step == ForgotStep.VerifyIdentity) R.string.auth_forgot_title else R.string.auth_new_password_title
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            color = TwinTheme.colors.ink,
        )
        AuthBanner(state.message)
        when {
            state.isDone -> Unit
            state.step == ForgotStep.VerifyIdentity -> {
                AuthHelpText(stringResource(R.string.auth_forgot_intro))
                TwinTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.auth_email_label),
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    keyboardType = KeyboardType.Email,
                    enabled = !state.isLoading,
                )
                TwinTextField(
                    value = state.phone,
                    onValueChange = onPhoneChange,
                    label = stringResource(R.string.auth_phone_label),
                    placeholder = stringResource(R.string.auth_forgot_phone_placeholder),
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    onImeAction = onVerify,
                    enabled = !state.isLoading,
                )
                GlassButton(
                    text = stringResource(if (state.isLoading) R.string.auth_checking else R.string.auth_continue),
                    onClick = onVerify,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                SlowRequestHint(isLoading = state.isLoading)
            }
            else -> {
                TwinTextField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = stringResource(R.string.auth_new_password_label),
                    placeholder = stringResource(R.string.auth_new_password_placeholder),
                    isPassword = true,
                    enabled = !state.isLoading,
                )
                TwinTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = stringResource(R.string.auth_confirm_password_label),
                    placeholder = stringResource(R.string.auth_confirm_password_placeholder),
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = onUpdatePassword,
                    enabled = !state.isLoading,
                )
                GlassButton(
                    text = stringResource(if (state.isLoading) R.string.auth_updating else R.string.auth_update_password),
                    onClick = onUpdatePassword,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                SlowRequestHint(isLoading = state.isLoading)
            }
        }
        if (!state.isDone) {
            AuthDividerText(stringResource(R.string.auth_remembered))
            GlassButton(
                text = stringResource(R.string.auth_back_to_login),
                onClick = onBackToSignIn,
                style = GlassButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun ForgotVerifyPreview() {
    TwinTheme { TwinBackground { ForgotPasswordContent(ForgotPasswordUiState(), {}, {}, {}, {}, {}, {}, {}) } }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun ForgotNewPasswordPreview() {
    TwinTheme {
        TwinBackground {
            ForgotPasswordContent(ForgotPasswordUiState(step = ForgotStep.NewPassword), {}, {}, {}, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360, fontScale = 1.3f)
@Composable
private fun ForgotDonePreview() {
    TwinTheme {
        TwinBackground {
            ForgotPasswordContent(
                ForgotPasswordUiState(step = ForgotStep.NewPassword, isDone = true, message = AuthMessage.Success(R.string.auth_password_updated)),
                {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}
