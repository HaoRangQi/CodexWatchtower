package com.codextraffic

import com.codextraffic.model.ConnectionStatus
import com.codextraffic.model.TrafficSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TrafficRepository {
    val connectionStatus: StateFlow<ConnectionStatus>
    val snapshots: Flow<TrafficSnapshot>

    fun start()

    fun stop()

    fun requiredPermissions(): Array<String>

    fun hasRequiredPermissions(): Boolean
}

