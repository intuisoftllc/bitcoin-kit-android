package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoinutils.BitcoinScope
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TransactionSendTimer(private val period: Long) {

    interface Listener {
        fun onTimePassed()
    }

    var listener: Listener? = null
    private var task: Job? = null

    @Synchronized
    fun startIfNotRunning() {
        if (task == null) {
            task = BitcoinScope.applicationScope.launch(Dispatchers.IO) {
                while(true) {
                    this.ensureActive()
                    listener?.onTimePassed()
                    delay(period * 1000)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        task?.let {
            it.cancel()
            task = null
        }
    }

}
