package org.gobinda.reactiveble.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.mockk.*
import kotlinx.coroutines.*
import org.gobinda.reactiveble.common.IntentFilterFactory
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

class DiscoveryManagerTest7 {

    private val bluetoothSdk: BluetoothSdk by inject(BluetoothSdk::class.java)

    private lateinit var mContext: Context
    private lateinit var mPermissionManager: PermissionManager
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothLeScanner: BluetoothLeScanner
    private lateinit var mScanCallback: CapturingSlot<ScanCallback>
    private lateinit var mIntentFilterFactory: IntentFilterFactory
    private lateinit var mIntentFilter: IntentFilter
    private lateinit var mBroadcastReceiver: CapturingSlot<BroadcastReceiver>

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
        mBroadcastReceiver = slot()

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
     * Bluetooth gets disabled after discovering 2 device,
     * Flow will be closed and DisabledBluetoothAdapterException will be thrown
     * - all the permissions are okay
     * - bluetooth adapter was enabled initially
     */
    @Test
    fun testNow(): Unit = runBlocking {

        val deviceNameList = listOf("ble device 1", "ble device 2")
        val deviceAddressList = listOf("11:22:33:44:55:66", "99:88:77:66:55:44")

        val scanResult1 = mockk<ScanResult> {
            every { device.name } returns deviceNameList[0]
            every { device.address } returns deviceAddressList[0]
        }
        val scanResult2 = mockk<ScanResult> {
            every { device.name } returns deviceNameList[1]
            every { device.address } returns deviceAddressList[1]
        }

        every { mPermissionManager.missingBluetoothStartScanPermission() } returns false
        every { mContext.getSystemService(Context.BLUETOOTH_SERVICE) } returns mBluetoothManager
        every { mBluetoothManager.adapter } returns mBluetoothAdapter
        every { mBluetoothAdapter.isEnabled } returns true
        every { mBluetoothAdapter.bluetoothLeScanner } returns mBluetoothLeScanner
        every { mBluetoothLeScanner.startScan(capture(mScanCallback)) } answers {
            launch {
                delay(100)
                mScanCallback.captured.onScanResult(1, scanResult1)
                delay(100)
                mScanCallback.captured.onScanResult(1, scanResult2)
            }
        }
        every { mBluetoothLeScanner.stopScan(capture(mScanCallback)) } returns Unit
        every { mIntentFilterFactory.getNewIntentFilter(any()) } returns mIntentFilter
        every { mIntentFilter.addAction(any()) } returns Unit
        every { mContext.registerReceiver(capture(mBroadcastReceiver), any()) } answers {
            val mCurrentIntent = mockk<Intent> {
                every { action } returns BluetoothAdapter.ACTION_STATE_CHANGED
                every { getIntExtra(any(), any()) } returns BluetoothAdapter.STATE_OFF
            }
            launch {
                delay(300)
                mBroadcastReceiver.captured.onReceive(mContext, mCurrentIntent)
            }
            mCurrentIntent
        }
        every { mContext.unregisterReceiver(any()) } returns Unit

        var globalExceptionFound = false
        var disabledBluetoothAdapterException = false
        val discoveredList = mutableListOf<DiscoveredDevice>()

        val mJob = launch {
            try {
                bluetoothSdk.discoveryManager.startScan().collect {
                    assert(deviceNameList.contains(it.name))
                    assert(deviceAddressList.contains(it.address))
                    discoveredList.add(it)
                }
            } catch (e: DisabledBluetoothAdapterException) {
                disabledBluetoothAdapterException = true
            } catch (e: Exception) {
                globalExceptionFound = true
            }
        }
        mJob.join()

        assert(disabledBluetoothAdapterException)
        assert(globalExceptionFound.not())
        assert(discoveredList.size == 2)
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