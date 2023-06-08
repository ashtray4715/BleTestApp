package org.gobinda.reactiveble.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.IntentFilter
import io.mockk.*
import kotlinx.coroutines.*
import org.gobinda.reactiveble.common.IntentFilterFactory
import org.gobinda.reactiveble.common.PermissionManager
import org.gobinda.reactiveble.discovery.DiscoveredDevice
import org.gobinda.reactiveble.discovery.DiscoveryManager
import org.gobinda.reactiveble.discovery.DiscoveryManagerImpl
import org.gobinda.reactiveble.errors.ScanFailedException
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

class DiscoveryManagerTest5 {

    private val bluetoothSdk: BluetoothSdk by inject(BluetoothSdk::class.java)

    private lateinit var mContext: Context
    private lateinit var mPermissionManager: PermissionManager
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothLeScanner: BluetoothLeScanner
    private lateinit var mScanCallback: CapturingSlot<ScanCallback>
    private lateinit var mIntentFilterFactory: IntentFilterFactory
    private lateinit var mIntentFilter: IntentFilter

    @Before
    fun setup() {
        Timber.plant(TimberTestTree())

        mContext = mockk()
        mPermissionManager = mockk()
        mBluetoothManager = mockk()
        mBluetoothAdapter = mockk()
        mBluetoothLeScanner = mockk()
        mScanCallback = slot()
        mIntentFilterFactory = mockk()
        mIntentFilter = mockk()

        startKoin {
            modules(
                module {
                    single { mContext }
                    single { mPermissionManager }
                    single { mIntentFilterFactory }
                    single<DiscoveryManager> { DiscoveryManagerImpl(androidContext()) }
                    single<BluetoothSdk> { BluetoothSdkImpl() }
                }
            )
        }
    }

    /**
     * scan gets failed before discovering any device,
     * - all the permissions are okay,
     * - bluetooth adapter is enabled
     */
    @Test
    fun testNow(): Unit = runBlocking {

        every { mPermissionManager.missingBluetoothStartScanPermission() } returns false
        every { mContext.getSystemService(Context.BLUETOOTH_SERVICE) } returns mBluetoothManager
        every { mBluetoothManager.adapter } returns mBluetoothAdapter
        every { mBluetoothAdapter.isEnabled } returns true
        every { mBluetoothAdapter.bluetoothLeScanner } returns mBluetoothLeScanner
        every { mBluetoothLeScanner.startScan(capture(mScanCallback)) } answers {
            launch {
                delay(10)
                mScanCallback.captured.onScanFailed(10)
            }
        }
        every { mBluetoothLeScanner.stopScan(capture(mScanCallback)) } returns Unit
        every { mIntentFilterFactory.getNewIntentFilter(any()) } returns mIntentFilter
        every { mIntentFilter.addAction(any()) } returns Unit
        every { mContext.registerReceiver(any(), any()) } returns mockk()
        every { mContext.unregisterReceiver(any()) } returns Unit

        var globalExceptionFound = false
        var scanFailedExceptionFound = false
        val discoveredList = mutableListOf<DiscoveredDevice>()

        val mJob = launch {
            try {
                bluetoothSdk.discoveryManager.startScan().collect {
                    discoveredList.add(it)
                }
            } catch (e: ScanFailedException) {
                assert(e.errorCode == 10)
                scanFailedExceptionFound = true
            } catch (e: Exception) {
                globalExceptionFound = true
            }
        }
        mJob.join()

        assert(scanFailedExceptionFound)
        assert(globalExceptionFound.not())
        assert(discoveredList.isEmpty())
        verify { mBluetoothLeScanner.startScan(capture(mScanCallback)) }
        verify { mBluetoothLeScanner.stopScan(capture(mScanCallback)) }
        verify { mContext.registerReceiver(any(), any()) }
        verify { mContext.unregisterReceiver(any()) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}