package org.gobinda.reactiveble.common

import android.content.IntentFilter

class IntentFilterFactory {
    fun getNewIntentFilter(action: String): IntentFilter {
        return IntentFilter().apply { addAction(action) }
    }
}