package com.codextraffic.ui

import androidx.compose.ui.test.assertIsDisplayed
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
    fun showsOverallLightAndProjectRows() {
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

        composeRule.onNodeWithTag("overall_light").assertIsDisplayed()
        composeRule.onNodeWithTag("overall_label").assertIsDisplayed()
        composeRule.onNodeWithText("WORKING").assertIsDisplayed()
        composeRule.onNodeWithText("LOADING").assertIsDisplayed()
        composeRule.onNodeWithText("4s / WORK").assertIsDisplayed()
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

        composeRule.onNodeWithTag("connection_status").assertIsDisplayed()
        composeRule.onNodeWithText("DISCONNECTED").assertIsDisplayed()
        composeRule.onNodeWithText("NO PROJECT SIGNAL").assertIsDisplayed()
    }
}

