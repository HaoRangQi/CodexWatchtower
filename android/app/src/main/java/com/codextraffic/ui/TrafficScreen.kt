package com.codextraffic.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.codextraffic.TrafficViewModel
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficUiState
import kotlin.math.min

private val Ink = Color(0xFFE8E1CA)
private val Panel = Color.Black
private val PanelDark = Color.Black
private val GridLine = Color(0xFF171717)
private val BotShell = Color(0xFFC9922D)
private val BotShellDark = Color(0xFF5F4316)
private val BotShadow = Color(0xFF4A3411)
private val BotScreen = Color(0xFF020706)
private val LensRim = Color(0xFF9B772E)
private val LensGlass = Color(0xFF061012)
private val TreadRubber = Color(0xFF080808)
private val TreadDot = Color(0xFF262626)
private val StatusGreen = Color(0xFF36D66B)
private val StatusYellow = Color(0xFFE9C846)
private val StatusRed = Color(0xFFE34A4A)
private val StatusBlue = Color(0xFF69B7FF)
private val Muted = Color(0xFF747474)

@Composable
fun CodexTrafficApp(viewModel: TrafficViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    TrafficTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PanelDark,
        ) {
            TrafficScreen(uiState = uiState)
        }
    }
}

@Composable
fun TrafficTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = PanelDark,
            surface = Panel,
            onBackground = Ink,
            onSurface = Ink,
        ),
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
        ),
        content = content,
    )
}

@Composable
fun TrafficScreen(uiState: TrafficUiState) {
    val summary = uiState.summary()
    val sortedProjects = uiState.snapshot.projects.sortedWith(projectComparator)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelDark)
            .padding(18.dp),
    ) {
        Header(uiState.connectionStatus)
        Spacer(Modifier.height(14.dp))
        BotStatusPanel(summary)
        Spacer(Modifier.height(16.dp))
        ProjectList(
            projects = sortedProjects,
            omittedCount = uiState.snapshot.omittedCount,
        )
    }
}

@Composable
private fun Header(connectionStatus: ConnectionStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "CODEX 桌宠",
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
        )
        Text(
            modifier = Modifier
                .border(2.dp, connectionStatus.accent())
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("connection_status"),
            text = connectionStatus.label(),
            color = connectionStatus.accent(),
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun BotStatusPanel(summary: BotSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLine, RoundedCornerShape(8.dp))
            .background(Panel, RoundedCornerShape(8.dp))
            .padding(14.dp)
            .testTag("bot_panel"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PetBot(
            mood = summary.mood,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = 260.dp)
                .aspectRatio(1f)
                .testTag("pet_bot"),
        )
        Spacer(Modifier.height(12.dp))
        StatusBubble(
            summary = summary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PetBot(
    mood: BotMood,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .background(Color.Black)
            .border(1.dp, GridLine, RoundedCornerShape(8.dp)),
    ) {
        drawPet(mood)
    }
}

private fun DrawScope.drawPet(mood: BotMood) {
    val side = min(size.width, size.height)
    val w = size.width
    val h = size.height
    val unit = side / 100f
    val accent = mood.accent

    drawRoundRect(
        color = Color(0xFF020202),
        topLeft = Offset(w * 0.10f, h * 0.80f),
        size = Size(w * 0.80f, h * 0.06f),
        cornerRadius = CornerRadius(unit * 10f, unit * 10f),
    )

    drawArm(w * 0.31f, h * 0.56f, unit, accent, left = true)
    drawArm(w * 0.69f, h * 0.56f, unit, accent, left = false)

    drawTreads(w, h, unit, accent)
    drawBody(w, h, unit, mood)
    drawNeck(w, h, unit)
    drawEyeBridge(w, h, unit)
    drawLens(Offset(w * 0.37f, h * 0.27f), unit, mood, left = true)
    drawLens(Offset(w * 0.63f, h * 0.27f), unit, mood, left = false)
}

private fun DrawScope.drawLens(
    center: Offset,
    unit: Float,
    mood: BotMood,
    left: Boolean,
) {
    val moodAccent = mood.accent
    val outer = Size(unit * 27f, unit * 22f)
    val outerTopLeft = Offset(center.x - outer.width / 2f, center.y - outer.height / 2f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFD2AA55), LensRim, Color(0xFF342409)),
            center = Offset(center.x - unit * 4f, center.y - unit * 5f),
            radius = unit * 18f,
        ),
        topLeft = outerTopLeft,
        size = outer,
    )
    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(outerTopLeft.x + unit * 1.2f, outerTopLeft.y + unit * 1.4f),
        size = Size(outer.width - unit * 2.4f, outer.height - unit * 2.6f),
        style = Stroke(width = unit * 1.1f),
    )

    val glass = Size(unit * 20f, unit * 14.5f)
    val glassTopLeft = Offset(center.x - glass.width / 2f, center.y - glass.height / 2f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF132325), LensGlass, Color.Black),
            center = Offset(center.x - unit * 3f, center.y - unit * 3f),
            radius = unit * 14f,
        ),
        topLeft = glassTopLeft,
        size = glass,
    )

    when (mood.eyePattern) {
        EyePattern.Bright -> {
            drawCircle(moodAccent.copy(alpha = 0.16f), radius = unit * 6.8f, center = center)
            drawCircle(Color(0xFF173A29), radius = unit * 5.2f, center = center)
            drawCircle(moodAccent.copy(alpha = 0.86f), radius = unit * 2.8f, center = center)
            drawCircle(Color.Black, radius = unit * 1.1f, center = center)
        }

        EyePattern.Watch -> {
            val watchCenter = center.copy(x = center.x + if (left) unit * 1.6f else -unit * 1.6f)
            drawCircle(moodAccent.copy(alpha = 0.15f), radius = unit * 6.0f, center = watchCenter)
            drawCircle(Color(0xFF3A3217), radius = unit * 4.8f, center = watchCenter)
            drawCircle(moodAccent.copy(alpha = 0.82f), radius = unit * 2.5f, center = watchCenter)
            drawCircle(Color.Black, radius = unit * 1.1f, center = watchCenter)
        }

        EyePattern.Alert -> {
            drawRoundRect(
                color = moodAccent,
                topLeft = Offset(center.x - unit * 1.2f, center.y - unit * 5f),
                size = Size(unit * 2.4f, unit * 7.2f),
                cornerRadius = CornerRadius(unit * 1.2f, unit * 1.2f),
            )
            drawCircle(moodAccent, radius = unit * 1.5f, center = Offset(center.x, center.y + unit * 5f))
        }

        EyePattern.Sleep -> {
            drawLine(
                color = mood.eyeColor,
                start = Offset(center.x - unit * 6f, center.y),
                end = Offset(center.x + unit * 6f, center.y),
                strokeWidth = unit * 2.1f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }

        EyePattern.Offline -> {
            drawCircle(mood.eyeColor.copy(alpha = 0.25f), radius = unit * 4.5f, center = center)
            drawCircle(mood.eyeColor, radius = unit * 2.0f, center = center)
        }
    }

    drawCircle(
        color = Color.White.copy(alpha = 0.30f),
        radius = unit * 1.7f,
        center = Offset(center.x + if (left) -unit * 4.2f else unit * 4.2f, center.y - unit * 4.6f),
    )
}

