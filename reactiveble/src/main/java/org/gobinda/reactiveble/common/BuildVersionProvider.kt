package org.gobinda.reactiveble.common

import android.os.Build

class BuildVersionProvider {
    fun getBuildVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}