package org.gobinda.reactiveble.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gobinda.reactiveble.common.PermissionManager
import org.gobinda.reactiveble.discovery.DiscoveredDevice
import org.gobinda.reactiveble.discovery.DiscoveryManager
import org.gobinda.reactiveble.discovery.DiscoveryManagerImpl
import org.gobinda.reactiveble.errors.DisabledBluetoothAdapterException
import org.gobinda.reactiveble.log.TimberTestTree
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class DiscoveryManagerTest3 {

    private val bluetoothSdk: BluetoothSdk by inject(BluetoothSdk::class.java)

    private lateinit var mContext: Context
    private lateinit var mPermissionManager: PermissionManager
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    @Before
    fun setup() {
        Timber.plant(TimberTestTree())

        mContext = mockk()
        mPermissionManager = mockk()
        mBluetoothManager = mockk()
        mBluetoothAdapter = mockk()

        startKoin {
            modules(
                module {
                    single { mContext }
                    single { mPermissionManager }
                    single<DiscoveryManager> { DiscoveryManagerImpl(androidContext()) }
                    single<BluetoothSdk> { BluetoothSdkImpl() }
                }
            )
        }
    }

    /**
     * All the permissions are okay, device also supports bluetooth, but
     * bluetooth adapter is not enabled (that's the exception here)
     */
    @Test
    fun testNow(): Unit = runBlocking {

        every { mPermissionManager.missingBluetoothStartScanPermission() } returns false
        every { mContext.getSystemService(Context.BLUETOOTH_SERVICE) } returns mBluetoothManager
        every { mBluetoothManager.adapter } returns mBluetoothAdapter
        every { mBluetoothAdapter.isEnabled } returns false

        var bluetoothDisableExceptionFound = false
        var globalExceptionFound = false
        val discoveredList = mutableListOf<DiscoveredDevice>()

        val mJob = launch {
            try {
                bluetoothSdk.discoveryManager.startScan().collect {
                    discoveredList.add(it)
                }
            } catch (e: DisabledBluetoothAdapterException) {
                bluetoothDisableExceptionFound = true
            } catch (e: Exception) {
                globalExceptionFound = true
            }
        }

        mJob.join()

        assert(bluetoothDisableExceptionFound)
        assert(globalExceptionFound.not())
        assert(discoveredList.isEmpty())
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}