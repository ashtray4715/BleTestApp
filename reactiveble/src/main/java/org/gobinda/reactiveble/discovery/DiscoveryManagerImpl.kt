package org.gobinda.reactiveble.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.gobinda.reactiveble.common.IntentFilterFactory
import org.gobinda.reactiveble.common.PermissionManager
import org.gobinda.reactiveble.errors.*
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

        val scanCallBack = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Timber.i("$TAG onScanResult: address ? ${result.device.address}")
                try {
                    trySend(DiscoveredDeviceImpl(result.device))
                } catch (e: Exception) {
                    Timber.e("$TAG onScanResult: trySend failed with exception ${e.message}")
                    e.printStackTrace()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Timber.i("$TAG onScanFailed: errorCode ? $errorCode")
                close(ScanFailedException(errorCode = errorCode))
            }
        }

        Timber.i("$TAG startScan: invoked successfully")
        bleAdapter.bluetoothLeScanner.startScan(scanCallBack)

        awaitClose {
            Timber.i("$TAG stopScan: invoked successfully")
            bleAdapter.bluetoothLeScanner.stopScan(scanCallBack)
        }
    }

    companion object {
        private const val TAG = "DiscoveryManagerImpl"
    }
}