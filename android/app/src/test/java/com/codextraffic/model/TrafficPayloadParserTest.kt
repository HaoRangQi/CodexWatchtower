package com.codextraffic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficPayloadParserTest {
    private val parser = TrafficPayloadParser()

    @Test
    fun parsesCompactPayload() {
        val snapshot = parser.parse(
            """{"v":1,"t":1780039000,"o":"g","p":[["a1b2c3d4","loading","g",4,"work"]],"m":0}"""
        )

        assertEquals(1, snapshot.version)
        assertEquals(1780039000L, snapshot.timestampSeconds)
        assertEquals(TrafficLight.Green, snapshot.overall)
        assertEquals(0, snapshot.omittedCount)
        assertEquals(
            ProjectTraffic(
                id = "a1b2c3d4",
                name = "loading",
                light = TrafficLight.Green,
                ageSeconds = 4,
                reason = ReasonCode.Work,
            ),
            snapshot.projects.single(),
        )
    }

    @Test
    fun treatsUnknownLightAndReasonAsSafeIdleDefaults() {
        val snapshot = parser.parse(
            """{"v":1,"t":1,"o":"x","p":[["id","name","x",8,"unknown"]],"m":2}"""
        )

        assertEquals(TrafficLight.Red, snapshot.overall)
        assertEquals(TrafficLight.Red, snapshot.projects.single().light)
        assertEquals(ReasonCode.Idle, snapshot.projects.single().reason)
        assertEquals(2, snapshot.omittedCount)
    }

    @Test
    fun ignoresMalformedProjectRows() {
        val snapshot = parser.parse(
            """{"v":1,"t":1,"o":"y","p":[["too-short"],["ok","proj","y",30,"recent"]],"m":0}"""
        )

        assertEquals(TrafficLight.Yellow, snapshot.overall)
        assertEquals(1, snapshot.projects.size)
        assertEquals("proj", snapshot.projects.single().name)
    }

    @Test
    fun parsesPetFeedRows() {
        val snapshot = parser.parse(
            """{"v":1,"t":1780039000,"o":"g","p":[],"m":0,"f":[["a1b2c3d4","正在推进 loading","4 秒内有新动作","g",4,"work"]],"n":2}"""
        )

        assertEquals(2, snapshot.omittedFeedCount)
        assertEquals(
            PetFeedItem(
                projectId = "a1b2c3d4",
                title = "正在推进 loading",
                body = "4 秒内有新动作",
                light = TrafficLight.Green,
                ageSeconds = 4,
                reason = ReasonCode.Work,
            ),
            snapshot.feedItems.single(),
        )
    }

    @Test
    fun preservesCompanionRealtimeFeedRowsWhenProjectsWereTruncated() {
        val snapshot = parser.parse(
            """{"v":1,"t":1780076809,"o":"r","p":[["82cbb3d6","agent_jobs","r",435391,"stale"],["ed9cf634","loading","g",3,"work"],["43f4ab72","HubKit","g",8,"work"]],"m":16,"f":[["ed9cf634","正在推进 loading","3 秒内有结构化状态更新","g",3,"work"],["43f4ab72","正在推进 HubKit","8 秒内有结构化状态更新","g",8,"work"],["ea3acc9e","gho-setting 刚有动静","356 秒前更新，当前未确认持续推进","y",356,"recent"]],"n":2}"""
        )

        assertEquals(16, snapshot.omittedCount)
        assertEquals(2, snapshot.omittedFeedCount)
        assertEquals(3, snapshot.feedItems.size)
        assertEquals(
            PetFeedItem(
                projectId = "ed9cf634",
                title = "正在推进 loading",
                body = "3 秒内有结构化状态更新",
                light = TrafficLight.Green,
                ageSeconds = 3,
                reason = ReasonCode.Work,
            ),
            snapshot.feedItems.first(),
        )
    }

    @Test
    fun derivesPetFeedRowsFromLegacyProjectPayload() {
        val snapshot = parser.parse(
            """{"v":1,"t":1780039000,"o":"g","p":[["a1b2c3d4","loading","g",4,"work"]],"m":0}"""
        )

        assertEquals(
            PetFeedItem(
                projectId = "a1b2c3d4",
                title = "正在推进 loading",
                body = "4 秒内有新动作",
                light = TrafficLight.Green,
                ageSeconds = 4,
                reason = ReasonCode.Work,
            ),
            snapshot.feedItems.single(),
        )
    }
}
