package com.twinhearts.app.feature.vault.domain.model

/** What kind of file a vault item is (`type` on the web: "photo" or "video"). */
enum class VaultKind(val key: String) {
    Photo("photo"),
    Video("video");

    companion object {
        fun fromKey(key: String?): VaultKind = if (key == Video.key) Video else Photo
    }
}

/**
 * One secret photo or video. [url] is a long-lived signed link, [path] is where the file lives in
 * the private `vault-media` storage (needed to delete it). Same fields the web saves in `vault`.
 */
data class VaultItem(
    val id: String,
    val url: String,
    val path: String,
    val kind: VaultKind,
    val date: String,
)

/** The answer of one "add photos/videos" request. */
data class VaultAddResult(
    val items: List<VaultItem>,
    val added: Int,
    val failed: Int,
)
