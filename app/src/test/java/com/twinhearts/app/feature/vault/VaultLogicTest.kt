package com.twinhearts.app.feature.vault

import com.twinhearts.app.R
import com.twinhearts.app.feature.vault.domain.VaultLogic
import com.twinhearts.app.feature.vault.domain.model.VaultKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultLogicTest {

    @Test fun `a good pin is accepted`() = assertNull(VaultLogic.weakPinMessage("4827"))
    @Test fun `too short is refused`() = assertEquals(R.string.vault_pin_four_digits, VaultLogic.weakPinMessage("123"))
    @Test fun `too long is refused`() = assertEquals(R.string.vault_pin_four_digits, VaultLogic.weakPinMessage("12345"))
    @Test fun `letters are refused`() = assertEquals(R.string.vault_pin_four_digits, VaultLogic.weakPinMessage("12a4"))
    @Test fun `repeated digits are refused`() = assertEquals(R.string.vault_pin_repeated, VaultLogic.weakPinMessage("1111"))
    @Test fun `ascending sequence is refused`() = assertEquals(R.string.vault_pin_sequence, VaultLogic.weakPinMessage("2345"))
    @Test fun `descending sequence is refused`() = assertEquals(R.string.vault_pin_sequence, VaultLogic.weakPinMessage("8765"))

    @Test fun `kind keys match the web`() {
        assertEquals("photo", VaultKind.Photo.key)
        assertEquals("video", VaultKind.Video.key)
        assertEquals(VaultKind.Video, VaultKind.fromKey("video"))
        assertEquals(VaultKind.Photo, VaultKind.fromKey("anything"))
    }
}
