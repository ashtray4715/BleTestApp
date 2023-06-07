package org.gobinda.reactiveble.discovery

import kotlinx.coroutines.flow.Flow

interface DiscoveryManager {
    fun startScan(): Flow<DiscoveredDevice>
}