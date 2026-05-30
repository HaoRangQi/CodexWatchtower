package com.codextraffic.ui

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.PetFeedItem
import com.codextraffic.model.ReasonCode
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficUiState

private const val OverlayAssetFile = "codex_bsod_spritesheet.webp"
private const val OverlayFrameWidth = 192
private const val OverlayFrameHeight = 208
private const val PrototypeViewportWidth = 356f
private const val PrototypeViewportHeight = 320f

@Composable
fun PetFeedScreen(
    uiState: TrafficUiState,
    mascotMotionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val feedItems = uiState.snapshot.feedItems.sortedWith(overlayFeedComparator)
    val mascotState = feedItems.overlayMascotState(uiState.snapshot.overall)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OverlayColors.Background)
            .padding(8.dp)
            .testTag("pet_feed_screen"),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = PrototypeViewportWidth.dp)
                .aspectRatio(PrototypeViewportWidth / PrototypeViewportHeight)
                .offset(y = (-48).dp)
                .testTag("avatar_overlay_content_frame"),
        ) {
            val trayWidth = maxWidth * (276f / PrototypeViewportWidth)
            val trayHeight = maxHeight * (131f / PrototypeViewportHeight)
            val trayLeft = maxWidth * (80f / PrototypeViewportWidth)
            val trayTop = maxHeight * (56f / PrototypeViewportHeight)
            val mascotWidth = maxWidth * (112f / PrototypeViewportWidth)
            val mascotHeight = maxHeight * (121f / PrototypeViewportHeight)
            val mascotLeft = maxWidth * (244f / PrototypeViewportWidth)
            val mascotTop = maxHeight * (191f / PrototypeViewportHeight)

            OverlayStage()
            NotificationTray(
                feedItems = feedItems,
                omittedFeedCount = uiState.snapshot.omittedFeedCount,
                connectionStatus = uiState.connectionStatus,
                modifier = Modifier
                    .width(trayWidth)
                    .height(trayHeight)
                    .align(Alignment.TopStart)
                    .offset(x = trayLeft, y = trayTop)
                    .testTag("avatar_notification_tray"),
            )
            FloatingMascot(
                state = mascotState,
                activeCount = feedItems.size,
                motionEnabled = mascotMotionEnabled,
                modifier = Modifier
                    .width(mascotWidth)
                    .height(mascotHeight)
                    .align(Alignment.TopStart)
                    .offset(x = mascotLeft, y = mascotTop)
                    .testTag("avatar_overlay_mascot"),
            )
        }
    }
}

@Composable
private fun OverlayStage() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("avatar_overlay_stage"),
    ) {
        // Intentionally empty: the source overlay has a transparent stage.
    }
}

@Composable
private fun NotificationTray(
    feedItems: List<PetFeedItem>,
    omittedFeedCount: Int,
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = 2.dp),
    ) {
        if (feedItems.isEmpty()) {
            EmptyOverlayFeed(connectionStatus = connectionStatus)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                items(
                    feedItems.take(3),
                    key = { "${it.projectId}_${it.reason.wireValue}_${it.ageSeconds}" },
                ) { item ->
                    OverlayNotificationRow(item = item)
                }
                if (omittedFeedCount > 0) {
                    item {
                        Text(
                            text = "+$omittedFeedCount 更早",
                            color = OverlayColors.Faint,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            letterSpacing = 0.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOverlayFeed(connectionStatus: ConnectionStatus) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight()
            .shadow(10.dp, OverlayShapes.NotificationCard)
            .background(OverlayColors.CardSurface, OverlayShapes.NotificationCard)
            .border(
                BorderStroke(1.dp, OverlayColors.CardBorder),
                OverlayShapes.NotificationCard,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = connectionStatus.emptyFeedLabel(),
            color = OverlayColors.Muted,
            fontSize = 13.sp,
            letterSpacing = 0.sp,
        )
    }
}

private fun ConnectionStatus.emptyFeedLabel(): String = when (this) {
    ConnectionStatus.Connected -> "暂无动态"
    ConnectionStatus.Scanning,
    ConnectionStatus.Connecting -> "正在连接 Mac"
    ConnectionStatus.PermissionMissing -> "需要蓝牙权限"
    ConnectionStatus.BluetoothOff -> "蓝牙未开启"
    ConnectionStatus.Error -> "连接异常"
    ConnectionStatus.Disconnected -> "未连接 Mac"
}

@Composable
private fun OverlayNotificationRow(item: PetFeedItem) {
    val isWaiting = item.reason == ReasonCode.Blocked || item.reason == ReasonCode.Stale
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, OverlayShapes.NotificationCard)
            .background(OverlayColors.CardSurface, OverlayShapes.NotificationCard)
            .border(
                BorderStroke(0.75.dp, if (isWaiting) OverlayColors.WaitingBorder else OverlayColors.CardBorder),
                OverlayShapes.NotificationCard,
            )
            .testTag("feed_${item.projectId}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 5.dp, end = 34.dp, bottom = 5.dp),
        ) {
            Text(
                text = item.overlayTitle(),
                color = OverlayColors.Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = item.body,
                color = OverlayColors.Muted,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                maxLines = if (isWaiting) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            if (isWaiting) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OverlayAction(item.primaryActionLabel(), OverlayColors.PrimaryButton)
                    OverlayAction("忽略", OverlayColors.SecondaryButton)
                }
            }
        }
        StatusGlyph(
            item = item,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 6.dp),
        )
    }
}