private fun DrawScope.drawEyeBridge(w: Float, h: Float, unit: Float) {
    drawRoundRect(
        color = BotShadow,
        topLeft = Offset(w * 0.46f, h * 0.31f),
        size = Size(w * 0.08f, unit * 4f),
        cornerRadius = CornerRadius(unit * 2f, unit * 2f),
    )
}

private fun DrawScope.drawNeck(w: Float, h: Float, unit: Float) {
    drawRoundRect(
        color = BotShadow,
        topLeft = Offset(w * 0.475f, h * 0.36f),
        size = Size(w * 0.05f, h * 0.10f),
        cornerRadius = CornerRadius(unit * 2f, unit * 2f),
    )
    drawCircle(BotShellDark, radius = unit * 2.1f, center = Offset(w * 0.50f, h * 0.41f))
}

private fun DrawScope.drawBody(w: Float, h: Float, unit: Float, mood: BotMood) {
    val bodyLeft = w * 0.28f
    val bodyTop = h * 0.45f
    val bodyWidth = w * 0.44f
    val bodyHeight = h * 0.25f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFD9A849), BotShell, BotShellDark),
            startY = bodyTop,
            endY = bodyTop + bodyHeight,
        ),
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(unit * 6f, unit * 6f),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.40f),
        topLeft = Offset(bodyLeft + bodyWidth * 0.70f, bodyTop + unit * 3f),
        size = Size(bodyWidth * 0.23f, bodyHeight - unit * 8f),
        cornerRadius = CornerRadius(unit * 4f, unit * 4f),
    )
    drawRoundRect(
        color = BotShadow,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(unit * 6f, unit * 6f),
        style = Stroke(width = unit * 1.25f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.18f),
        radius = unit * 3.5f,
        center = Offset(bodyLeft + bodyWidth * 0.23f, bodyTop + bodyHeight * 0.20f),
    )

    val screenLeft = bodyLeft + bodyWidth * 0.16f
    val screenTop = bodyTop + bodyHeight * 0.34f
    val screenWidth = bodyWidth * 0.68f
    val screenHeight = bodyHeight * 0.30f
    drawRoundRect(
        color = BotScreen,
        topLeft = Offset(screenLeft, screenTop),
        size = Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(unit * 3.2f, unit * 3.2f),
    )
    drawRoundRect(
        color = mood.accent.copy(alpha = 0.62f),
        topLeft = Offset(screenLeft, screenTop),
        size = Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(unit * 3.2f, unit * 3.2f),
        style = Stroke(width = unit * 0.85f),
    )
    drawContext.canvas.nativeCanvas.drawText(
        mood.bellyText,
        screenLeft + screenWidth / 2f,
        screenTop + screenHeight * 0.68f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mood.accent.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = unit * 6.2f
            typeface = Typeface.DEFAULT_BOLD
        },
    )

    repeat(5) { index ->
        drawCircle(
            color = if (index == 2) mood.accent else BotShadow,
            radius = unit * 1.45f,
            center = Offset(bodyLeft + bodyWidth * (0.22f + index * 0.14f), bodyTop + bodyHeight * 0.80f),
        )
    }
}

