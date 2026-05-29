package com.codextraffic.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codextraffic.TrafficViewModel
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.ProjectTraffic
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficUiState

private val Ink = Color(0xFFE7E1CC)
private val Panel = Color(0xFF202020)
private val PanelDark = Color(0xFF141414)
private val GridLine = Color(0xFF333333)
private val BotShell = Color(0xFFD9D1BD)
private val BotShadow = Color(0xFF9E9586)
private val BotScreen = Color(0xFF101716)
private val PixelGreen = Color(0xFF36D66B)
private val PixelYellow = Color(0xFFE9C846)
private val PixelRed = Color(0xFFE34A4A)
private val PixelBlue = Color(0xFF69B7FF)
private val Muted = Color(0xFF9C9C9C)

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
            .border(3.dp, GridLine)
            .background(Panel)
            .padding(14.dp)
            .testTag("bot_panel"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelBot(
            mood = summary.mood,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 220.dp)
                .aspectRatio(1f)
                .testTag("pixel_bot"),
        )
        Spacer(Modifier.height(12.dp))
        StatusBubble(
            summary = summary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PixelBot(
    mood: BotMood,
    modifier: Modifier = Modifier,
) {
    val eye = mood.eyeColor
    val mouth = mood.mouthPattern
    val accent = mood.accent

    Box(
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .border(4.dp, GridLine)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                PixelBlock(accent, 10)
                Spacer(Modifier.width(22.dp))
                PixelBlock(accent, 10)
            }
            Spacer(Modifier.height(4.dp))
            Box {
                Column(
                    modifier = Modifier
                        .width(106.dp)
                        .background(BotShell, RectangleShape)
                        .border(4.dp, BotShadow)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PixelBlock(eye, 18)
                        PixelBlock(eye, 18)
                    }
                    Spacer(Modifier.height(10.dp))
                    PixelMouth(mouth, accent)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        PixelBlock(accent.copy(alpha = 0.65f), 8)
                        PixelBlock(accent.copy(alpha = 0.45f), 8)
                        PixelBlock(accent.copy(alpha = 0.30f), 8)
                    }
                }
                PixelBlock(accent, 12, Modifier.align(Alignment.TopStart))
                PixelBlock(accent, 12, Modifier.align(Alignment.TopEnd))
            }
            Row(
                modifier = Modifier.width(132.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PixelBlock(BotShell, 16)
                PixelBlock(BotShell, 16)
            }
            Column(
                modifier = Modifier
                    .width(92.dp)
                    .background(BotShell)
                    .border(4.dp, BotShadow)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(BotScreen)
                        .border(2.dp, accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mood.bellyText,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                    )
                }
            }
            Row(
                modifier = Modifier.width(76.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PixelBlock(BotShadow, 18)
                PixelBlock(BotShadow, 18)
            }
        }
    }
}

@Composable
private fun PixelMouth(pattern: MouthPattern, color: Color) {
    val rows = when (pattern) {
        MouthPattern.Smile -> listOf("10001", "01110")
        MouthPattern.Flat -> listOf("00000", "11111")
        MouthPattern.Alert -> listOf("00100", "00100", "00100")
        MouthPattern.Sleep -> listOf("01010", "10101")
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row {
                row.forEach { cell ->
                    PixelBlock(
                        color = if (cell == '1') color else Color.Transparent,
                        size = 6,
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelBlock(
    color: Color,
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color, RectangleShape),
    )
}

@Composable
private fun StatusBubble(
    summary: BotSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .border(3.dp, summary.mood.accent)
            .background(PanelDark)
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
                .background(status.color)
                .border(2.dp, Color(0xFF050505)),
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
    ReasonCode.Work -> ProjectStatusLabel("推进中", PixelGreen)
    ReasonCode.Recent -> ProjectStatusLabel("观察", PixelYellow)
    ReasonCode.Idle -> ProjectStatusLabel("空闲", Muted)
    ReasonCode.Stale -> ProjectStatusLabel("卡住?", PixelRed)
    ReasonCode.Blocked -> ProjectStatusLabel("阻塞", PixelRed)
    ReasonCode.CodexOff -> ProjectStatusLabel("离线", PixelRed)
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
    ConnectionStatus.Connected -> PixelGreen
    ConnectionStatus.Connecting,
    ConnectionStatus.Scanning -> PixelYellow
    ConnectionStatus.PermissionMissing,
    ConnectionStatus.BluetoothOff,
    ConnectionStatus.Error -> PixelRed
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

private enum class MouthPattern {
    Smile,
    Flat,
    Alert,
    Sleep,
}

private enum class BotMood(
    val accent: Color,
    val eyeColor: Color,
    val bellyText: String,
    val mouthPattern: MouthPattern,
) {
    Happy(PixelGreen, PixelGreen, "跑", MouthPattern.Smile),
    Watch(PixelYellow, PixelYellow, "看", MouthPattern.Flat),
    Alert(PixelRed, PixelRed, "!!!", MouthPattern.Alert),
    Sleepy(Muted, Color(0xFF6C6C6C), "歇", MouthPattern.Sleep),
    Offline(PixelBlue, Color(0xFF5E7380), "等", MouthPattern.Flat),
}
