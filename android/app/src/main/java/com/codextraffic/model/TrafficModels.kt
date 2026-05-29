package com.codextraffic.model

enum class TrafficLight(val code: String) {
    Green("g"),
    Yellow("y"),
    Red("r");

    companion object {
        fun fromCode(code: String): TrafficLight = when (code) {
            "g" -> Green
            "y" -> Yellow
            "r" -> Red
            else -> Red
        }
    }
}

enum class ReasonCode(val wireValue: String) {
    Work("work"),
    Recent("recent"),
    Idle("idle"),
    Stale("stale"),
    Blocked("blocked"),
    CodexOff("codex_off");

    companion object {
        fun fromWireValue(value: String): ReasonCode = entries.firstOrNull { it.wireValue == value } ?: Idle
    }
}

data class ProjectTraffic(
    val id: String,
    val name: String,
    val light: TrafficLight,
    val ageSeconds: Long,
    val reason: ReasonCode,
)

data class TrafficSnapshot(
    val version: Int,
    val timestampSeconds: Long,
    val overall: TrafficLight,
    val projects: List<ProjectTraffic>,
    val omittedCount: Int,
) {
    companion object {
        val empty = TrafficSnapshot(
            version = 1,
            timestampSeconds = 0L,
            overall = TrafficLight.Red,
            projects = emptyList(),
            omittedCount = 0,
        )
    }
}

enum class ConnectionStatus {
    Disconnected,
    Scanning,
    Connecting,
    Connected,
    PermissionMissing,
    BluetoothOff,
    Error,
}

data class TrafficUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val snapshot: TrafficSnapshot = TrafficSnapshot.empty,
    val errorMessage: String? = null,
)

