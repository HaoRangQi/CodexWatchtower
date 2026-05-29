package com.codextraffic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficSnapshot
import com.codextraffic.model.TrafficUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrafficScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

        composeRule.onAllNodesWithText("CODEX 桌宠").assertCountEquals(0)
        composeRule.onNodeWithTag("pet_bot").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("蓝屏白壳桌宠").assertIsDisplayed()
        composeRule.onNodeWithTag("bot_summary").assertIsDisplayed()
        composeRule.onNodeWithText("已连接").assertIsDisplayed()
        composeRule.onNodeWithText("1 个项目正在推进").assertIsDisplayed()
        composeRule.onAllNodesWithText("项目看板").assertCountEquals(0)
        composeRule.onNodeWithText("loading").assertIsDisplayed()
        composeRule.onNodeWithText("4 秒").assertIsDisplayed()
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
        composeRule.onNodeWithTag("bot_summary").assertTextEquals("未连接")
        composeRule.onNodeWithText("暂无项目").assertIsDisplayed()
    }
}
