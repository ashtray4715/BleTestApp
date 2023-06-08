package org.gobinda.reactiveble.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

internal class DiscoveredDeviceImpl(
    private val bluetoothDevice: BluetoothDevice
) : DiscoveredDevice {
    override val name: String
        @SuppressLint("MissingPermission")
        get() = bluetoothDevice.name ?: ""

    override val address: String
        get() = bluetoothDevice.address
}