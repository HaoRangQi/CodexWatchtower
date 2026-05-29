package com.codextraffic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.min

private val Ink = Color(0xFFE8E1CA)
private val Panel = Color.Black
private val PanelDark = Color.Black
private val GridLine = Color(0xFF171717)
private val BotShell = Color(0xFFECEFF4)
private val BotShellDark = Color(0xFF5E6978)
private val BotShadow = Color(0xFF131820)
private val BotScreen = Color(0xFF198FE8)
private val BotCheek = Color(0xFFFF8AB4)
private val StatusGreen = Color(0xFF36D66B)
private val StatusYellow = Color(0xFFE9C846)
private val StatusRed = Color(0xFFE34A4A)
private val StatusBlue = Color(0xFF69B7FF)
private val Muted = Color(0xFF747474)
private const val CodexBsodAssetFile = "codex_bsod_spritesheet.webp"
private const val BsodFrameWidth = 192
private const val BsodFrameHeight = 208

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
    val context = LocalContext.current
    val bsodSpritesheet = remember(context) {
        runCatching {
            context.assets.open(CodexBsodAssetFile).use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .border(1.dp, GridLine, RoundedCornerShape(8.dp))
            .semantics { contentDescription = "蓝屏白壳桌宠" },
    ) {
        if (bsodSpritesheet != null) {
            drawBsodPet(bsodSpritesheet, mood)
        } else {
            drawPet(mood)
        }
    }
}

private fun DrawScope.drawBsodPet(spritesheet: Bitmap, mood: BotMood) {
    val side = min(size.width, size.height)
    val frame = mood.bsodFrame
    val source = AndroidRect(
        frame.column * BsodFrameWidth,
        frame.row * BsodFrameHeight,
        (frame.column + 1) * BsodFrameWidth,
        (frame.row + 1) * BsodFrameHeight,
    )
    val targetWidth = side * 0.76f
    val targetHeight = targetWidth * BsodFrameHeight / BsodFrameWidth
    val left = (size.width - targetWidth) / 2f
    val top = (size.height - targetHeight) / 2f - side * 0.02f
    val target = RectF(left, top, left + targetWidth, top + targetHeight)

    drawRoundRect(
        color = Color(0xFF020202),
        topLeft = Offset(size.width * 0.24f, top + targetHeight * 0.87f),
        size = Size(size.width * 0.52f, side * 0.050f),
        cornerRadius = CornerRadius(side * 0.06f, side * 0.06f),
    )
    drawContext.canvas.nativeCanvas.drawBitmap(
        spritesheet,
        source,
        target,
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
        },
    )
}

private fun DrawScope.drawPet(mood: BotMood) {
    val side = min(size.width, size.height)
    val w = size.width
    val h = size.height
    val unit = side / 100f
    val headLeft = w * 0.24f
    val headTop = h * 0.22f
    val headWidth = w * 0.52f
    val headHeight = h * 0.34f
    val screenLeft = headLeft + headWidth * 0.14f
    val screenTop = headTop + headHeight * 0.24f
    val screenWidth = headWidth * 0.72f
    val screenHeight = headHeight * 0.46f

    drawRoundRect(
        color = Color(0xFF020202),
        topLeft = Offset(w * 0.30f, h * 0.74f),
        size = Size(w * 0.40f, h * 0.045f),
        cornerRadius = CornerRadius(unit * 5f, unit * 5f),
    )
    drawLine(
        color = BotShellDark,
        start = Offset(w * 0.50f, h * 0.22f),
        end = Offset(w * 0.56f, h * 0.14f),
        strokeWidth = unit * 2f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
    drawCircle(StatusBlue, radius = unit * 3.2f, center = Offset(w * 0.57f, h * 0.13f))
    drawRoundRect(
        color = BotShell,
        topLeft = Offset(headLeft, headTop),
        size = Size(headWidth, headHeight),
        cornerRadius = CornerRadius(unit * 10f, unit * 10f),
    )
    drawRoundRect(
        color = BotShellDark,
        topLeft = Offset(headLeft, headTop),
        size = Size(headWidth, headHeight),
        cornerRadius = CornerRadius(unit * 10f, unit * 10f),
        style = Stroke(width = unit * 1.3f),
    )
    drawRoundRect(
        color = BotScreen,
        topLeft = Offset(screenLeft, screenTop),
        size = Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(unit * 6f, unit * 6f),
    )
    drawCircle(Color(0xFF03101E), radius = unit * 1.7f, center = Offset(screenLeft + screenWidth * 0.38f, screenTop + screenHeight * 0.45f))
    drawCircle(Color(0xFF03101E), radius = unit * 1.7f, center = Offset(screenLeft + screenWidth * 0.62f, screenTop + screenHeight * 0.45f))
    drawCircle(BotCheek, radius = unit * 1.7f, center = Offset(screenLeft + screenWidth * 0.30f, screenTop + screenHeight * 0.66f))
    drawCircle(BotCheek, radius = unit * 1.7f, center = Offset(screenLeft + screenWidth * 0.70f, screenTop + screenHeight * 0.66f))
    drawRoundRect(
        color = BotShell,
        topLeft = Offset(w * 0.39f, h * 0.56f),
        size = Size(w * 0.22f, h * 0.17f),
        cornerRadius = CornerRadius(unit * 6f, unit * 6f),
    )
    drawRoundRect(
        color = mood.accent,
        topLeft = Offset(w * 0.44f, h * 0.60f),
        size = Size(w * 0.12f, h * 0.045f),
        cornerRadius = CornerRadius(unit * 3.2f, unit * 3.2f),
    )
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
    val bsodFrame: SpriteFrame,
) {
    Happy(StatusGreen, StatusGreen, "忙", EyePattern.Bright, SpriteFrame(row = 3, column = 2)),
    Watch(StatusYellow, StatusYellow, "看", EyePattern.Watch, SpriteFrame(row = 0, column = 0)),
    Alert(StatusRed, StatusRed, "!!!", EyePattern.Alert, SpriteFrame(row = 5, column = 4)),
    Sleepy(Muted, Color(0xFF5C5C5C), "歇", EyePattern.Sleep, SpriteFrame(row = 0, column = 1)),
    Offline(StatusBlue, Color(0xFF4E6470), "等", EyePattern.Offline, SpriteFrame(row = 5, column = 1)),
}

private data class SpriteFrame(
    val row: Int,
    val column: Int,
)
