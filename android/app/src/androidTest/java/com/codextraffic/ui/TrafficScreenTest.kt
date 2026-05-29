package com.codextraffic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficSnapshot
import com.codextraffic.model.TrafficUiState
import org.junit.Rule
import org.junit.Test

class TrafficScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsPetAndProjectRows() {
        composeRule.setContent {
            TrafficTheme {
                TrafficScreen(
                    TrafficUiState(
                        connectionStatus = ConnectionStatus.Connected,
                        snapshot = TrafficSnapshot(
                            version = 1,
                            timestampSeconds = 10,
                            overall = TrafficLight.Green,
                            projects = listOf(
                                ProjectTraffic("a1b2c3d4", "loading", TrafficLight.Green, 4, ReasonCode.Work)
                            ),
                            omittedCount = 0,
                        ),
                    )
                )
            }
        }

        composeRule.onNodeWithText("CODEX 桌宠").assertIsDisplayed()
        composeRule.onNodeWithTag("pet_bot").assertIsDisplayed()
        composeRule.onNodeWithTag("bot_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("bot_summary").assertIsDisplayed()
        composeRule.onNodeWithText("已连接").assertIsDisplayed()
        composeRule.onNodeWithText("1 个项目正在推进").assertIsDisplayed()
        composeRule.onNodeWithText("项目看板").assertIsDisplayed()
        composeRule.onNodeWithText("LOADING").assertIsDisplayed()
        composeRule.onNodeWithText("4 秒 · 正在干活").assertIsDisplayed()
        composeRule.onNodeWithText("推进中").assertIsDisplayed()
    }

    @Test
    fun disconnectedStateDoesNotHideBusinessSnapshot() {
        composeRule.setContent {
            TrafficTheme {
                TrafficScreen(
                    TrafficUiState(
                        connectionStatus = ConnectionStatus.Disconnected,
                        snapshot = TrafficSnapshot.empty,
                    )
                )
            }
        }

        composeRule.onNodeWithTag("connection_status").assertTextEquals("未连接")
        composeRule.onNodeWithTag("pet_bot").assertIsDisplayed()
        composeRule.onNodeWithText("我暂时听不到 Mac companion 的信号。").assertIsDisplayed()
        composeRule.onNodeWithText("桌宠还没收到项目信号").assertIsDisplayed()
    }
}
