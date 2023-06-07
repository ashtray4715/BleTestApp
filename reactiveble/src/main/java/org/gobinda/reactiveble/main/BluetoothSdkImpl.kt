package org.gobinda.reactiveble.main

import org.gobinda.reactiveble.discovery.DiscoveryManager
import org.koin.java.KoinJavaComponent.get

internal class BluetoothSdkImpl : BluetoothSdk {
    override val discoveryManager: DiscoveryManager = get(DiscoveryManager::class.java)
}