private fun PetFeedItem.overlayTitle(): String = when (reason) {
    ReasonCode.Blocked -> title.ifBlank { "等待你处理" }
    ReasonCode.Stale -> title.ifBlank { "任务可能卡住" }
    else -> title
}

private fun PetFeedItem.primaryActionLabel(): String = when (reason) {
    ReasonCode.Stale -> "查看"
    ReasonCode.Blocked -> "处理"
    else -> "打开"
}

@Composable
private fun StatusGlyph(
    item: PetFeedItem,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "feed_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.68f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = item.reason.overlayPulseMillis()),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "feed_pulse_alpha",
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                alpha = pulse
                scaleX = 0.92f + pulse * 0.08f
                scaleY = 0.92f + pulse * 0.08f
            }
            .background(Color.Transparent, CircleShape)
            .testTag("feed_pulse_${item.projectId}"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = item.light.overlayAccent().copy(alpha = 0.14f),
                radius = size.minDimension * 0.48f,
            )
            drawCircle(
                color = item.light.overlayAccent(),
                radius = size.minDimension * 0.22f,
            )
        }
    }
}

@Composable
private fun OverlayAction(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        color = OverlayColors.Ink,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .border(BorderStroke(1.dp, OverlayColors.ActionBorder), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        letterSpacing = 0.sp,
    )
}

@Composable
private fun FloatingMascot(
    state: OverlayAvatarState,
    activeCount: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("avatar_mascot_state_${state.wireName}"),
        ) {
            OverlayMascot(state, motionEnabled)
        }
        if (activeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = (-1).dp.roundToPx(), y = 4.dp.roundToPx()) }
                    .background(OverlayColors.Badge, CircleShape)
                    .border(BorderStroke(1.dp, OverlayColors.BadgeBorder), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeCount.toString(),
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

@Composable
private fun OverlayMascot(
    state: OverlayAvatarState,
    motionEnabled: Boolean,
) {
    val context = LocalContext.current
    val spritesheet = remember(context) {
        runCatching {
            context.assets.open(OverlayAssetFile).use(BitmapFactory::decodeStream)
        }.getOrNull()
    }
    val frame = rememberOverlaySpriteFrame(
        animation = state.animation,
        animationEnabled = motionEnabled,
        animationKey = state,
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (spritesheet == null) {
            drawOverlayFallbackMascot(state.accent)
        } else {
            val source = AndroidRect(
                frame.column * OverlayFrameWidth,
                frame.row * OverlayFrameHeight,
                (frame.column + 1) * OverlayFrameWidth,
                (frame.row + 1) * OverlayFrameHeight,
            )
            val width = size.width
            val height = width * OverlayFrameHeight / OverlayFrameWidth
            val left = (size.width - width) / 2f
            val top = (size.height - height) / 2f
            drawContext.canvas.nativeCanvas.drawBitmap(
                spritesheet,
                source,
                RectF(left, top, left + width, top + height),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    isDither = true
                },
            )
        }
    }
}

