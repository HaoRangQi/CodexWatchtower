package com.codextraffic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.codextraffic.model.TrafficLight
import com.codextraffic.model.TrafficUiState

private val Ink = Color(0xFFE7E1CC)
private val Panel = Color(0xFF202020)
private val PanelDark = Color(0xFF141414)
private val GridLine = Color(0xFF333333)
private val PixelGreen = Color(0xFF36D66B)
private val PixelYellow = Color(0xFFE9C846)
private val PixelRed = Color(0xFFE34A4A)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelDark)
            .padding(20.dp),
    ) {
        Header(uiState.connectionStatus)
        Spacer(Modifier.height(18.dp))
        OverallTrafficLight(uiState.snapshot.overall)
        Spacer(Modifier.height(20.dp))
        ProjectList(uiState)
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
            text = "CODEX 红绿灯",
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
        )
        Text(
            modifier = Modifier.testTag("connection_status"),
            text = connectionStatus.label(),
            color = if (connectionStatus == ConnectionStatus.Connected) PixelGreen else Muted,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun OverallTrafficLight(overall: TrafficLight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(4.dp, GridLine)
            .background(Panel)
            .padding(16.dp)
            .testTag("overall_light"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLamp(
            light = TrafficLight.Red,
            active = overall == TrafficLight.Red,
            squareSize = 72,
        )
        Spacer(Modifier.height(10.dp))
        PixelLamp(
            light = TrafficLight.Yellow,
            active = overall == TrafficLight.Yellow,
            squareSize = 72,
        )
        Spacer(Modifier.height(10.dp))
        PixelLamp(
            light = TrafficLight.Green,
            active = overall == TrafficLight.Green,
            squareSize = 72,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            modifier = Modifier.testTag("overall_label"),
            text = overall.label(),
            color = overall.color(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun PixelLamp(
    light: TrafficLight,
    active: Boolean,
    squareSize: Int,
) {
    val base = if (active) light.color() else Color(0xFF2B2B2B)
    Box(
        modifier = Modifier
            .size(squareSize.dp)
            .clipToBounds()
            .background(Color(0xFF0B0B0B))
            .border(4.dp, Color(0xFF464646)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((squareSize - 22).dp)
                .background(base.copy(alpha = if (active) 1f else 0.55f), RectangleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .size(18.dp)
                .background(Color.White.copy(alpha = if (active) 0.22f else 0.04f), RectangleShape),
        )
    }
}

@Composable
private fun ProjectList(uiState: TrafficUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, GridLine)
            .background(Panel)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("项目", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (uiState.snapshot.omittedCount > 0) {
                Text("+${uiState.snapshot.omittedCount}", color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (uiState.snapshot.projects.isEmpty()) {
            Text(
                modifier = Modifier.testTag("empty_projects"),
                text = "暂无项目信号",
                color = Muted,
                fontSize = 14.sp,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.snapshot.projects, key = { it.id }) { project ->
                    ProjectRow(project)
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: ProjectTraffic) {
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
                .size(18.dp)
                .background(project.light.color())
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
                text = "${project.ageSeconds} 秒 / ${project.reason.label()}",
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = project.light.shortLabel(),
            color = project.light.color(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.sp,
        )
    }
}

private fun TrafficLight.color(): Color = when (this) {
    TrafficLight.Green -> PixelGreen
    TrafficLight.Yellow -> PixelYellow
    TrafficLight.Red -> PixelRed
}

private fun TrafficLight.label(): String = when (this) {
    TrafficLight.Green -> "工作中"
    TrafficLight.Yellow -> "近期活跃"
    TrafficLight.Red -> "空闲"
}

private fun TrafficLight.shortLabel(): String = when (this) {
    TrafficLight.Green -> "绿"
    TrafficLight.Yellow -> "黄"
    TrafficLight.Red -> "红"
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
