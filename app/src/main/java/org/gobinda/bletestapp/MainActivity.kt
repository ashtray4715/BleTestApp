package org.gobinda.bletestapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.gobinda.reactiveble.main.SdkInitializer
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var mCoroutine: CoroutineScope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        Timber.i("$TAG onStart: invoked")
        SdkInitializer.init(applicationContext)

        mCoroutine?.cancel()
        mCoroutine = CoroutineScope(Dispatchers.IO)

        mCoroutine?.launch {
            try {
                SdkInitializer.getSdk().discoveryManager.startScan().collect {
                    Timber.i("$TAG onDiscovered: collected -> ${it.address}")
                }
            } catch (e: Exception) {
                Timber.e("$TAG onDiscovered: error occurs ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        mCoroutine?.cancel()
        SdkInitializer.deInit()
        super.onStop()
    }

    companion object {
        private const val TAG = "[MG]"
    }
}