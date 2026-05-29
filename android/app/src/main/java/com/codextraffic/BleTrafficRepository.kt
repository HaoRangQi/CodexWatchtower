package com.codextraffic

import com.codextraffic.ble.BleTrafficClient
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.TrafficPayloadParser
import com.codextraffic.model.TrafficSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull

class BleTrafficRepository(
    private val bleTrafficClient: BleTrafficClient,
    private val parser: TrafficPayloadParser = TrafficPayloadParser(),
) : TrafficRepository {
    override val connectionStatus: StateFlow<ConnectionStatus> = bleTrafficClient.connectionStatus

    override val snapshots: Flow<TrafficSnapshot> = bleTrafficClient.payloads.mapNotNull { payload ->
        runCatching { parser.parse(payload) }.getOrNull()
    }

    override fun start() {
        bleTrafficClient.start()
    }

    override fun stop() {
        bleTrafficClient.stop()
    }

    override fun requiredPermissions(): Array<String> = bleTrafficClient.requiredPermissions()

    override fun hasRequiredPermissions(): Boolean = bleTrafficClient.hasRequiredPermissions()
}