private fun DrawScope.drawArm(anchorX: Float, anchorY: Float, unit: Float, accent: Color, left: Boolean) {
    val dir = if (left) -1f else 1f
    drawLine(
        color = BotShadow,
        start = Offset(anchorX, anchorY),
        end = Offset(anchorX + dir * unit * 10f, anchorY + unit * 8f),
        strokeWidth = unit * 2.5f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
    drawCircle(BotShellDark, radius = unit * 3f, center = Offset(anchorX + dir * unit * 11f, anchorY + unit * 9f))
    drawCircle(accent.copy(alpha = 0.38f), radius = unit * 1.4f, center = Offset(anchorX + dir * unit * 11f, anchorY + unit * 9f))
}

private fun DrawScope.drawTreads(w: Float, h: Float, unit: Float, accent: Color) {
    val left = w * 0.24f
    val top = h * 0.70f
    val width = w * 0.52f
    val height = h * 0.12f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF171717), TreadRubber, Color.Black),
            startY = top,
            endY = top + height,
        ),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f),
    )
    drawRoundRect(
        color = BotShadow,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f),
        style = Stroke(width = unit * 1.1f),
    )
    repeat(6) { index ->
        drawCircle(
            color = if (index == 2 || index == 3) accent.copy(alpha = 0.45f) else TreadDot,
            radius = unit * 2.7f,
            center = Offset(left + width * (0.18f + index * 0.13f), top + height / 2f),
        )
    }
}

