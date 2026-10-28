package com.twinhearts.app.core.reminders

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the shared `reminders` array into phone alarms. Calendar and Study call [sync] after
 * they read or save, with the array they already hold, so no extra request is needed.
 */
@Singleton
class RemindersSync @Inject constructor(
    private val scheduler: ReminderScheduler,
) {
    fun sync(reminders: JsonElement?, myRole: String) {
        val items = (reminders as? JsonArray).orEmpty().mapNotNull { it.toItem() }
        runCatching { scheduler.sync(ReminderRules.upcoming(items, myRole, System.currentTimeMillis())) }
    }

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

    private fun JsonElement.toItem(): ReminderItem? {
        val o = this as? JsonObject ?: return null
        return ReminderItem(
            id = o["id"].text(),
            title = o["title"].text(),
            note = o["note"].text(),
            date = o["date"].text(),
            time = o["time"].text(),
            forWhom = o["forWhom"].text(),
            fired = o["fired"].text() == "true",
            by = o["by"].text(),
        )
    }

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
}
