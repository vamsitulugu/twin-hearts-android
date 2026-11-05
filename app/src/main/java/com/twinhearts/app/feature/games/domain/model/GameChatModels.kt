package com.twinhearts.app.feature.games.domain.model

/**
 * Why this file exists: the plain shapes of the in-game chat (web: `GameChat` in game-comms.js).
 * No Android, no network types.
 */
enum class GameChatKind { Text, Voice }

enum class GameChatStatus { Sending, Sent, Failed }

/**
 * One message in the little chat sheet. [url] is where a voice message lives once uploaded;
 * [localPath] is the recorded file on this phone, kept so a failed upload can be retried.
 */
data class GameChatMessage(
    val id: String,
    val kind: GameChatKind,
    val text: String = "",
    val url: String = "",
    val seconds: Int = 0,
    val sentAt: Long,
    val mine: Boolean,
    val status: GameChatStatus = GameChatStatus.Sent,
    val localPath: String? = null,
)

/** Everything read from the server in one go. [mine] is only filled on the first read. */
data class GameChatPull(val mine: List<GameChatMessage>, val theirs: List<GameChatMessage>)

/** Who I am in the chat: which side keeps which key, and the names for the title. */
data class GameChatIdentity(
    val side: Side,
    val coupleId: String,
    val role: String,
    val myName: String,
    val partnerName: String,
)