@Composable
private fun rememberOverlaySpriteFrame(
    animation: OverlaySpriteAnimation,
    animationEnabled: Boolean,
    animationKey: Any,
): OverlaySpriteFrame {
    var frame by remember(animationKey) { mutableStateOf(animation.firstFrame) }

    LaunchedEffect(animation, animationEnabled, animationKey) {
        frame = animation.firstFrame
        if (!animationEnabled || animation.frames.size == 1) {
            return@LaunchedEffect
        }

        var startedAt: Long? = null
        while (true) {
            val now = withInfiniteAnimationFrameMillis { it }
            if (startedAt == null) {
                startedAt = now
            }
            frame = animation.frameAt(now - startedAt)
        }
    }

    return frame
}

private fun DrawScope.drawOverlayFallbackMascot(accent: Color) {
    drawRoundRect(
        color = Color(0xFFECEFF4),
        topLeft = Offset(size.width * 0.24f, size.height * 0.28f),
        size = Size(size.width * 0.54f, size.height * 0.34f),
        cornerRadius = CornerRadius(size.width * 0.08f),
    )
    drawRoundRect(
        color = Color(0xFF198FE8),
        topLeft = Offset(size.width * 0.32f, size.height * 0.38f),
        size = Size(size.width * 0.38f, size.height * 0.13f),
        cornerRadius = CornerRadius(size.width * 0.05f),
    )
    drawCircle(Color.Black, radius = size.width * 0.018f, center = Offset(size.width * 0.45f, size.height * 0.44f))
    drawCircle(Color.Black, radius = size.width * 0.018f, center = Offset(size.width * 0.58f, size.height * 0.44f))
    drawRoundRect(
        color = accent,
        topLeft = Offset(size.width * 0.42f, size.height * 0.64f),
        size = Size(size.width * 0.18f, size.height * 0.06f),
        cornerRadius = CornerRadius(size.width * 0.03f),
    )
}

private val overlayFeedComparator = compareBy<PetFeedItem>(
    { it.overlayPriority() },
    { it.ageSeconds },
    { it.title.lowercase() },
)

private fun PetFeedItem.overlayPriority(): Int = when (reason) {
    ReasonCode.Stale,
    ReasonCode.Blocked,
    ReasonCode.CodexOff -> 0
    ReasonCode.Work -> 1
    ReasonCode.Recent -> 2
    ReasonCode.Idle -> 3
}

private fun ReasonCode.overlayPulseMillis(): Int = when (this) {
    ReasonCode.Work -> 680
    ReasonCode.Recent -> 980
    ReasonCode.Idle -> 1600
    ReasonCode.Stale,
    ReasonCode.Blocked,
    ReasonCode.CodexOff -> 540
}

private fun List<PetFeedItem>.overlayMascotState(overall: TrafficLight): OverlayAvatarState {
    val topReason = minByOrNull { it.overlayPriority() }?.reason
    return when {
        topReason == ReasonCode.Work -> OverlayAvatarState.Running
        topReason == ReasonCode.Recent -> OverlayAvatarState.Review
        topReason == ReasonCode.Stale || topReason == ReasonCode.Blocked || topReason == ReasonCode.CodexOff -> {
            OverlayAvatarState.Failed
        }
        overall == TrafficLight.Green -> OverlayAvatarState.Running
        overall == TrafficLight.Yellow -> OverlayAvatarState.Review
        else -> OverlayAvatarState.Idle
    }
}

private fun TrafficLight.overlayAccent() = when (this) {
    TrafficLight.Green -> OverlayColors.Green
    TrafficLight.Yellow -> OverlayColors.Yellow
    TrafficLight.Red -> OverlayColors.Danger
}

