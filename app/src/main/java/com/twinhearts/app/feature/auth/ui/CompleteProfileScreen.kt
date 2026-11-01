package com.twinhearts.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.auth.domain.model.Gender
import com.twinhearts.app.feature.auth.ui.components.AuthBanner
import com.twinhearts.app.feature.auth.ui.components.AuthHelpText
import com.twinhearts.app.feature.auth.ui.components.AuthScaffold
import com.twinhearts.app.feature.auth.ui.components.SlowRequestHint

/** "Almost there": the remaining sign-up details for a new Google account. Connects the ViewModel to [CompleteProfileContent]. */
@Composable
fun CompleteProfileScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: CompleteProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == AuthEvent.SignedIn) onSignedIn()
        }
    }
    CompleteProfileContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onGenderChange = viewModel::onGenderChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPinChange = viewModel::onPinChange,
        onPinConfirmChange = viewModel::onPinConfirmChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
    )
}

@Composable
internal fun CompleteProfileContent(
    state: CompleteProfileUiState,
    onNameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = !state.isLoading
    AuthScaffold(onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_google_profile_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TwinTheme.colors.ink,
        )
        AuthHelpText(stringResource(R.string.auth_google_profile_intro, state.email))
        AuthBanner(state.message)
        TwinTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.auth_name_label),
            placeholder = stringResource(R.string.auth_name_placeholder),
            enabled = editable,
        )
        GenderPicker(selected = state.gender, onSelect = onGenderChange, enabled = editable)
        TwinTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.auth_phone_label),
            placeholder = stringResource(R.string.auth_phone_placeholder),
            keyboardType = KeyboardType.Phone,
            enabled = editable,
        )
        AuthHelpText(stringResource(R.string.auth_phone_help))
        TwinTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_signup_password_label),
            placeholder = stringResource(R.string.auth_signup_password_placeholder),
            isPassword = true,
            enabled = editable,
        )
        TwinTextField(
            value = state.pin,
            onValueChange = onPinChange,
            label = stringResource(R.string.auth_pin_label),
            placeholder = stringResource(R.string.auth_pin_placeholder),
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true,
            maxLength = AuthValidators.PIN_LENGTH,
            enabled = editable,
        )
        TwinTextField(
            value = state.pinConfirm,
            onValueChange = onPinConfirmChange,
            label = stringResource(R.string.auth_pin_confirm_label),
            placeholder = stringResource(R.string.auth_pin_confirm_placeholder),
            keyboardType = KeyboardType.NumberPassword,
            isPassword = true,
            maxLength = AuthValidators.PIN_LENGTH,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            enabled = editable,
        )
        AuthHelpText(stringResource(R.string.auth_pin_help))
        GlassButton(
            text = stringResource(if (state.isLoading) R.string.auth_creating else R.string.auth_create_account_button),
            onClick = onSubmit,
            enabled = editable,
            modifier = Modifier.fillMaxWidth(),
        )
        SlowRequestHint(isLoading = state.isLoading)
    }
}

/** "I am: 👩 Girl / 👨 Boy" (same control as Sign Up). */
@Composable
private fun GenderPicker(selected: Gender?, onSelect: (Gender) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.auth_gender_label),
            style = MaterialTheme.typography.labelMedium,
            color = TwinTheme.colors.inkMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            TwinFilterChip(
                label = stringResource(R.string.auth_gender_girl),
                selected = selected == Gender.Female,
                onClick = { if (enabled) onSelect(Gender.Female) },
                modifier = Modifier.weight(1f),
            )
            TwinFilterChip(
                label = stringResource(R.string.auth_gender_boy),
                selected = selected == Gender.Male,
                onClick = { if (enabled) onSelect(Gender.Male) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 1100, widthDp = 360)
@Composable
private fun CompleteProfilePreview() {
    TwinTheme {
        TwinBackground {
            CompleteProfileContent(
                CompleteProfileUiState(email = "you@gmail.com", name = "Vamsi"),
                {}, {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}
