package com.codextraffic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficUiState
import kotlin.math.min

@Composable
fun GeekStatusScreen(
    uiState: TrafficUiState,
    modifier: Modifier = Modifier,
) {
    val projects = uiState.snapshot.projects.sortedWith(geekProjectComparator)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GeekColors.Background)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("geek_status_screen"),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("geek_hud_scope"),
        ) {
            drawGeekGrid()
            drawRadarScope(uiState.snapshot.overall)
            drawSignalBars(projects)
            drawScanLine()
        }

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "信号矩阵",
                    color = GeekColors.Ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.sp,
                )
                OverallChip(uiState.snapshot.overall)
            }
            Spacer(Modifier.height(14.dp))
            SignalSummary(uiState)
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(projects, key = { it.id }) { project ->
                    GeekSignalRow(project)
                }
                if (uiState.snapshot.omittedCount > 0) {
                    item {
                        Text(
                            text = "+${uiState.snapshot.omittedCount} 个后台信号",
                            color = GeekColors.Faint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallChip(light: TrafficLight) {
    Row(
        modifier = Modifier
            .background(light.geekAccent().copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(light.geekAccent(), CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = light.geekLabel(),
            color = light.geekAccent(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun SignalSummary(uiState: TrafficUiState) {
    val activeCount = uiState.snapshot.projects.count { it.light == TrafficLight.Green }
    val warmCount = uiState.snapshot.projects.count { it.light == TrafficLight.Yellow }
    val coldCount = uiState.snapshot.projects.count { it.light == TrafficLight.Red }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricCell("推进", activeCount.toString(), GeekColors.Green, Modifier.weight(1f))
        MetricCell("待判", warmCount.toString(), GeekColors.Yellow, Modifier.weight(1f))
        MetricCell("静默", coldCount.toString(), GeekColors.Red, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(GeekColors.Panel, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
        )
        Text(
            text = label,
            color = GeekColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun GeekSignalRow(project: ProjectTraffic) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GeekColors.Row, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("geek_signal_${project.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignalGlyph(project.light)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = project.name,
                color = GeekColors.Ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            Text(
                text = project.reason.wireValue,
                color = GeekColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = project.ageLabel(),
            color = project.light.geekAccent(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun SignalGlyph(light: TrafficLight) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(light.geekAccent().copy(alpha = 0.16f), radius = size.minDimension * 0.48f)
            drawCircle(light.geekAccent(), radius = size.minDimension * 0.18f)
            drawCircle(
                color = light.geekAccent().copy(alpha = 0.7f),
                radius = size.minDimension * 0.43f,
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.drawGeekGrid() {
    val gap = 22.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = GeekColors.Grid,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5f,
        )
        x += gap
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = GeekColors.Grid,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5f,
        )
        y += gap
    }
}

private fun DrawScope.drawRadarScope(light: TrafficLight) {
    val radius = min(size.width, size.height) * 0.34f
    val center = Offset(size.width * 0.72f, size.height * 0.28f)
    val accent = light.geekAccent()
    drawCircle(
        color = accent.copy(alpha = 0.14f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawCircle(
        color = accent.copy(alpha = 0.09f),
        radius = radius * 0.62f,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawLine(
        color = accent.copy(alpha = 0.18f),
        start = center,
        end = Offset(center.x + radius * 0.78f, center.y - radius * 0.44f),
        strokeWidth = 2.dp.toPx(),
    )
}

private fun DrawScope.drawSignalBars(projects: List<ProjectTraffic>) {
    val baseX = size.width * 0.08f
    val baseY = size.height * 0.33f
    val barWidth = 7.dp.toPx()
    projects.take(8).forEachIndexed { index, project ->
        val height = when (project.light) {
            TrafficLight.Green -> 54.dp.toPx()
            TrafficLight.Yellow -> 36.dp.toPx()
            TrafficLight.Red -> 22.dp.toPx()
        }
        val left = baseX + index * 13.dp.toPx()
        drawRoundRect(
            color = project.light.geekAccent().copy(alpha = 0.64f),
            topLeft = Offset(left, baseY - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
    }
}

private fun DrawScope.drawScanLine() {
    val top = size.height * 0.58f
    drawRoundRect(
        color = GeekColors.Blue.copy(alpha = 0.08f),
        topLeft = Offset(0f, top),
        size = Size(size.width, 34.dp.toPx()),
    )
    drawLine(
        color = GeekColors.Blue.copy(alpha = 0.24f),
        start = Offset(0f, top),
        end = Offset(size.width, top),
        strokeWidth = 1.dp.toPx(),
    )
}

private val geekProjectComparator = compareBy<ProjectTraffic>(
    { it.light.geekOrder() },
    { it.ageSeconds },
    { it.name.lowercase() },
)

private fun TrafficLight.geekOrder(): Int = when (this) {
    TrafficLight.Green -> 0
    TrafficLight.Yellow -> 1
    TrafficLight.Red -> 2
}

private fun TrafficLight.geekAccent(): Color = when (this) {
    TrafficLight.Green -> GeekColors.Green
    TrafficLight.Yellow -> GeekColors.Yellow
    TrafficLight.Red -> GeekColors.Red
}

private fun TrafficLight.geekLabel(): String = when (this) {
    TrafficLight.Green -> "ACTIVE"
    TrafficLight.Yellow -> "WATCH"
    TrafficLight.Red -> "QUIET"
}

private fun ProjectTraffic.ageLabel(): String = when {
    ageSeconds < 60 -> "${ageSeconds} 秒"
    ageSeconds < 3600 -> "${ageSeconds / 60} 分"
    else -> "${ageSeconds / 3600} 时"
}

private object GeekColors {
    val Background = Color.Black
    val Panel = Color(0xD90A1014)
    val Row = Color(0xCC080B0E)
    val Grid = Color(0x14258DAD)
    val Ink = Color(0xFFE8F7FF)
    val Muted = Color(0xFF6E8590)
    val Faint = Color(0xFF3B4A50)
    val Blue = Color(0xFF33B8FF)
    val Green = Color(0xFF39E37A)
    val Yellow = Color(0xFFE8C94A)
    val Red = Color(0xFFFF5A5F)
}
