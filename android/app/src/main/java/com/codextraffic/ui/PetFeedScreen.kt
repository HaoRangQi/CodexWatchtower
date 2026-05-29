package com.codextraffic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codextraffic.model.PetFeedItem
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficUiState

@Composable
fun PetFeedScreen(
    uiState: TrafficUiState,
    modifier: Modifier = Modifier,
) {
    val feedItems = uiState.snapshot.feedItems.sortedWith(feedComparator)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PetFeedColors.Background)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("pet_feed_screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "宠物动态",
                color = PetFeedColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Text(
                text = "←",
                color = PetFeedColors.Muted,
                fontSize = 18.sp,
                letterSpacing = 0.sp,
            )
        }
        Spacer(Modifier.height(10.dp))

        if (feedItems.isEmpty()) {
            EmptyPetFeed()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(feedItems, key = { "${it.projectId}_${it.reason.wireValue}_${it.ageSeconds}" }) { item ->
                    PetFeedRow(item)
                }
                if (uiState.snapshot.omittedFeedCount > 0) {
                    item {
                        Text(
                            text = "+${uiState.snapshot.omittedFeedCount} 条",
                            color = PetFeedColors.Dim,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 5.dp),
                            letterSpacing = 0.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPetFeed() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "暂无动态",
            color = PetFeedColors.Muted,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun PetFeedRow(item: PetFeedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PetFeedColors.Row)
            .padding(horizontal = 8.dp, vertical = 9.dp)
            .testTag("feed_${item.projectId}"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(9.dp)
                .background(item.light.accent(), CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = PetFeedColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = item.body,
                color = PetFeedColors.Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.shortAge(),
            color = PetFeedColors.Dim,
            fontSize = 11.sp,
            letterSpacing = 0.sp,
        )
    }
}

private val feedComparator = compareBy<PetFeedItem>(
    { it.priority() },
    { it.ageSeconds },
    { it.title.lowercase() },
)

private fun PetFeedItem.priority(): Int = when (reason) {
    ReasonCode.Stale,
    ReasonCode.Blocked,
    ReasonCode.CodexOff -> 0
    ReasonCode.Work -> 1
    ReasonCode.Recent -> 2
    ReasonCode.Idle -> 3
}

private fun PetFeedItem.shortAge(): String = when {
    ageSeconds < 60 -> "${ageSeconds} 秒"
    ageSeconds < 3600 -> "${ageSeconds / 60} 分"
    else -> "${ageSeconds / 3600} 时"
}

private fun TrafficLight.accent() = when (this) {
    TrafficLight.Green -> PetFeedColors.Green
    TrafficLight.Yellow -> PetFeedColors.Yellow
    TrafficLight.Red -> PetFeedColors.Red
}

private object PetFeedColors {
    val Background = androidx.compose.ui.graphics.Color.Black
    val Row = androidx.compose.ui.graphics.Color(0xFF050505)
    val Ink = androidx.compose.ui.graphics.Color(0xFFE8E1CA)
    val Muted = androidx.compose.ui.graphics.Color(0xFF8A8A8A)
    val Dim = androidx.compose.ui.graphics.Color(0xFF4B4B4B)
    val Green = androidx.compose.ui.graphics.Color(0xFF36D66B)
    val Yellow = androidx.compose.ui.graphics.Color(0xFFE9C846)
    val Red = androidx.compose.ui.graphics.Color(0xFFE34A4A)
}
