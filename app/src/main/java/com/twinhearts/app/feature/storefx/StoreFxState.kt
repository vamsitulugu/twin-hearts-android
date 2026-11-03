package com.twinhearts.app.feature.storefx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What is equipped in the Twin Store right now. Chat, Music, avatars, Home and Games READ this;
 * the Rewards screen WRITES it (summary refresh and every equip). It is Compose snapshot state, so
 * every screen that reads it recolours the moment something is equipped, with no extra wiring.
 *
 * Empty = nothing equipped = every screen looks exactly as before.
 */
object StoreFxState {
    private var equippedMap by mutableStateOf<Map<String, String>>(emptyMap())
    private var partnerFrameId by mutableStateOf("")

    /** Called with the server's truth. An empty map really means "nothing equipped". */
    fun apply(equipped: Map<String, String>, partnerFrame: String = partnerFrameId) {
        equippedMap = equipped.filterValues { it.isNotBlank() }
        partnerFrameId = partnerFrame
    }

    /** Used right after the user equips or unequips one item (blank [itemId] = unequip). */
    fun applyOne(category: String, itemId: String) {
        equippedMap = if (itemId.isBlank()) equippedMap - category else equippedMap + (category to itemId)
    }

    val theme: StoreFxTheme? get() = StoreFxCatalog.theme(equippedMap["theme"])
    val myFrame: StoreFxCatalog.Frame? get() = StoreFxCatalog.frame(equippedMap["frame"])
    val partnerFrame: StoreFxCatalog.Frame? get() = StoreFxCatalog.frame(partnerFrameId)
    val decor: StoreFxCatalog.Decor? get() = StoreFxCatalog.decor(equippedMap["decor"])
    val pack: StoreFxCatalog.Pack? get() = StoreFxCatalog.pack(equippedMap["game"])
}
