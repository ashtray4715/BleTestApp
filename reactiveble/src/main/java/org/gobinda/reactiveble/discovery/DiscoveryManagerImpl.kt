package org.gobinda.reactiveble.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.gobinda.reactiveble.common.IntentFilterFactory
import org.gobinda.reactiveble.common.PermissionManager
import org.gobinda.reactiveble.errors.DisabledBluetoothAdapterException
import org.gobinda.reactiveble.errors.MissingPermissionsException
import org.gobinda.reactiveble.errors.ModuleNotInitializedException
import org.gobinda.reactiveble.errors.NullBluetoothAdapterException
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

internal class DiscoveryManagerImpl(private val context: Context) : DiscoveryManager {

    private val permissionManager: PermissionManager by inject(
        PermissionManager::class.java
    )

    private val intentFilterFactory: IntentFilterFactory by inject(
        IntentFilterFactory::class.java
    )

    @SuppressLint("MissingPermission")
    override fun startScan(): Flow<DiscoveredDevice> = callbackFlow {
        Timber.i("$TAG startScan: flow started")

        if (permissionManager.missingBluetoothStartScanPermission()) {
            close(MissingPermissionsException())
            return@callbackFlow
        }

        val bluetoothService = context.getSystemService(Context.BLUETOOTH_SERVICE)
        val bleAdapter = (bluetoothService as BluetoothManager?)?.adapter

        if (bleAdapter == null) {
            Timber.e("$TAG startScan: Bluetooth adapter is null")
            close(NullBluetoothAdapterException())
            return@callbackFlow
        }
        if (bleAdapter.isEnabled.not()) {
            Timber.e("$TAG startScan: Bluetooth adapter is not enabled")
            close(DisabledBluetoothAdapterException())
            return@callbackFlow
        }

        throw ModuleNotInitializedException()
    }

    companion object {
        private const val TAG = "DiscoveryManagerImpl"
    }
}