private enum class OverlayAvatarState(
    val wireName: String,
    val accent: Color,
    val animation: OverlaySpriteAnimation,
) {
    Idle("idle", OverlayColors.Muted, OverlayAvatarAnimations.Idle),
    Running("running", OverlayColors.Green, OverlayAvatarAnimations.Running),
    Review("review", OverlayColors.Green, OverlayAvatarAnimations.Review),
    Waiting("waiting", OverlayColors.Yellow, OverlayAvatarAnimations.Waiting),
    Failed("failed", OverlayColors.Danger, OverlayAvatarAnimations.Failed),
}

private data class OverlaySpriteFrame(
    val row: Int,
    val column: Int,
    val durationMs: Int,
)

private data class OverlaySpriteAnimation(
    val frames: List<OverlaySpriteFrame>,
    val loopStartIndex: Int,
) {
    val firstFrame: OverlaySpriteFrame = frames.first()

    fun frameAt(elapsedMs: Long): OverlaySpriteFrame {
        if (frames.size == 1) {
            return firstFrame
        }

        val introDuration = frames.take(loopStartIndex).sumOf { it.durationMs }
        val loopFrames = frames.drop(loopStartIndex).ifEmpty { frames }
        val loopDuration = loopFrames.sumOf { it.durationMs }.coerceAtLeast(1)
        val position = if (elapsedMs < introDuration) {
            elapsedMs.toInt()
        } else {
            introDuration + ((elapsedMs - introDuration) % loopDuration).toInt()
        }

        var cursor = 0
        for (candidate in frames.take(loopStartIndex) + loopFrames) {
            cursor += candidate.durationMs
            if (position < cursor) {
                return candidate
            }
        }
        return loopFrames.last()
    }
}

private object OverlayAvatarAnimations {
    private const val IdleSlowdown = 6
    private val idleBase = listOf(
        OverlaySpriteFrame(row = 0, column = 0, durationMs = 280 * IdleSlowdown),
        OverlaySpriteFrame(row = 0, column = 1, durationMs = 110 * IdleSlowdown),
        OverlaySpriteFrame(row = 0, column = 2, durationMs = 110 * IdleSlowdown),
        OverlaySpriteFrame(row = 0, column = 3, durationMs = 140 * IdleSlowdown),
        OverlaySpriteFrame(row = 0, column = 4, durationMs = 140 * IdleSlowdown),
        OverlaySpriteFrame(row = 0, column = 5, durationMs = 320 * IdleSlowdown),
    )

    val Idle = OverlaySpriteAnimation(frames = idleBase, loopStartIndex = 0)
    val Running = action(row = 7, count = 6, durationMs = 120, lastDurationMs = 220)
    val Review = action(row = 8, count = 6, durationMs = 150, lastDurationMs = 280)
    val Waiting = action(row = 6, count = 6, durationMs = 150, lastDurationMs = 260)
    val Failed = action(row = 5, count = 8, durationMs = 140, lastDurationMs = 240)

    private fun action(
        row: Int,
        count: Int,
        durationMs: Int,
        lastDurationMs: Int,
    ): OverlaySpriteAnimation {
        val actionFrames = List(count) { column ->
            OverlaySpriteFrame(
                row = row,
                column = column,
                durationMs = if (column == count - 1) lastDurationMs else durationMs,
            )
        }
        val intro = actionFrames + actionFrames + actionFrames
        return OverlaySpriteAnimation(
            frames = intro + idleBase,
            loopStartIndex = intro.size,
        )
    }
}

private object OverlayColors {
    val Background = Color.Black
    val CardSurface = Color(0xF21E1F22)
    val CardBorder = Color(0x52D7D2C7)
    val WaitingBorder = Color(0x6EF7D36C)
    val Ink = Color(0xFFF0EEE6)
    val Muted = Color(0xFFA7A39A)
    val Faint = Color(0xFF6B6761)
    val PrimaryButton = Color(0x333D8BFF)
    val SecondaryButton = Color(0x1CF0EEE6)
    val ActionBorder = Color(0x38F7F3EA)
    val Badge = Color(0xFFF8F4EC)
    val BadgeBorder = Color(0xB0000000)
    val Green = Color(0xFF37D67A)
    val Yellow = Color(0xFFE9C846)
    val Danger = Color(0xFFE34A4A)
}

private object OverlayShapes {
    val NotificationCard = RoundedCornerShape(18.dp)
}
