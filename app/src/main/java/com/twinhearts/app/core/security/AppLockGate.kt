package com.twinhearts.app.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Why this file exists: the "App lock" switch in Settings -> Privacy. When it is on, the app asks for the phone's
 * fingerprint / face / PIN every time it is opened, or opened again after being away for more than [GRACE_MS].
 * If the phone has no screen lock at all there is nothing to ask for, so the app opens normally.
 */
object AppLockGate {
    /** Coming back to the app within this time (camera, a picker, a call screen...) does not lock it again. */
    const val GRACE_MS = 30_000L

    private const val ALLOWED = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun canAuthenticate(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt. [onResult] gets true once the person is verified. */
    fun prompt(activity: FragmentActivity, title: String, onResult: (Boolean) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(ALLOWED)
            .build()
        prompt.authenticate(info)
    }
}
