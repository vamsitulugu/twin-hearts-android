package com.twinhearts.app.feature.home.domain.model

/**
 * Why this file exists: the Stay Connected card has four pictures (idle, hug, touch, miss).
 * Each one can be replaced by the couple's own photo. The [key] is the name the web app saves
 * under `dashImgs` in the shared state, so web and app read the same photos.
 */
enum class CardPhotoSlot(val key: String) {
    Idle("idle"),
    Hug("hug"),
    Touch("touch"),
    MissYou("miss"),
}

/** A photo the couple uploaded. [path] is what the server needs to delete the old file later. */
data class CardPhoto(val url: String, val path: String)

/** The picture the card shows for the newest signal (nothing sent yet = idle). */
fun LoveKind?.toCardSlot(): CardPhotoSlot = when (this) {
    null -> CardPhotoSlot.Idle
    LoveKind.Hug -> CardPhotoSlot.Hug
    LoveKind.Touch -> CardPhotoSlot.Touch
    LoveKind.MissYou -> CardPhotoSlot.MissYou
}
