package com.codextraffic

import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.TrafficSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HybridTrafficRepository(
    private val primary: TrafficRepository,
    private val fallback: TrafficRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : TrafficRepository {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    private val _snapshots = MutableSharedFlow<TrafficSnapshot>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var started = false
    private var relayJobs: List<Job> = emptyList()

    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    override val snapshots: Flow<TrafficSnapshot> = _snapshots

    override fun start() {
        if (started) {
            return
        }

        started = true
        startRelayJobs()
        primary.start()
        fallback.start()
    }

    override fun stop() {
        started = false
        relayJobs.forEach { it.cancel() }
        relayJobs = emptyList()
        primary.stop()
        fallback.stop()
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    override fun requiredPermissions(): Array<String> = (
        primary.requiredPermissions().toList() + fallback.requiredPermissions().toList()
        ).distinct().toTypedArray()

    override fun hasRequiredPermissions(): Boolean =
        primary.hasRequiredPermissions() && fallback.hasRequiredPermissions()

    private fun startRelayJobs() {
        if (relayJobs.isNotEmpty()) {
            return
        }
        relayJobs = listOf(
            scope.launch {
                combine(primary.connectionStatus, fallback.connectionStatus, ::mergeConnectionStatus)
                    .collect { _connectionStatus.value = it }
            },
            scope.launch {
                primary.snapshots.collect { _snapshots.emit(it) }
            },
            scope.launch {
                fallback.snapshots.collect { _snapshots.emit(it) }
            },
        )
    }
}

private fun mergeConnectionStatus(
    primary: ConnectionStatus,
    fallback: ConnectionStatus,
): ConnectionStatus = when {
    primary == ConnectionStatus.Connected || fallback == ConnectionStatus.Connected -> ConnectionStatus.Connected
    primary == ConnectionStatus.Connecting || fallback == ConnectionStatus.Connecting -> ConnectionStatus.Connecting
    primary == ConnectionStatus.Scanning || fallback == ConnectionStatus.Scanning -> ConnectionStatus.Scanning
    primary == ConnectionStatus.PermissionMissing -> fallback
    primary == ConnectionStatus.BluetoothOff -> fallback
    primary == ConnectionStatus.Error && fallback != ConnectionStatus.Error -> fallback
    fallback == ConnectionStatus.PermissionMissing -> primary
    fallback == ConnectionStatus.BluetoothOff -> primary
    else -> primary
}
