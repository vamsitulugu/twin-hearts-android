package com.twinhearts.app.feature.bucket.data

import com.twinhearts.app.feature.bucket.domain.canSeeDream
import com.twinhearts.app.feature.bucket.domain.model.BucketCategory
import com.twinhearts.app.feature.bucket.domain.model.BucketDream
import com.twinhearts.app.feature.bucket.domain.model.BucketVisibility
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate

/** Text of a JSON value, or "" when it is missing or not a plain value. */
internal fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

/**
 * Raw `bucket` items to dreams the person may see. The raw items are kept elsewhere untouched,
 * so fields written by other screens (Dream Goals) are never lost. One bad item never hides the rest.
 */
internal fun JsonArray.toDreams(role: String): List<BucketDream> = mapNotNull { item ->
    val obj = item as? JsonObject ?: return@mapNotNull null
    val title = obj["title"].text().trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
    val visible = canSeeDream(
        visibility = obj["visibility"].text(),
        by = obj["by"].text(),
        me = (obj["me"] as? JsonPrimitive)?.booleanOrNull == true,
        role = role,
    )
    if (!visible) return@mapNotNull null
    // Dream Goals stores the target as `target`; the web Bucket List stores it as `date`.
    val dateText = obj["date"].text().ifBlank { obj["target"].text() }
    BucketDream(
        id = obj["id"].text().ifBlank { title },
        title = title,
        done = (obj["done"] as? JsonPrimitive)?.booleanOrNull == true,
        date = runCatching { LocalDate.parse(dateText) }.getOrNull(),
        category = BucketCategory.fromKey(obj["cat"].text()),
        visibility = BucketVisibility.fromKey(obj["visibility"].text()),
    )
}
