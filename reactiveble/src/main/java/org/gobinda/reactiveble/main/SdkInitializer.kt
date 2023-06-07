package org.gobinda.reactiveble.main

import android.content.Context
import org.gobinda.reactiveble.common.BuildVersionProvider
import org.gobinda.reactiveble.common.IntentFilterFactory
import org.gobinda.reactiveble.common.PermissionManager
import org.gobinda.reactiveble.discovery.DiscoveryManager
import org.gobinda.reactiveble.discovery.DiscoveryManagerImpl
import org.koin.android.ext.koin.androidContext

import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.get

object SdkInitializer {
    fun init(context: Context) {
        startKoin {
            androidContext(context)
            modules(myLibraryModule)
        }
    }

    fun deInit() {
        stopKoin()
    }

    fun getSdk(): BluetoothSdk = get(BluetoothSdk::class.java)
}

val myLibraryModule = module {
    single { BuildVersionProvider() }
    single { IntentFilterFactory() }
    single { PermissionManager() }
    single<DiscoveryManager> { DiscoveryManagerImpl(androidContext()) }
    single<BluetoothSdk> { BluetoothSdkImpl() }
}