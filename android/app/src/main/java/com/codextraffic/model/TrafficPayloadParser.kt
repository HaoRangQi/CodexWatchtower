package com.codextraffic.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class TrafficPayloadParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(payload: String): TrafficSnapshot {
        val root = json.parseToJsonElement(payload).jsonObject
        val projects = root.array("p").mapNotNull(::parseProject)
        val feedItems = root.array("f").mapNotNull(::parseFeedItem)

        return TrafficSnapshot(
            version = root.int("v"),
            timestampSeconds = root.long("t"),
            overall = TrafficLight.fromCode(root.string("o")),
            projects = projects,
            omittedCount = root.int("m"),
            feedItems = feedItems,
            omittedFeedCount = root.int("n"),
        )
    }

    private fun parseProject(element: JsonElement): ProjectTraffic? {
        val row = element as? JsonArray ?: return null
        if (row.size < 5) return null

        return ProjectTraffic(
            id = row[0].asString(),
            name = row[1].asString(),
            light = TrafficLight.fromCode(row[2].asString()),
            ageSeconds = row[3].asLong(),
            reason = ReasonCode.fromWireValue(row[4].asString()),
        )
    }

    private fun parseFeedItem(element: JsonElement): PetFeedItem? {
        val row = element as? JsonArray ?: return null
        if (row.size < 6) return null

        return PetFeedItem(
            projectId = row[0].asString(),
            title = row[1].asString(),
            body = row[2].asString(),
            light = TrafficLight.fromCode(row[3].asString()),
            ageSeconds = row[4].asLong(),
            reason = ReasonCode.fromWireValue(row[5].asString()),
        )
    }

    private fun JsonObject.string(name: String): String = this[name]?.asString().orEmpty()

    private fun JsonObject.int(name: String): Int = this[name]?.jsonPrimitive?.intOrNull ?: 0

    private fun JsonObject.long(name: String): Long = this[name]?.jsonPrimitive?.longOrNull ?: 0L

    private fun JsonObject.array(name: String): List<JsonElement> = this[name]?.let {
        runCatching { it.jsonArray }.getOrNull()
    } ?: emptyList()

    private fun JsonElement.asString(): String = (this as? JsonPrimitive)?.content.orEmpty()

    private fun JsonElement.asLong(): Long = (this as? JsonPrimitive)?.longOrNull ?: 0L
}
