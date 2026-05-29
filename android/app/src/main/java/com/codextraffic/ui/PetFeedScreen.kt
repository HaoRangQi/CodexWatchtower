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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
            .padding(10.dp)
            .testTag("pet_feed_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = PrototypeViewportWidth.dp)
                .aspectRatio(PrototypeViewportWidth / PrototypeViewportHeight)
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

            OverlayNoise()
            NotificationTray(
                feedItems = feedItems,
                omittedFeedCount = uiState.snapshot.omittedFeedCount,
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
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 2.dp, bottom = 4.dp),
                text = "‹",
                color = OverlayColors.Faint,
                fontSize = 20.sp,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
private fun OverlayNoise() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("avatar_overlay_stage"),
    ) {
        val gap = 18.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = OverlayColors.Grid,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.6f,
            )
            x += gap
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = OverlayColors.Grid,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.6f,
            )
            y += gap
        }
    }
}

@Composable
private fun NotificationTray(
    feedItems: List<PetFeedItem>,
    omittedFeedCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(18.dp))
            .background(OverlayColors.Tray, RoundedCornerShape(18.dp))
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最新",
                color = OverlayColors.Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (feedItems.isEmpty()) "0" else "${feedItems.size}",
                color = OverlayColors.Muted,
                fontSize = 10.sp,
                letterSpacing = 0.sp,
            )
        }

        if (feedItems.isEmpty()) {
            EmptyOverlayFeed()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(feedItems, key = { "${it.projectId}_${it.reason.wireValue}_${it.ageSeconds}" }) { item ->
                    OverlayNotificationRow(item)
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
private fun EmptyOverlayFeed() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "暂无动态",
            color = OverlayColors.Muted,
            fontSize = 13.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun OverlayNotificationRow(item: PetFeedItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(OverlayColors.Row, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag("feed_${item.projectId}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusGlyph(item)
            Spacer(Modifier.width(7.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = item.title,
                color = OverlayColors.Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
            Text(
                text = item.overlayAge(),
                color = OverlayColors.Faint,
                fontSize = 9.sp,
                letterSpacing = 0.sp,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.body,
            color = OverlayColors.Muted,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp,
        )
        if (item.reason == ReasonCode.Blocked || item.reason == ReasonCode.Stale) {
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OverlayAction("查看", OverlayColors.Button)
                OverlayAction("忽略", OverlayColors.Danger.copy(alpha = 0.18f))
            }
        }
    }
}

@Composable
private fun StatusGlyph(item: PetFeedItem) {
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
        modifier = Modifier
            .size(17.dp)
            .graphicsLayer {
                alpha = pulse
                scaleX = 0.92f + pulse * 0.08f
                scaleY = 0.92f + pulse * 0.08f
            }
            .background(item.light.overlayAccent().copy(alpha = 0.16f), CircleShape)
            .testTag("feed_pulse_${item.projectId}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.reason.overlayIcon(),
            color = item.light.overlayAccent(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
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
        fontSize = 10.sp,
        modifier = Modifier
            .background(color, RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
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
                    .offset { IntOffset(x = (-2).dp.roundToPx(), y = 2.dp.roundToPx()) }
                    .background(state.accent, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
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
            val width = size.width * 0.82f
            val height = width * OverlayFrameHeight / OverlayFrameWidth
            val left = size.width - width
            val top = (size.height - height) * 0.54f
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

private fun PetFeedItem.overlayAge(): String = when {
    ageSeconds < 60 -> "${ageSeconds} 秒"
    ageSeconds < 3600 -> "${ageSeconds / 60} 分"
    else -> "${ageSeconds / 3600} 时"
}

private fun ReasonCode.overlayIcon(): String = when (this) {
    ReasonCode.Work -> "●"
    ReasonCode.Recent -> "◐"
    ReasonCode.Idle -> "·"
    ReasonCode.Stale -> "!"
    ReasonCode.Blocked -> "?"
    ReasonCode.CodexOff -> "×"
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
        topReason == ReasonCode.Recent -> OverlayAvatarState.Waiting
        topReason == ReasonCode.Stale || topReason == ReasonCode.Blocked || topReason == ReasonCode.CodexOff -> {
            OverlayAvatarState.Failed
        }
        overall == TrafficLight.Green -> OverlayAvatarState.Running
        overall == TrafficLight.Yellow -> OverlayAvatarState.Waiting
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
    val Grid = Color(0x111B5C7A)
    val Tray = Color(0xF20B0D10)
    val Row = Color(0xFF15171B)
    val Ink = Color(0xFFEAE7DE)
    val Muted = Color(0xFF9A9A9A)
    val Faint = Color(0xFF5C5C5C)
    val Button = Color(0x222F81F7)
    val Green = Color(0xFF37D67A)
    val Yellow = Color(0xFFE9C846)
    val Danger = Color(0xFFE34A4A)
}
