package org.gobinda.bletestapp

import android.app.Application
import timber.log.Timber

class BleTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}