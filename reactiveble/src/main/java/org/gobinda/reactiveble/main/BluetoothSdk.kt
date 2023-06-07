package org.gobinda.reactiveble.main

import org.gobinda.reactiveble.discovery.DiscoveryManager

interface BluetoothSdk {
    val discoveryManager: DiscoveryManager
}