package org.gobinda.reactiveble.main

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gobinda.reactiveble.common.PermissionManager
import org.gobinda.reactiveble.discovery.DiscoveredDevice
import org.gobinda.reactiveble.discovery.DiscoveryManager
import org.gobinda.reactiveble.discovery.DiscoveryManagerImpl
import org.gobinda.reactiveble.errors.NullBluetoothAdapterException
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

class DiscoveryManagerTest2 {

    private val bluetoothSdk: BluetoothSdk by inject(BluetoothSdk::class.java)

    private lateinit var mContext: Context
    private lateinit var mPermissionManager: PermissionManager

    @Before
    fun setup() {
        Timber.plant(TimberTestTree())

        mContext = mockk()
        mPermissionManager = mockk()

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

    @Test
    fun testNullBluetoothAdapterException(): Unit = runBlocking {

        every { mPermissionManager.missingBluetoothStartScanPermission() } returns false
        every { mContext.getSystemService(Context.BLUETOOTH_SERVICE) } returns null

        var nullBluetoothExceptionFound = false
        var globalExceptionFound = false
        val discoveredList = mutableListOf<DiscoveredDevice>()

        val mJob = launch {
            try {
                bluetoothSdk.discoveryManager.startScan().collect {
                    discoveredList.add(it)
                }
            } catch (e: NullBluetoothAdapterException) {
                nullBluetoothExceptionFound = true
            } catch (e: Exception) {
                globalExceptionFound = true
            }
        }

        mJob.join()

        assert(nullBluetoothExceptionFound)
        assert(globalExceptionFound.not())
        assert(discoveredList.isEmpty())
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}