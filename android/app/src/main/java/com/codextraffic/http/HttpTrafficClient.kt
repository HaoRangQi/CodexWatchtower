package com.codextraffic.http

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.codextraffic.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class HttpTrafficClient(
    context: Context,
    private val pollIntervalMs: Long = 2_000L,
) {
    private companion object {
        const val TAG = "CodexTrafficHttp"
        const val SERVICE_TYPE = "_codextraffic._tcp."
        const val STATUS_PATH = "/status"
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var activeEndpoint: HttpEndpoint? = null
    private var pollScheduled = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _payloads = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val payloads: SharedFlow<String> = _payloads

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        _connectionStatus.value = ConnectionStatus.Scanning
        startDiscovery()
    }

    fun stop() {
        running.set(false)
        mainHandler.removeCallbacksAndMessages(null)
        pollScheduled = false
        activeEndpoint = null
        stopDiscovery()
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun hasRequiredPermissions(): Boolean = true

    fun requiredPermissions(): Array<String> = emptyArray()

    private fun startDiscovery() {
        stopDiscovery()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "HTTP discovery started")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!running.get() || serviceInfo.serviceType != SERVICE_TYPE) {
                    return
                }

                Log.d(TAG, "Found HTTP service ${serviceInfo.serviceName}")
                _connectionStatus.value = ConnectionStatus.Connecting
                resolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Lost HTTP service ${serviceInfo.serviceName}")
                activeEndpoint = null
                if (running.get()) {
                    _connectionStatus.value = ConnectionStatus.Scanning
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "HTTP discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "HTTP discovery start failed: $errorCode")
                _connectionStatus.value = ConnectionStatus.Error
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "HTTP discovery stop failed: $errorCode")
                stopDiscovery()
            }
        }

        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun stopDiscovery() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        runCatching {
            nsdManager.stopServiceDiscovery(listener)
        }.onFailure { error ->
            Log.d(TAG, "Ignoring discovery stop failure: ${error.message}")
        }
    }

    private fun resolve(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val host = resolvedService.host?.hostAddress ?: return
                val port = resolvedService.port
                if (port <= 0 || !running.get()) {
                    return
                }

                activeEndpoint = HttpEndpoint(host = host, port = port)
                _connectionStatus.value = ConnectionStatus.Connected
                schedulePoll(immediate = true)
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "HTTP service resolve failed: $errorCode")
                if (running.get()) {
                    _connectionStatus.value = ConnectionStatus.Scanning
                }
            }
        }

        runCatching {
            nsdManager.resolveService(serviceInfo, resolveListener)
        }.onFailure { error ->
            Log.w(TAG, "HTTP service resolve threw", error)
            if (running.get()) {
                _connectionStatus.value = ConnectionStatus.Scanning
            }
        }
    }

    private fun schedulePoll(immediate: Boolean = false) {
        if (!running.get() || pollScheduled) {
            return
        }

        pollScheduled = true
        mainHandler.postDelayed({
            pollScheduled = false
            pollOnce()
        }, if (immediate) 0L else pollIntervalMs)
    }

    private fun pollOnce() {
        val endpoint = activeEndpoint
        if (!running.get() || endpoint == null) {
            if (running.get()) {
                _connectionStatus.value = ConnectionStatus.Scanning
            }
            return
        }

        scope.launch {
            val payload = runCatching { endpoint.fetchStatus() }
                .onFailure { error ->
                    Log.w(TAG, "HTTP status fetch failed: ${error.message}")
                }
                .getOrNull()

            if (!running.get()) {
                return@launch
            }

            if (payload == null) {
                activeEndpoint = null
                _connectionStatus.value = ConnectionStatus.Scanning
                mainHandler.post { schedulePoll(immediate = false) }
                return@launch
            }

            _payloads.emit(payload)
            _connectionStatus.value = ConnectionStatus.Connected
            mainHandler.post { schedulePoll(immediate = false) }
        }
    }
}

private data class HttpEndpoint(
    val host: String,
    val port: Int,
) {
    suspend fun fetchStatus(): String = withContext(Dispatchers.IO) {
        val connection = (URL("http://$host:$port/status").openConnection() as HttpURLConnection).apply {
            connectTimeout = 1_500
            readTimeout = 1_500
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode}")
            }

            BufferedReader(
                InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)
            ).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
