package com.codextraffic

import com.codextraffic.http.HttpTrafficClient
import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.TrafficPayloadParser
import com.codextraffic.model.TrafficSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull

class HttpTrafficRepository(
    private val httpTrafficClient: HttpTrafficClient,
    private val parser: TrafficPayloadParser = TrafficPayloadParser(),
) : TrafficRepository {
    override val connectionStatus: StateFlow<ConnectionStatus> = httpTrafficClient.connectionStatus

    override val snapshots: Flow<TrafficSnapshot> = httpTrafficClient.payloads.mapNotNull { payload ->
        runCatching { parser.parse(payload) }.getOrNull()
    }

    override fun start() {
        httpTrafficClient.start()
    }

    override fun stop() {
        httpTrafficClient.stop()
    }

    override fun requiredPermissions(): Array<String> = httpTrafficClient.requiredPermissions()

    override fun hasRequiredPermissions(): Boolean = httpTrafficClient.hasRequiredPermissions()
}
