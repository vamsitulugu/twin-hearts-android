package com.twinhearts.app.feature.auth

import com.twinhearts.app.R
import com.twinhearts.app.feature.auth.ui.AuthValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The PIN and password rules must match the server's (routes/auth.js). */
class AuthValidatorsTest {
    @Test fun goodPin_isAccepted() = assertNull(AuthValidators.vaultPinProblem("2580"))
    @Test fun shortPin_isRejected() = assertEquals(R.string.auth_err_pin_digits, AuthValidators.vaultPinProblem("123"))
    @Test fun lettersInPin_areRejected() = assertEquals(R.string.auth_err_pin_digits, AuthValidators.vaultPinProblem("12a4"))
    @Test fun repeatedDigits_areRejected() = assertEquals(R.string.auth_err_pin_repeated, AuthValidators.vaultPinProblem("1111"))
    @Test fun ascendingRun_isRejected() = assertEquals(R.string.auth_err_pin_sequence, AuthValidators.vaultPinProblem("1234"))
    @Test fun descendingRun_isRejected() = assertEquals(R.string.auth_err_pin_sequence, AuthValidators.vaultPinProblem("4321"))

    @Test
    fun passwordNeedsSixCharacters() {
        assertTrue(AuthValidators.isPasswordTooShort("12345"))
        assertFalse(AuthValidators.isPasswordTooShort("123456"))
    }
}
