package com.codextraffic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.PetFeedItem
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
                TrafficPagerScreen(
                    uiState = TrafficUiState(
                        connectionStatus = ConnectionStatus.Connected,
                        snapshot = TrafficSnapshot(
                            version = 1,
                            timestampSeconds = 10,
                            overall = TrafficLight.Green,
                            projects = listOf(
                                ProjectTraffic("a1b2c3d4", "loading", TrafficLight.Green, 4, ReasonCode.Work),
                                ProjectTraffic("b2c3d4e5", "shimmyUI", TrafficLight.Yellow, 23, ReasonCode.Recent),
                            ),
                            omittedCount = 0,
                        ),
                    ),
                    petMotionEnabled = false,
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
        composeRule.onNodeWithText("shimmyUI").assertIsDisplayed()
        composeRule.onNodeWithText("刚动过").assertIsDisplayed()
        composeRule.onAllNodesWithText("观察").assertCountEquals(0)
    }

    @Test
    fun recentStateUsesConcreteText() {
        composeRule.setContent {
            TrafficTheme {
                TrafficPagerScreen(
                    uiState = TrafficUiState(
                        connectionStatus = ConnectionStatus.Connected,
                        snapshot = TrafficSnapshot(
                            version = 1,
                            timestampSeconds = 10,
                            overall = TrafficLight.Yellow,
                            projects = listOf(
                                ProjectTraffic("b2c3d4e5", "shimmyUI", TrafficLight.Yellow, 23, ReasonCode.Recent)
                            ),
                            omittedCount = 0,
                        ),
                    ),
                    petMotionEnabled = false,
                )
            }
        }

        composeRule.onNodeWithTag("bot_summary").assertTextEquals("刚有动静")
        composeRule.onNodeWithText("当前没新进展").assertIsDisplayed()
        composeRule.onNodeWithText("刚动过").assertIsDisplayed()
        composeRule.onAllNodesWithText("观察").assertCountEquals(0)
    }

    @Test
    fun disconnectedStateDoesNotHideBusinessSnapshot() {
        composeRule.setContent {
            TrafficTheme {
                TrafficScreen(
                    uiState = TrafficUiState(
                        connectionStatus = ConnectionStatus.Disconnected,
                        snapshot = TrafficSnapshot.empty,
                    ),
                    petMotionEnabled = false,
                )
            }
        }

        composeRule.onNodeWithTag("connection_status").assertTextEquals("未连接")
        composeRule.onNodeWithTag("pet_bot").assertIsDisplayed()
        composeRule.onNodeWithTag("bot_summary").assertTextEquals("未连接")
        composeRule.onNodeWithText("暂无项目").assertIsDisplayed()
    }

    @Test
    fun canHideAndRestoreProjectsFromPhone() {
        var hiddenProject: ProjectTraffic? = null
        var restoredProject: ProjectTraffic? = null
        val hidden = ProjectTraffic("hidden", "old-job", TrafficLight.Yellow, 23, ReasonCode.Recent)

        composeRule.setContent {
            TrafficTheme {
                TrafficScreen(
                    uiState = TrafficUiState(
                        connectionStatus = ConnectionStatus.Connected,
                        snapshot = TrafficSnapshot(
                            version = 1,
                            timestampSeconds = 10,
                            overall = TrafficLight.Green,
                            projects = listOf(
                                ProjectTraffic("active", "loading", TrafficLight.Green, 4, ReasonCode.Work)
                            ),
                            omittedCount = 0,
                        ),
                        hiddenProjects = listOf(hidden),
                    ),
                    petMotionEnabled = false,
                    onHideProject = { hiddenProject = it },
                    onRestoreProject = { restoredProject = it },
                )
            }
        }

        composeRule.onNodeWithText("隐藏").performClick()
        composeRule.runOnIdle {
            check(hiddenProject?.id == "active")
        }

        composeRule.onNodeWithText("已隐藏 1 项 · 点此恢复").performClick()
        composeRule.onNodeWithText("old-job").assertIsDisplayed()
        composeRule.onNodeWithText("恢复").performClick()
        composeRule.runOnIdle {
            check(restoredProject?.id == "hidden")
        }
    }

    @Test
    fun swipingLeftShowsCodexOverlayStylePetFeedScreen() {
        composeRule.setContent {
            TrafficTheme {
                TrafficPagerScreen(
                    uiState = TrafficUiState(
                        connectionStatus = ConnectionStatus.Connected,
                        snapshot = TrafficSnapshot(
                            version = 1,
                            timestampSeconds = 10,
                            overall = TrafficLight.Green,
                            projects = listOf(
                                ProjectTraffic("a1b2c3d4", "loading", TrafficLight.Green, 4, ReasonCode.Work)
                            ),
                            omittedCount = 0,
                            feedItems = listOf(
                                PetFeedItem(
                                    projectId = "a1b2c3d4",
                                    title = "正在推进 loading",
                                    body = "4 秒内有新动作",
                                    light = TrafficLight.Green,
                                    ageSeconds = 4,
                                    reason = ReasonCode.Work,
                                )
                            ),
                            omittedFeedCount = 0,
                        ),
                    ),
                    petMotionEnabled = false,
                )
            }
        }

        composeRule.onNodeWithTag("traffic_pager").performTouchInput { swipeLeft() }

        composeRule.onNodeWithTag("pet_feed_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("avatar_overlay_stage").assertIsDisplayed()
        composeRule.onNodeWithTag("avatar_notification_tray").assertIsDisplayed()
        composeRule.onNodeWithTag("avatar_overlay_mascot").assertIsDisplayed()
        composeRule.onNodeWithText("最新").assertIsDisplayed()
        composeRule.onNodeWithText("正在推进 loading").assertIsDisplayed()
        composeRule.onNodeWithText("4 秒内有新动作").assertIsDisplayed()
    }

    @Test
    fun swipingLeftTwiceShowsGeekStatusScreen() {
        composeRule.setContent {
            TrafficTheme {
                TrafficPagerScreen(
                    uiState = TrafficUiState(
                        connectionStatus = ConnectionStatus.Connected,
                        snapshot = TrafficSnapshot(
                            version = 1,
                            timestampSeconds = 1780039000,
                            overall = TrafficLight.Green,
                            projects = listOf(
                                ProjectTraffic("a1b2c3d4", "loading", TrafficLight.Green, 4, ReasonCode.Work)
                            ),
                            omittedCount = 2,
                            feedItems = listOf(
                                PetFeedItem(
                                    projectId = "a1b2c3d4",
                                    title = "正在推进 loading",
                                    body = "4 秒内有新动作",
                                    light = TrafficLight.Green,
                                    ageSeconds = 4,
                                    reason = ReasonCode.Work,
                                )
                            ),
                            omittedFeedCount = 1,
                        ),
                    ),
                    petMotionEnabled = false,
                )
            }
        }

        composeRule.onNodeWithTag("traffic_pager").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("traffic_pager").performTouchInput { swipeLeft() }

        composeRule.onNodeWithTag("geek_status_screen").assertIsDisplayed()
        composeRule.onNodeWithText("信号矩阵").assertIsDisplayed()
        composeRule.onNodeWithTag("geek_hud_scope").assertIsDisplayed()
        composeRule.onNodeWithTag("geek_signal_loading").assertIsDisplayed()
        composeRule.onNodeWithText("loading").assertIsDisplayed()
    }
}
