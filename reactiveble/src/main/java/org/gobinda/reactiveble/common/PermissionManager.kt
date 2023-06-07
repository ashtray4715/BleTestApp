package org.gobinda.reactiveble.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.koin.java.KoinJavaComponent.inject

class PermissionManager {
    private val context: Context by inject(
        Context::class.java
    )

    private val buildVersionProvider: BuildVersionProvider by inject(
        BuildVersionProvider::class.java
    )

    fun missingBluetoothStartScanPermission(): Boolean {
        return mutableListOf<Boolean>().apply {
            when {
                buildVersionProvider.getBuildVersion() >= Build.VERSION_CODES.S -> {
                    add(isBluetoothScanPermissionAllowed())
                    add(isBluetoothConnectPermissionAllowed())
                }
                else -> {
                    // todo - add permissions for below Android-S
                }
            }
        }.contains(false)
    }

    @SuppressLint("InlinedApi")
    private fun isBluetoothScanPermissionAllowed(): Boolean {
        return if (buildVersionProvider.getBuildVersion() >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("InlinedApi")
    private fun isBluetoothConnectPermissionAllowed(): Boolean {
        return if (buildVersionProvider.getBuildVersion() >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}