package com.codextraffic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
private val Dim = Color(0xFF303030)
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
            TrafficScreen(
                uiState = uiState,
                onHideProject = viewModel::hideProject,
                onRestoreProject = viewModel::restoreProject,
            )
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
fun TrafficScreen(
    uiState: TrafficUiState,
    petMotionEnabled: Boolean = true,
    onHideProject: (ProjectTraffic) -> Unit = {},
    onRestoreProject: (ProjectTraffic) -> Unit = {},
) {
    val summary = uiState.summary()
    val sortedProjects = uiState.snapshot.projects.sortedWith(projectComparator)
    val hiddenProjects = uiState.hiddenProjects.sortedWith(projectComparator)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConnectionPill(uiState.connectionStatus)
        Spacer(Modifier.height(4.dp))
        BotStatusPanel(
            summary = summary,
            petMotionEnabled = petMotionEnabled,
        )
        Spacer(Modifier.height(8.dp))
        ProjectList(
            projects = sortedProjects,
            omittedCount = uiState.snapshot.omittedCount,
            hiddenProjects = hiddenProjects,
            onHideProject = onHideProject,
            onRestoreProject = onRestoreProject,
        )
    }
}

@Composable
private fun ConnectionPill(connectionStatus: ConnectionStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(connectionStatus.accent(), CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 3.dp)
                .testTag("connection_status"),
            text = connectionStatus.label(),
            color = connectionStatus.accent(),
            fontSize = 11.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun BotStatusPanel(
    summary: BotSummary,
    petMotionEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .testTag("bot_panel"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PetBot(
            mood = summary.mood,
            motionEnabled = petMotionEnabled,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 360.dp)
                .aspectRatio(1f)
                .testTag("pet_bot"),
        )
        Spacer(Modifier.height(2.dp))
        StatusBubble(
            summary = summary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PetBot(
    mood: BotMood,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val motion = if (motionEnabled) rememberPetMotion(mood) else PetMotion.Still
    val bsodSpritesheet = remember(context) {
        runCatching {
            context.assets.open(CodexBsodAssetFile).use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .graphicsLayer {
                translationX = motion.swayPx
                translationY = motion.bobPx
                rotationZ = motion.rotationDegrees
                scaleX = motion.scale
                scaleY = motion.scale
            }
            .semantics { contentDescription = "蓝屏白壳桌宠" },
    ) {
        if (bsodSpritesheet != null) {
            drawBsodPet(bsodSpritesheet, mood)
        } else {
            drawPet(mood)
        }
    }
}

@Composable
private fun rememberPetMotion(mood: BotMood): PetMotion {
    val transition = rememberInfiniteTransition(label = "pet_motion")
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = mood.motionMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet_bob",
    )
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = mood.motionMillis + 450),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet_sway",
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet_breath",
    )
    return PetMotion(
        bobPx = bob * mood.bobPx,
        swayPx = sway * mood.swayPx,
        rotationDegrees = sway * mood.rotationDegrees,
        scale = 1f + blink * mood.scalePulse,
    )
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
    val targetWidth = side * 0.84f
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
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.testTag("bot_summary"),
            text = summary.title,
            color = summary.mood.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 0.sp,
        )
        if (summary.detail.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = summary.detail,
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<ProjectTraffic>,
    omittedCount: Int,
    hiddenProjects: List<ProjectTraffic>,
    onHideProject: (ProjectTraffic) -> Unit,
    onRestoreProject: (ProjectTraffic) -> Unit,
) {
    var showHiddenProjects by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 4.dp)
            .testTag("project_list"),
    ) {
        if (projects.isEmpty()) {
            Text(
                modifier = Modifier.testTag("empty_projects"),
                text = "暂无项目",
                color = Muted,
                fontSize = 12.sp,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectRow(
                        project = project,
                        actionText = "隐藏",
                        onAction = { onHideProject(project) },
                    )
                }
                if (omittedCount > 0) {
                    item {
                        Text(
                            text = "+$omittedCount",
                            color = Dim,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
                        )
                    }
                }
                if (hiddenProjects.isNotEmpty()) {
                    item {
                        HiddenProjectsToggle(
                            count = hiddenProjects.size,
                            expanded = showHiddenProjects,
                            onClick = { showHiddenProjects = !showHiddenProjects },
                        )
                    }
                    if (showHiddenProjects) {
                        items(hiddenProjects, key = { "hidden_${it.id}" }) { project ->
                            ProjectRow(
                                project = project,
                                actionText = "恢复",
                                onAction = { onRestoreProject(project) },
                                muted = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenProjectsToggle(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Text(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 7.dp)
            .clickable(onClick = onClick)
            .testTag("hidden_projects_toggle"),
        text = if (expanded) "已隐藏 $count 项 · 收起" else "已隐藏 $count 项 · 点此恢复",
        color = Dim,
        fontSize = 11.sp,
        letterSpacing = 0.sp,
    )
}

@Composable
private fun ProjectRow(
    project: ProjectTraffic,
    actionText: String,
    onAction: () -> Unit,
    muted: Boolean = false,
) {
    val status = project.statusLabel()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelDark)
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .testTag("project_${project.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(status.color.copy(alpha = if (muted) 0.48f else 1f), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = project.name,
                color = if (muted) Muted else Ink,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = "${project.ageSeconds} 秒",
            color = Muted,
            fontSize = 11.sp,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.text,
            color = status.color.copy(alpha = if (muted) 0.55f else 1f),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            modifier = Modifier
                .clickable(onClick = onAction)
                .padding(horizontal = 2.dp, vertical = 3.dp)
                .testTag("${actionText}_${project.id}"),
            text = actionText,
            color = if (muted) StatusGreen else Dim,
            fontSize = 11.sp,
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
            detail = connectionStatus.actionText(),
        )
    }

    val projects = snapshot.projects
    if (projects.isEmpty()) {
        return BotSummary(
            mood = BotMood.Sleepy,
            title = "待机中",
            detail = "暂无项目",
        )
    }

    val attention = projects.count { it.needsAttention() }
    val working = projects.count { it.reason == ReasonCode.Work }
    val recent = projects.count { it.reason == ReasonCode.Recent }

    return when {
        attention > 0 -> BotSummary(
            mood = BotMood.Alert,
            title = "$attention 个项目需要看一眼",
            detail = "列表顶部优先处理",
        )

        working > 0 -> BotSummary(
            mood = BotMood.Happy,
            title = "$working 个项目正在推进",
            detail = "",
        )

        recent > 0 -> BotSummary(
            mood = BotMood.Watch,
            title = "刚有动静",
            detail = "当前没新进展",
        )

        else -> BotSummary(
            mood = BotMood.Sleepy,
            title = "现在比较安静",
            detail = "没有推进信号",
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
    ReasonCode.Recent -> ProjectStatusLabel("刚动过", StatusYellow)
    ReasonCode.Idle -> ProjectStatusLabel("空闲", Muted)
    ReasonCode.Stale -> ProjectStatusLabel("卡住?", StatusRed)
    ReasonCode.Blocked -> ProjectStatusLabel("阻塞", StatusRed)
    ReasonCode.CodexOff -> ProjectStatusLabel("离线", StatusRed)
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
)

private data class PetMotion(
    val bobPx: Float,
    val swayPx: Float,
    val rotationDegrees: Float,
    val scale: Float,
) {
    companion object {
        val Still = PetMotion(
            bobPx = 0f,
            swayPx = 0f,
            rotationDegrees = 0f,
            scale = 1f,
        )
    }
}

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
    val motionMillis: Int,
    val bobPx: Float,
    val swayPx: Float,
    val rotationDegrees: Float,
    val scalePulse: Float,
) {
    Happy(StatusGreen, StatusGreen, "忙", EyePattern.Bright, SpriteFrame(row = 3, column = 2), 1250, 5f, 2.4f, 1.3f, 0.010f),
    Watch(StatusYellow, StatusYellow, "看", EyePattern.Watch, SpriteFrame(row = 0, column = 0), 1650, 3.8f, 1.8f, 0.9f, 0.007f),
    Alert(StatusRed, StatusRed, "!!!", EyePattern.Alert, SpriteFrame(row = 5, column = 4), 900, 6f, 3f, 1.8f, 0.012f),
    Sleepy(Muted, Color(0xFF5C5C5C), "歇", EyePattern.Sleep, SpriteFrame(row = 0, column = 1), 2200, 2.5f, 0.8f, 0.4f, 0.004f),
    Offline(StatusBlue, Color(0xFF4E6470), "等", EyePattern.Offline, SpriteFrame(row = 5, column = 1), 1900, 3f, 1.2f, 0.6f, 0.005f),
}

private data class SpriteFrame(
    val row: Int,
    val column: Int,
)
