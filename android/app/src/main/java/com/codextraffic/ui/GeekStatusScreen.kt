package com.codextraffic.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class OrbitalSlot(val x: Float, val y: Float)

private val orbitalSlots = listOf(
    OrbitalSlot(0f, -1f),
    OrbitalSlot(0.82f, -0.45f),
    OrbitalSlot(0.76f, 0.52f),
    OrbitalSlot(0f, 1f),
    OrbitalSlot(-0.64f, 0.56f),
    OrbitalSlot(-0.5f, -0.5f),
)

@Composable
fun GeekStatusScreen(
    uiState: TrafficUiState,
    modifier: Modifier = Modifier,
) {
    val projects = uiState.snapshot.projects.sortedWith(geekProjectComparator)
    val scanner by rememberGeekScanner()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GeekColors.Background)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("geek_status_screen"),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("geek_hud_scope"),
        ) {
            drawGeekGrid()
            drawScopeFrame(scanner)
            drawRadarScope(uiState.snapshot.overall, scanner)
            drawSignalBars(projects)
            drawSignalWave(projects, scanner)
            drawScanLine(scanner)
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
                    text = "项目信号",
                    color = GeekColors.Ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.sp,
                )
                OverallChip(uiState.snapshot.overall)
            }
            Spacer(Modifier.height(12.dp))
            SignalSummary(uiState)
            Spacer(Modifier.height(10.dp))
            OrbitalHud(
                projects = projects,
                overall = uiState.snapshot.overall,
                scanner = scanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .testTag("geek_orbital_hud"),
            )
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .testTag("geek_scanner_motion"),
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(5.dp),
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
private fun rememberGeekScanner(): State<Float> {
    val transition = rememberInfiniteTransition(label = "geek_scanner")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "geek_scan_phase",
    )
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
private fun OrbitalHud(
    projects: List<ProjectTraffic>,
    overall: TrafficLight,
    scanner: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight)
        val orbitProjects = projects.take(6)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.52f)
            val radius = min(size.width, size.height) * 0.34f
            val scanAngle = (scanner * 360f - 90f) * PI.toFloat() / 180f
            drawRoundRect(
                color = GeekColors.PanelStroke.copy(alpha = 0.28f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.03f),
                size = Size(size.width * 0.84f, size.height * 0.9f),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = overall.geekAccent().copy(alpha = 0.05f),
                radius = radius * 1.08f,
                center = center,
            )
            listOf(1f, 0.68f, 0.36f).forEach { scale ->
                drawCircle(
                    color = overall.geekAccent().copy(alpha = 0.18f * scale),
                    radius = radius * scale,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawLine(
                color = GeekColors.Blue.copy(alpha = 0.28f),
                start = Offset(center.x - radius * 1.18f, center.y),
                end = Offset(center.x + radius * 1.18f, center.y),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = GeekColors.Blue.copy(alpha = 0.20f),
                start = Offset(center.x, center.y - radius * 1.18f),
                end = Offset(center.x, center.y + radius * 1.18f),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = overall.geekAccent().copy(alpha = 0.46f),
                start = center,
                end = Offset(
                    x = center.x + cos(scanAngle).toFloat() * radius * 1.08f,
                    y = center.y + sin(scanAngle).toFloat() * radius * 1.08f,
                ),
                strokeWidth = 2.dp.toPx(),
            )
            orbitProjects.forEachIndexed { index, project ->
                val angle = (-90.0 + index * (360.0 / orbitProjects.size.coerceAtLeast(1))) * PI / 180.0
                val pointRadius = radius * when (project.light) {
                    TrafficLight.Green -> 0.84f
                    TrafficLight.Yellow -> 0.64f
                    TrafficLight.Red -> 0.44f
                }
                val slot = orbitalSlots[index % orbitalSlots.size]
                val point = Offset(x = center.x + slot.x * pointRadius, y = center.y + slot.y * pointRadius)
                drawLine(
                    color = project.light.geekAccent().copy(alpha = 0.18f),
                    start = center,
                    end = point,
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(
                    color = project.light.geekAccent().copy(alpha = 0.18f),
                    radius = 13.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = project.light.geekAccent(),
                    radius = 5.dp.toPx(),
                    center = point,
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = overall.geekCoreLabel(),
                color = overall.geekAccent(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.sp,
            )
            Text(
                text = "${projects.count()} 个项目",
                color = GeekColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
            )
        }

        orbitProjects.forEachIndexed { index, project ->
            val slot = orbitalSlots[index % orbitalSlots.size]
            val orbit = side * when (project.light) {
                TrafficLight.Green -> 0.34f
                TrafficLight.Yellow -> 0.27f
                TrafficLight.Red -> 0.20f
            }
            OrbitalProjectChip(
                project = project,
                modifier = Modifier
                    .offset(
                        x = (slot.x * orbit.value).dp,
                        y = (slot.y * orbit.value).dp,
                    )
                    .testTag("geek_project_orbit_${project.name}"),
            )
        }
    }
}

@Composable
private fun OrbitalProjectChip(
    project: ProjectTraffic,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .widthIn(max = 92.dp)
            .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(50))
            .border(1.dp, project.light.geekAccent().copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(project.light.geekAccent(), CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = project.name,
            color = GeekColors.Ink,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun GeekSignalRow(project: ProjectTraffic) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GeekColors.Row, RoundedCornerShape(4.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
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
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            Text(
                text = project.geekReasonLabel(),
                color = GeekColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = project.ageLabel(),
            color = project.light.geekAccent(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
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

private fun DrawScope.drawScopeFrame(scanner: Float) {
    val inset = 10.dp.toPx()
    val corner = 18.dp.toPx()
    val accent = GeekColors.Blue.copy(alpha = 0.32f + scanner * 0.12f)
    val len = 34.dp.toPx()
    drawLine(accent, Offset(inset, inset), Offset(inset + len, inset), 1.4.dp.toPx())
    drawLine(accent, Offset(inset, inset), Offset(inset, inset + len), 1.4.dp.toPx())
    drawLine(accent, Offset(size.width - inset, inset), Offset(size.width - inset - len, inset), 1.4.dp.toPx())
    drawLine(accent, Offset(size.width - inset, inset), Offset(size.width - inset, inset + len), 1.4.dp.toPx())
    drawLine(accent, Offset(inset, size.height - inset), Offset(inset + len, size.height - inset), 1.4.dp.toPx())
    drawLine(accent, Offset(inset, size.height - inset), Offset(inset, size.height - inset - len), 1.4.dp.toPx())
    drawLine(accent, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - len, size.height - inset), 1.4.dp.toPx())
    drawLine(accent, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - len), 1.4.dp.toPx())
    drawRoundRect(
        color = GeekColors.PanelStroke.copy(alpha = 0.14f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = 1.dp.toPx()),
    )
}

private fun DrawScope.drawRadarScope(light: TrafficLight, scanner: Float) {
    val radius = min(size.width, size.height) * 0.34f
    val center = Offset(size.width * 0.72f, size.height * 0.28f)
    val accent = light.geekAccent()
    val scanAngle = (scanner * 360f - 90f) * PI.toFloat() / 180f
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
        color = accent.copy(alpha = 0.38f),
        start = center,
        end = Offset(
            x = center.x + cos(scanAngle).toFloat() * radius,
            y = center.y + sin(scanAngle).toFloat() * radius,
        ),
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

private fun DrawScope.drawSignalWave(projects: List<ProjectTraffic>, scanner: Float) {
    if (projects.isEmpty()) return
    val baseY = size.height * 0.42f
    val step = size.width / 14f
    var previous = Offset(0f, baseY)
    for (index in 1..14) {
        val project = projects[(index - 1) % projects.size]
        val amplitude = when (project.light) {
            TrafficLight.Green -> 19.dp.toPx()
            TrafficLight.Yellow -> 12.dp.toPx()
            TrafficLight.Red -> 7.dp.toPx()
        }
        val phase = scanner * PI.toFloat() * 2f
        val point = Offset(
            x = index * step,
            y = baseY + sin(index * 0.9f + phase) * amplitude,
        )
        drawLine(
            color = project.light.geekAccent().copy(alpha = 0.24f),
            start = previous,
            end = point,
            strokeWidth = 1.2.dp.toPx(),
        )
        previous = point
    }
}

private fun DrawScope.drawScanLine(scanner: Float) {
    val top = size.height * (0.48f + scanner * 0.34f)
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
    TrafficLight.Green -> "推进"
    TrafficLight.Yellow -> "待判"
    TrafficLight.Red -> "静默"
}

private fun TrafficLight.geekCoreLabel(): String = when (this) {
    TrafficLight.Green -> "推进"
    TrafficLight.Yellow -> "待判"
    TrafficLight.Red -> "静默"
}

private fun ProjectTraffic.ageLabel(): String = when {
    ageSeconds < 60 -> "${ageSeconds} 秒"
    ageSeconds < 3600 -> "${ageSeconds / 60} 分"
    else -> "${ageSeconds / 3600} 时"
}

private fun ProjectTraffic.geekReasonLabel(): String = when (reason) {
    ReasonCode.Work -> "正在推进"
    ReasonCode.Recent -> "刚有动静"
    ReasonCode.Idle -> "暂时安静"
    ReasonCode.Stale -> "疑似卡住"
    ReasonCode.Blocked -> "等待处理"
    ReasonCode.CodexOff -> "Codex 离线"
}

private object GeekColors {
    val Background = Color.Black
    val Panel = Color(0xD90A1014)
    val Row = Color(0xCC080B0E)
    val PanelStroke = Color(0xFF1E9BC2)
    val Grid = Color(0x14258DAD)
    val Ink = Color(0xFFE8F7FF)
    val Muted = Color(0xFF6E8590)
    val Faint = Color(0xFF3B4A50)
    val Blue = Color(0xFF33B8FF)
    val Green = Color(0xFF39E37A)
    val Yellow = Color(0xFFE8C94A)
    val Red = Color(0xFFFF5A5F)
}
