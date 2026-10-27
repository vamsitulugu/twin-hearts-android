package com.twinhearts.app.feature.twinai.data.mapper

import com.twinhearts.app.feature.twinai.data.remote.ResultDto
import com.twinhearts.app.feature.twinai.data.remote.RoundDto
import com.twinhearts.app.feature.twinai.data.remote.SessionDetailDto
import com.twinhearts.app.feature.twinai.data.remote.SessionDto
import com.twinhearts.app.feature.twinai.domain.model.BothDiscussion
import com.twinhearts.app.feature.twinai.domain.model.BothResult
import com.twinhearts.app.feature.twinai.domain.model.BothRound
import com.twinhearts.app.feature.twinai.domain.model.BothSection
import com.twinhearts.app.feature.twinai.domain.model.BothSession
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import java.time.Instant
import java.time.OffsetDateTime

/** Why this file exists: turns server JSON shapes into the plain models the screens use. */

fun ResultDto.toDomain() = BothResult(
    intent = intent.uppercase(),
    safety = safetyFlag,
    sections = sections.filter { it.title.isNotBlank() || it.content.isNotBlank() }
        .map { BothSection(it.title, it.content) },
)

fun RoundDto.toDomain() = BothRound(
    id = id,
    number = roundNumber,
    status = RoundStatus.from(status),
    youSubmitted = youSubmitted,
    partnerSubmitted = partnerSubmitted,
    yourContent = yourContent,
    result = result?.toDomain(),
)

fun SessionDto.toDomain() = BothSession(
    id = id,
    title = title?.takeIf { it.isNotBlank() },
    roundCount = roundCount,
    lastActivity = parseServerInstant(lastActivityAt),
    intent = intent?.takeIf { it.isNotBlank() }?.uppercase(),
)

fun SessionDetailDto.toDomain() = BothDiscussion(session.toDomain(), rounds.map { it.toDomain() })

/** Supabase sends `2026-10-26T09:00:00.123+00:00`; a bad value just means "no time". */
fun parseServerInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(raw).toInstant() }
        .recoverCatching { Instant.parse(raw) }
        .getOrNull()
}
