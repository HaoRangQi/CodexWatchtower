package com.codextraffic.ble

import android.os.ParcelUuid
import java.util.UUID

object BleConstants {
    const val DEVICE_NAME = "Codex Traffic"

    val SERVICE_UUID: UUID = UUID.fromString("4F4C0001-6C6F-6164-696E-672D636F6465")
    val STATUS_CHARACTERISTIC_UUID: UUID = UUID.fromString("4F4C0002-6C6F-6164-696E-672D636F6465")
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val SERVICE_PARCEL_UUID = ParcelUuid(SERVICE_UUID)
}

