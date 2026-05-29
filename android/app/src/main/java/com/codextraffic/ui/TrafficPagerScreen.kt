package com.codextraffic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.TrafficUiState

@Composable
fun TrafficPagerScreen(
    uiState: TrafficUiState,
    petMotionEnabled: Boolean = true,
    onHideProject: (ProjectTraffic) -> Unit = {},
    onRestoreProject: (ProjectTraffic) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(PagerBackground)
            .testTag("traffic_pager"),
    ) { page ->
        when (page) {
            0 -> TrafficScreen(
                uiState = uiState,
                petMotionEnabled = petMotionEnabled,
                onHideProject = onHideProject,
                onRestoreProject = onRestoreProject,
            )
            1 -> PetFeedScreen(uiState = uiState)
        }
    }
}

private val PagerBackground = androidx.compose.ui.graphics.Color.Black
