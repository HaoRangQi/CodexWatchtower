package com.codextraffic.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.codextraffic.model.ConnectionStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.StandardCharsets

class BleTrafficClient(private val context: Context) {
    private companion object {
        const val TAG = "CodexTrafficBle"
        const val FILTERED_SCAN_FALLBACK_MS = 5_000L
        const val UNFILTERED_SCAN_RETRY_MS = 10_000L
        const val RECONNECT_DELAY_MS = 1_000L
        const val READ_POLL_INTERVAL_MS = 2_000L
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanHandler = Handler(Looper.getMainLooper())

    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var statusCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanningWithoutFilters = false
    private var shouldRun = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _payloads = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val payloads: SharedFlow<String> = _payloads

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasRequiredPermissions()) {
            _connectionStatus.value = ConnectionStatus.PermissionMissing
            return
        }

        val bluetoothAdapter = adapter
        if (bluetoothAdapter?.isEnabled != true) {
            _connectionStatus.value = ConnectionStatus.BluetoothOff
            return
        }

        shouldRun = true
        scanner = bluetoothAdapter.bluetoothLeScanner
        _connectionStatus.value = ConnectionStatus.Scanning

        startFilteredScan()
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        shouldRun = false
        scanHandler.removeCallbacksAndMessages(null)
        scanner?.stopScan(scanCallback)
        scanner = null
        statusCharacteristic = null
        gatt?.close()
        gatt = null
        isScanningWithoutFilters = false
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun hasRequiredPermissions(): Boolean {
        val permissions = requiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun serviceFilters(): List<ScanFilter> = listOf(
        ScanFilter.Builder()
            .setServiceUuid(BleConstants.SERVICE_PARCEL_UUID)
            .build()
    )

    private fun scanSettings(): ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    @SuppressLint("MissingPermission")
    private fun startFilteredScan() {
        val activeScanner = scanner ?: return
        scanHandler.removeCallbacksAndMessages(null)
        isScanningWithoutFilters = false

        Log.d(TAG, "Starting filtered BLE scan")
        activeScanner.stopScan(scanCallback)
        activeScanner.startScan(serviceFilters(), scanSettings(), scanCallback)
        scanHandler.postDelayed(::fallbackToUnfilteredScan, FILTERED_SCAN_FALLBACK_MS)
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToUnfilteredScan() {
        val activeScanner = scanner ?: return
        if (_connectionStatus.value != ConnectionStatus.Scanning || isScanningWithoutFilters) {
            return
        }

        Log.d(TAG, "No filtered scan result; switching to unfiltered BLE scan")
        activeScanner.stopScan(scanCallback)
        isScanningWithoutFilters = true
        activeScanner.startScan(null, scanSettings(), scanCallback)
        scanHandler.postDelayed(::restartScanCycle, UNFILTERED_SCAN_RETRY_MS)
    }

    private fun restartScanCycle() {
        if (!shouldRun || _connectionStatus.value != ConnectionStatus.Scanning || scanner == null) {
            return
        }

        Log.d(TAG, "No unfiltered scan result; restarting BLE scan cycle")
        startFilteredScan()
    }

    private fun scheduleReconnect() {
        if (!shouldRun) {
            _connectionStatus.value = ConnectionStatus.Disconnected
            return
        }

        _connectionStatus.value = ConnectionStatus.Scanning
        scanHandler.removeCallbacksAndMessages(null)
        scanHandler.postDelayed({
            if (!shouldRun) {
                return@postDelayed
            }

            scanner = adapter?.bluetoothLeScanner
            startFilteredScan()
        }, RECONNECT_DELAY_MS)
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!matchesCodexTraffic(result)) {
                return
            }

            Log.d(TAG, "Found Codex Traffic BLE peripheral")
            _connectionStatus.value = ConnectionStatus.Connecting
            scanHandler.removeCallbacksAndMessages(null)
            scanner?.stopScan(this)
            scanner = null
            gatt = result.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            _connectionStatus.value = ConnectionStatus.Error
        }
    }

    @SuppressLint("MissingPermission")
    private fun matchesCodexTraffic(result: ScanResult): Boolean {
        val scanRecord = result.scanRecord
        val advertisedName = scanRecord?.deviceName
        val bondedName = result.device.name
        val serviceUuids = scanRecord?.serviceUuids.orEmpty()

        return advertisedName == BleConstants.DEVICE_NAME ||
            bondedName == BleConstants.DEVICE_NAME ||
            serviceUuids.contains(BleConstants.SERVICE_PARCEL_UUID)
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "GATT connection state failed: status=$status newState=$newState")
                statusCharacteristic = null
                gatt.close()
                if (this@BleTrafficClient.gatt == gatt) {
                    this@BleTrafficClient.gatt = null
                }
                scheduleReconnect()
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT connected")
                    _connectionStatus.value = ConnectionStatus.Connected
                    gatt.requestMtu(517)
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT disconnected")
                    statusCharacteristic = null
                    gatt.close()
                    if (this@BleTrafficClient.gatt == gatt) {
                        this@BleTrafficClient.gatt = null
                    }
                    scheduleReconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "GATT service discovery failed: $status")
                _connectionStatus.value = ConnectionStatus.Error
                return
            }

            val characteristic = gatt
                .getService(BleConstants.SERVICE_UUID)
                ?.getCharacteristic(BleConstants.STATUS_CHARACTERISTIC_UUID)

            if (characteristic == null) {
                Log.w(TAG, "Status characteristic not found")
                _connectionStatus.value = ConnectionStatus.Error
                return
            }

            Log.d(TAG, "Status characteristic discovered")
            statusCharacteristic = characteristic
            gatt.readCharacteristic(characteristic)
            enableNotifications(gatt, characteristic)
            scheduleNextRead()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                emitPayload(value)
            }
        }

        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                emitPayload(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            emitPayload(value)
        }

        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            emitPayload(characteristic.value)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    private fun scheduleNextRead() {
        scanHandler.postDelayed({
            val activeGatt = gatt
            val activeCharacteristic = statusCharacteristic
            if (!shouldRun || activeGatt == null || activeCharacteristic == null) {
                return@postDelayed
            }

            activeGatt.readCharacteristic(activeCharacteristic)
            scheduleNextRead()
        }, READ_POLL_INTERVAL_MS)
    }

    private fun emitPayload(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return
        Log.d(TAG, "Received payload bytes=${bytes.size}")
        _payloads.tryEmit(String(bytes, StandardCharsets.UTF_8))
    }
}