@Composable
private fun StatusBubble(
    summary: BotSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .border(2.dp, summary.mood.accent.copy(alpha = 0.62f), RoundedCornerShape(6.dp))
            .background(PanelDark, RoundedCornerShape(6.dp))
            .padding(12.dp),
    ) {
        Text(
            modifier = Modifier.testTag("bot_summary"),
            text = summary.title,
            color = summary.mood.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = summary.detail,
            color = Ink,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = summary.action,
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun ProjectList(
    projects: List<ProjectTraffic>,
    omittedCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, GridLine)
            .background(Panel)
            .padding(12.dp)
            .testTag("project_list"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("项目看板", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (omittedCount > 0) {
                Text("另有 $omittedCount 项", color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (projects.isEmpty()) {
            Text(
                modifier = Modifier.testTag("empty_projects"),
                text = "桌宠还没收到项目信号",
                color = Muted,
                fontSize = 14.sp,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectRow(project)
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: ProjectTraffic) {
    val status = project.statusLabel()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelDark)
            .border(2.dp, GridLine)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("project_${project.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(status.color, CircleShape)
                .border(1.dp, Color(0xFF050505), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = project.name.uppercase(),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            Text(
                text = "${project.ageSeconds} 秒 · ${project.reason.label()}",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = status.text,
            color = status.color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.sp,
        )
    }
}

private val projectComparator = compareBy<ProjectTraffic>(
    { it.priority() },
    { it.ageSeconds },
    { it.name.lowercase() },
)

private fun TrafficUiState.summary(): BotSummary {
    if (connectionStatus != ConnectionStatus.Connected) {
        return BotSummary(
            mood = BotMood.Offline,
            title = connectionStatus.label(),
            detail = "我暂时听不到 Mac companion 的信号。",
            action = connectionStatus.actionText(),
        )
    }

    val projects = snapshot.projects
    if (projects.isEmpty()) {
        return BotSummary(
            mood = BotMood.Sleepy,
            title = "待机中",
            detail = "桌宠还没收到项目信号。",
            action = "先保持 companion 运行，等 Codex 有动作后我会盯着。",
        )
    }

    val attention = projects.count { it.needsAttention() }
    val working = projects.count { it.reason == ReasonCode.Work }
    val recent = projects.count { it.reason == ReasonCode.Recent }

    return when {
        attention > 0 -> BotSummary(
            mood = BotMood.Alert,
            title = "$attention 个项目需要看一眼",
            detail = "可能是卡住、阻塞，或 Codex 已经不在运行。",
            action = "优先看列表最上面的项目。",
        )

        working > 0 -> BotSummary(
            mood = BotMood.Happy,
            title = "$working 个项目正在推进",
            detail = "我会每 2 秒刷新一次状态，先让它干活。",
            action = "不用一直盯着，有异常我会把项目顶到前面。",
        )

        recent > 0 -> BotSummary(
            mood = BotMood.Watch,
            title = "最近有动静",
            detail = "项目刚活跃过，但当前没有明确推进信号。",
            action = "可以先放着观察一会儿。",
        )

        else -> BotSummary(
            mood = BotMood.Sleepy,
            title = "现在比较安静",
            detail = "没有项目显示正在推进。",
            action = "如果你预期它在跑，可以回到 Codex 看是否等待输入。",
        )
    }
}

private fun ProjectTraffic.needsAttention(): Boolean = reason == ReasonCode.Stale ||
    reason == ReasonCode.Blocked ||
    reason == ReasonCode.CodexOff

private fun ProjectTraffic.priority(): Int = when {
    needsAttention() -> 0
    reason == ReasonCode.Work -> 1
    reason == ReasonCode.Recent -> 2
    reason == ReasonCode.Idle -> 3
    else -> 4
}

private fun ProjectTraffic.statusLabel(): ProjectStatusLabel = when (reason) {
    ReasonCode.Work -> ProjectStatusLabel("推进中", StatusGreen)
    ReasonCode.Recent -> ProjectStatusLabel("观察", StatusYellow)
    ReasonCode.Idle -> ProjectStatusLabel("空闲", Muted)
    ReasonCode.Stale -> ProjectStatusLabel("卡住?", StatusRed)
    ReasonCode.Blocked -> ProjectStatusLabel("阻塞", StatusRed)
    ReasonCode.CodexOff -> ProjectStatusLabel("离线", StatusRed)
}

private fun ReasonCode.label(): String = when (this) {
    ReasonCode.Work -> "正在干活"
    ReasonCode.Recent -> "最近活跃"
    ReasonCode.Idle -> "空闲"
    ReasonCode.Stale -> "疑似卡住"
    ReasonCode.Blocked -> "已阻塞"
    ReasonCode.CodexOff -> "Codex 未运行"
}

private fun ConnectionStatus.label(): String = when (this) {
    ConnectionStatus.Disconnected -> "未连接"
    ConnectionStatus.Scanning -> "扫描中"
    ConnectionStatus.Connecting -> "连接中"
    ConnectionStatus.Connected -> "已连接"
    ConnectionStatus.PermissionMissing -> "缺少权限"
    ConnectionStatus.BluetoothOff -> "蓝牙关闭"
    ConnectionStatus.Error -> "连接异常"
}

private fun ConnectionStatus.actionText(): String = when (this) {
    ConnectionStatus.PermissionMissing -> "先授予附近设备/蓝牙权限。"
    ConnectionStatus.BluetoothOff -> "先打开手机蓝牙。"
    ConnectionStatus.Scanning -> "我正在找 Mac 上的 Codex Traffic。"
    ConnectionStatus.Connecting -> "已经找到设备，正在连接。"
    ConnectionStatus.Error -> "连接失败，稍后会自动重试。"
    ConnectionStatus.Disconnected -> "确认 Mac companion 正在运行。"
    ConnectionStatus.Connected -> "连接正常。"
}

private fun ConnectionStatus.accent(): Color = when (this) {
    ConnectionStatus.Connected -> StatusGreen
    ConnectionStatus.Connecting,
    ConnectionStatus.Scanning -> StatusYellow
    ConnectionStatus.PermissionMissing,
    ConnectionStatus.BluetoothOff,
    ConnectionStatus.Error -> StatusRed
    ConnectionStatus.Disconnected -> Muted
}

private data class ProjectStatusLabel(
    val text: String,
    val color: Color,
)

private data class BotSummary(
    val mood: BotMood,
    val title: String,
    val detail: String,
    val action: String,
)

private enum class EyePattern {
    Bright,
    Watch,
    Alert,
    Sleep,
    Offline,
}

private enum class BotMood(
    val accent: Color,
    val eyeColor: Color,
    val bellyText: String,
    val eyePattern: EyePattern,
) {
    Happy(StatusGreen, StatusGreen, "忙", EyePattern.Bright),
    Watch(StatusYellow, StatusYellow, "看", EyePattern.Watch),
    Alert(StatusRed, StatusRed, "!!!", EyePattern.Alert),
    Sleepy(Muted, Color(0xFF5C5C5C), "歇", EyePattern.Sleep),
    Offline(StatusBlue, Color(0xFF4E6470), "等", EyePattern.Offline),
}
